//! Pure wire-format helpers used by the VPN engine.

use std::{
    cmp::Ordering as SequenceOrdering,
    io::{self, ErrorKind},
    os::fd::RawFd,
    time::Instant,
};

use crate::{
    packet::{self, Flow, TcpFlags},
    platform,
    session::{TcpSession, TcpState},
};

use super::{Callbacks, Engine};

pub(super) fn is_echo_request(version: packet::Version, kind: u8) -> bool {
    matches!(
        (version, kind),
        (packet::Version::V4, 8) | (packet::Version::V6, 128)
    )
}

pub(super) fn rewrite_dns_tcp_frames(
    buffer: &mut Vec<u8>,
    rcode: u8,
    flow: Flow,
    callbacks: &mut impl Callbacks,
) -> Vec<u8> {
    let mut output = Vec::new();
    let mut consumed = 0;
    while buffer.len().saturating_sub(consumed) >= 2 {
        let length = usize::from(u16::from_be_bytes([buffer[consumed], buffer[consumed + 1]]));
        let Some(frame_end) = consumed.checked_add(2 + length) else {
            break;
        };
        let Some(message) = buffer.get(consumed + 2..frame_end) else {
            break;
        };
        let rewritten = Engine::rewrite_dns_response(rcode, flow, message, callbacks)
            .unwrap_or_else(|| message.to_vec());
        let Ok(length) = u16::try_from(rewritten.len()) else {
            break;
        };
        output.extend_from_slice(&length.to_be_bytes());
        output.extend_from_slice(&rewritten);
        consumed = frame_end;
    }
    if consumed != 0 {
        buffer.drain(..consumed);
    }
    output
}

pub(super) fn write_packet(fd: RawFd, mut bytes: &[u8]) -> io::Result<()> {
    while !bytes.is_empty() {
        match platform::write(fd, bytes) {
            Ok(0) => {
                return Err(io::Error::new(
                    ErrorKind::WriteZero,
                    "VPN tunnel short write",
                ));
            }
            Ok(written) => bytes = &bytes[written..],
            Err(error) if error.kind() == ErrorKind::Interrupted => {}
            Err(error) => return Err(error),
        }
    }
    Ok(())
}

pub(super) fn sequence_order(first: u32, second: u32) -> SequenceOrdering {
    if first == second {
        SequenceOrdering::Equal
    } else if (first < second && second.wrapping_sub(first) < 0x7fff_ffff)
        || (first > second && first.wrapping_sub(second) > 0x7fff_ffff)
    {
        SequenceOrdering::Less
    } else {
        SequenceOrdering::Greater
    }
}

pub(super) fn syn_ack(flow: Flow, session: &mut TcpSession) -> Vec<u8> {
    session.refresh_receive_window();
    let packet = packet::tcp_reply_with_window_scale(
        flow,
        session.local_sequence,
        session.remote_sequence,
        session.advertised_receive_window(),
        TcpFlags {
            syn: true,
            ack: true,
            ..TcpFlags::default()
        },
        &[],
        session.receive_scale,
    );
    session.local_sequence = session.local_sequence.wrapping_add(1);
    session.state = TcpState::SynReceived;
    session.last_activity = Instant::now();
    packet
}
