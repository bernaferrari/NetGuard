//! Incremental SOCKS5 negotiation for TCP sessions.

use std::{
    io::{self, ErrorKind, Read},
    net::IpAddr,
    os::fd::AsRawFd,
};

use crate::{
    packet::Flow,
    platform,
    session::{TcpSession, TcpState},
};

pub(super) fn write_socks_hello(session: &mut TcpSession) {
    queue_socks_write(session, vec![5, 2, 0, 2]);
}

fn queue_socks_write(session: &mut TcpSession, request: Vec<u8>) {
    session.socks_pending = request;
    session.socks_pending_offset = 0;
}

pub(super) fn flush_socks_write(session: &mut TcpSession) -> io::Result<bool> {
    while session.socks_pending_offset < session.socks_pending.len() {
        let sent = match platform::send_tcp(
            session.socket.as_raw_fd(),
            &session.socks_pending[session.socks_pending_offset..],
            false,
        ) {
            Ok(0) => return Err(io::Error::new(ErrorKind::WriteZero, "SOCKS5 short write")),
            Ok(sent) => sent,
            Err(error)
                if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) =>
            {
                return Ok(false);
            }
            Err(error) => return Err(error),
        };
        session.socks_pending_offset += sent;
    }
    session.socks_pending.clear();
    session.socks_pending_offset = 0;
    Ok(true)
}

pub(super) fn progress_socks5(session: &mut TcpSession, flow: Flow) -> io::Result<bool> {
    if let Some(complete) = consume_socks_response(session, flow)? {
        return Ok(complete);
    }
    let mut response = [0_u8; 512];
    let length = match session.socket.read(&mut response) {
        Ok(length) => length,
        Err(error) if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) => {
            return Ok(false);
        }
        Err(error) => return Err(error),
    };
    if length == 0 {
        return Err(io::Error::new(
            ErrorKind::UnexpectedEof,
            "SOCKS5 proxy closed",
        ));
    }
    session
        .socks_response
        .extend_from_slice(&response[..length]);
    Ok(consume_socks_response(session, flow)?.unwrap_or(false))
}

fn consume_socks_response(session: &mut TcpSession, flow: Flow) -> io::Result<Option<bool>> {
    match session.state {
        TcpState::SocksHello => {
            if session.socks_response.len() < 2 {
                return Ok(None);
            }
            let response: [u8; 2] = session.socks_response[..2]
                .try_into()
                .map_err(|_| io::Error::other("invalid SOCKS5 greeting"))?;
            session.socks_response.drain(..2);
            if response[0] != 5 {
                return Err(io::Error::new(
                    ErrorKind::InvalidData,
                    "invalid SOCKS5 version",
                ));
            }
            match response[1] {
                0 => Ok(Some(write_socks_connect(session, flow))),
                2 => write_socks_authentication(session).map(Some),
                method => Err(io::Error::new(
                    ErrorKind::PermissionDenied,
                    format!("SOCKS5 authentication method {method} is unavailable"),
                )),
            }
        }
        TcpState::SocksAuthenticate => {
            if session.socks_response.len() < 2 {
                return Ok(None);
            }
            let response: [u8; 2] = session.socks_response[..2]
                .try_into()
                .map_err(|_| io::Error::other("invalid SOCKS5 authentication response"))?;
            session.socks_response.drain(..2);
            if response == [1, 0] {
                Ok(Some(write_socks_connect(session, flow)))
            } else {
                Err(io::Error::new(
                    ErrorKind::PermissionDenied,
                    "SOCKS5 authentication was rejected",
                ))
            }
        }
        TcpState::SocksConnect => {
            let Some(length) = socks_connect_reply_length(&session.socks_response)? else {
                return Ok(None);
            };
            let response: Vec<_> = session.socks_response.drain(..length).collect();
            socks_connect_reply(&response)
                .then_some(true)
                .ok_or_else(|| {
                    io::Error::new(ErrorKind::InvalidData, "invalid SOCKS5 CONNECT response")
                })
                .map(Some)
        }
        _ => Ok(None),
    }
}

fn write_socks_authentication(session: &mut TcpSession) -> io::Result<bool> {
    let Some(credentials) = session.socks5.as_ref() else {
        return Err(io::Error::other("missing SOCKS5 credentials"));
    };
    let username = credentials.username.as_bytes();
    let password = credentials.password.as_bytes();
    let username_len = u8::try_from(username.len()).map_err(|_| {
        io::Error::new(ErrorKind::InvalidInput, "SOCKS5 username exceeds 255 bytes")
    })?;
    let password_len = u8::try_from(password.len()).map_err(|_| {
        io::Error::new(ErrorKind::InvalidInput, "SOCKS5 password exceeds 255 bytes")
    })?;
    let mut request = Vec::with_capacity(3 + username.len() + password.len());
    request.extend_from_slice(&[1, username_len]);
    request.extend_from_slice(username);
    request.push(password_len);
    request.extend_from_slice(password);
    queue_socks_write(session, request);
    session.state = TcpState::SocksAuthenticate;
    Ok(false)
}

fn write_socks_connect(session: &mut TcpSession, flow: Flow) -> bool {
    let mut request = vec![5, 1, 0];
    match flow.destination {
        IpAddr::V4(address) => {
            request.push(1);
            request.extend_from_slice(&address.octets());
        }
        IpAddr::V6(address) => {
            request.push(4);
            request.extend_from_slice(&address.octets());
        }
    }
    request.extend_from_slice(&flow.destination_port.to_be_bytes());
    queue_socks_write(session, request);
    session.state = TcpState::SocksConnect;
    false
}

pub(super) fn socks_connect_reply_length(response: &[u8]) -> io::Result<Option<usize>> {
    if response.len() < 4 {
        return Ok(None);
    }
    if response[0] != 5 || response[2] != 0 {
        return Err(io::Error::new(
            ErrorKind::InvalidData,
            "invalid SOCKS5 CONNECT header",
        ));
    }
    let length = match response[3] {
        1 => 10,
        4 => 22,
        3 => {
            let Some(length) = response.get(4) else {
                return Ok(None);
            };
            7 + usize::from(*length)
        }
        _ => {
            return Err(io::Error::new(
                ErrorKind::InvalidData,
                "invalid SOCKS5 address type",
            ));
        }
    };
    Ok((response.len() >= length).then_some(length))
}

pub(super) fn socks_connect_reply(response: &[u8]) -> bool {
    socks_connect_reply_length(response)
        .ok()
        .flatten()
        .is_some_and(|length| response.len() == length && response[1] == 0)
}
