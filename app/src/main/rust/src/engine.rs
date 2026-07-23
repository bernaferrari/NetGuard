//! VPN event loop. It owns all packet buffers and sessions, while policy and
//! JNI remain injected interfaces rather than global mutable state.

#[path = "engine_socks5.rs"]
mod socks5;
#[path = "engine_wire.rs"]
mod wire;

use std::{
    cmp::Ordering as SequenceOrdering,
    io::{self, ErrorKind, Read},
    net::{IpAddr, Shutdown, SocketAddr},
    os::fd::{AsRawFd, RawFd},
    sync::atomic::{AtomicBool, AtomicI32, Ordering},
    time::Instant,
};

use crate::{
    MTU, dhcp, dns,
    fragment::Reassembler,
    packet::{self, Flow, Packet, TcpFlags, Transport},
    pcap::PcapWriter,
    platform,
    session::{
        IcmpSession, Session, SessionTable, Statistics, TcpSession, TcpState, UdpSession, Usage,
    },
    tls,
};
use socks5::{flush_socks_write, progress_socks5, write_socks_hello};
#[cfg(test)]
use socks5::{socks_connect_reply, socks_connect_reply_length};
use wire::{is_echo_request, rewrite_dns_tcp_frames, sequence_order, syn_ack, write_packet};

const TUN_YIELD: usize = 10;
const IPV4_SOCKET_DATAGRAM_MAX: usize = 65_535 - 20 - 8;
const IPV6_SOCKET_DATAGRAM_MAX: usize = 65_535 - 40 - 8;

fn socket_datagram_limit(version: packet::Version) -> usize {
    match version {
        packet::Version::V4 => IPV4_SOCKET_DATAGRAM_MAX,
        packet::Version::V6 => IPV6_SOCKET_DATAGRAM_MAX,
    }
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct Redirect {
    pub(crate) address: IpAddr,
    pub(crate) port: u16,
    pub(crate) redirected: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Socks5Config {
    pub(crate) endpoint: SocketAddr,
    pub(crate) username: String,
    pub(crate) password: String,
}

#[derive(Debug, Clone)]
pub(crate) struct PacketInfo {
    pub(crate) flow: Flow,
    pub(crate) flags: String,
    pub(crate) data: String,
    pub(crate) uid: i32,
}

pub(crate) trait Callbacks {
    fn uid_for(&mut self, flow: Flow) -> i32;
    fn allow(&mut self, packet: &PacketInfo) -> Option<Redirect>;
    fn domain_blocked(&mut self, name: &str) -> bool;
    fn log_dns_blocked(&mut self, packet: &PacketInfo);
    fn dns_resolved(&mut self, record: &dns::ResolvedRecord, uid: i32);
    fn protect_socket(&mut self, fd: RawFd) -> io::Result<()>;
    fn usage(&mut self, flow: Flow, usage: Usage);
    fn report_exit(&mut self, message: &str);
}

#[derive(Debug)]
pub(crate) struct Engine {
    sessions: SessionTable,
    fragments: Reassembler,
    pcap: PcapWriter,
    max_sessions: usize,
    dns_rcode: u8,
    forward_dns: bool,
    socks5: Option<Socks5Config>,
}

impl Engine {
    pub(crate) fn new() -> Self {
        Self {
            sessions: SessionTable::new(),
            fragments: Reassembler::default(),
            pcap: PcapWriter::default(),
            max_sessions: usize::try_from(platform::fd_limit())
                .ok()
                .map(|limit| limit.saturating_mul(40) / 100)
                .filter(|limit| *limit > 0)
                .unwrap_or(409)
                .min(409),
            dns_rcode: 3,
            forward_dns: false,
            socks5: None,
        }
    }
    pub(crate) fn statistics(&self) -> Statistics {
        self.sessions.statistics()
    }
    pub(crate) fn clear(&mut self) {
        self.sessions.clear();
    }
    pub(crate) fn configure_pcap(
        &mut self,
        path: Option<&std::path::Path>,
        record_size: usize,
        maximum_size: u64,
    ) -> io::Result<()> {
        self.pcap.configure(path, record_size, maximum_size)
    }
    pub(crate) fn configure_dns(&mut self, forward_dns: bool, rcode: i32) {
        self.dns_rcode = u8::try_from(rcode.clamp(0, 15)).unwrap_or_default();
        self.forward_dns = forward_dns;
    }
    pub(crate) fn configure_socks5(&mut self, socks5: Option<Socks5Config>) {
        self.socks5 = socks5;
    }

    // The event loop needs each independent lifecycle primitive in order to
    // make cancellation and the statistics hand-off explicit at this boundary.
    #[allow(clippy::too_many_arguments)]
    pub(crate) fn run(
        &mut self,
        tun: RawFd,
        wake: RawFd,
        stopping: &AtomicBool,
        clear_requested: &AtomicBool,
        stats: &[AtomicI32; 3],
        callbacks: &mut impl Callbacks,
        sync_configuration: &mut impl FnMut(&mut Self),
    ) {
        let mut buffer = vec![0_u8; MTU];
        // C invokes check_allowed before creating its event poller, so a
        // retained context is re-evaluated each time JNI starts a run.
        self.recheck_allowed(tun, callbacks);
        'events: while !stopping.load(Ordering::Acquire) {
            // JNI configuration is process-global in the C implementation.
            // Apply changes before accepting another tunnel packet so PCAP and
            // SOCKS5 updates affect a running service as well as a new one.
            sync_configuration(self);
            if clear_requested.swap(false, Ordering::AcqRel) {
                self.clear();
            }
            match platform::readable_or_woken(tun, wake, 50) {
                Ok(true) => {
                    // Match C's bounded drain: sustained tunnel traffic must
                    // not starve sockets and timeout handling.
                    for index in 0..TUN_YIELD {
                        match platform::read(tun, &mut buffer) {
                            Ok(0) => {
                                callbacks.report_exit("VPN tunnel reached EOF");
                                break 'events;
                            }
                            Ok(length) => self.handle_tun_packet(tun, &buffer[..length], callbacks),
                            Err(error)
                                if matches!(
                                    error.kind(),
                                    ErrorKind::Interrupted | ErrorKind::WouldBlock
                                ) =>
                            {
                                break;
                            }
                            Err(error) => {
                                callbacks.report_exit(&format!("VPN tunnel read failed: {error}"));
                                break 'events;
                            }
                        }
                        if index + 1 == TUN_YIELD || stopping.load(Ordering::Acquire) {
                            break;
                        }
                        match platform::readable(tun) {
                            Ok(true) => {}
                            Ok(false) | Err(_) => break,
                        }
                    }
                }
                Ok(false) => platform::drain_fd(wake),
                Err(error) if error.kind() == ErrorKind::Interrupted => {}
                Err(error) => {
                    callbacks.report_exit(&format!("VPN tunnel poll failed: {error}"));
                    break;
                }
            }
            self.drain_sockets(tun, callbacks);
            let timeouts = self.sessions.advance_timeouts(self.max_sessions);
            for (flow, sequence, acknowledgement) in timeouts.tcp_resets {
                self.write_tcp(
                    tun,
                    flow,
                    sequence,
                    acknowledgement,
                    0,
                    TcpFlags {
                        rst: true,
                        ..TcpFlags::default()
                    },
                    &[],
                );
            }
            for (flow, usage) in timeouts.usages {
                callbacks.usage(flow, usage);
            }
            for (flow, usage) in self.sessions.finish_closing() {
                callbacks.usage(flow, usage);
            }
            let current = self.statistics();
            stats[0].store(current.icmp, Ordering::Release);
            stats[1].store(current.udp, Ordering::Release);
            stats[2].store(current.tcp, Ordering::Release);
        }
        // A dead tunnel cannot safely resume an old stream. Drop every
        // protected socket deterministically before returning to JNI.
        self.clear();
        let current = self.statistics();
        stats[0].store(current.icmp, Ordering::Release);
        stats[1].store(current.udp, Ordering::Release);
        stats[2].store(current.tcp, Ordering::Release);
    }

    fn recheck_allowed(&mut self, tun: RawFd, callbacks: &mut impl Callbacks) {
        let mut remove = Vec::new();
        let mut finish_udp = Vec::new();
        let mut resets = Vec::new();
        for flow in self.sessions.flows() {
            let Some(session) = self.sessions.get_mut(flow) else {
                continue;
            };
            let (uid, active, protocol) = match session {
                Session::Icmp(icmp) => (icmp.uid, !icmp.stop, packet::ICMP),
                Session::Udp(udp) => (udp.uid, true, packet::UDP),
                Session::BlockedUdp { .. } => {
                    // check_allowed removes blocked entries immediately.
                    remove.push(flow);
                    continue;
                }
                Session::ClosedUdp { .. } => continue,
                Session::Tcp(tcp) => (
                    tcp.uid,
                    !matches!(tcp.state, TcpState::Closing | TcpState::Closed),
                    packet::TCP,
                ),
            };
            if !active {
                continue;
            }
            let allowed = callbacks.allow(&PacketInfo {
                flow,
                flags: String::new(),
                data: String::new(),
                uid,
            });
            if allowed.is_some() {
                continue;
            }
            match session {
                Session::Icmp(icmp) => icmp.stop = true,
                Session::Udp(_) => finish_udp.push(flow),
                Session::Tcp(tcp) => {
                    let acknowledge = matches!(
                        tcp.state,
                        TcpState::Connecting
                            | TcpState::SocksHello
                            | TcpState::SocksAuthenticate
                            | TcpState::SocksConnect
                    );
                    resets.push((flow, tcp.local_sequence, tcp.remote_sequence, acknowledge));
                    tcp.state = TcpState::Closing;
                }
                Session::BlockedUdp { .. } | Session::ClosedUdp { .. } => {}
            }
            debug_assert_eq!(flow.protocol, protocol);
        }
        for flow in remove {
            self.sessions.remove(flow);
        }
        for flow in finish_udp {
            if let Some(usage) = self.sessions.finish_udp(flow)
                && (usage.sent != 0 || usage.received != 0)
            {
                callbacks.usage(flow, usage);
            }
        }
        for (flow, sequence, acknowledgement, acknowledge_flag) in resets {
            self.write_tcp(
                tun,
                flow,
                sequence,
                acknowledgement,
                0,
                TcpFlags {
                    rst: true,
                    ack: acknowledge_flag,
                    ..TcpFlags::default()
                },
                &[],
            );
        }
    }

    fn handle_tun_packet(&mut self, tun: RawFd, bytes: &[u8], callbacks: &mut impl Callbacks) {
        self.handle_tun_packet_inner(tun, bytes, callbacks, true);
    }

    fn handle_tun_packet_inner(
        &mut self,
        tun: RawFd,
        bytes: &[u8],
        callbacks: &mut impl Callbacks,
        capture: bool,
    ) {
        if capture && let Err(error) = self.pcap.write(bytes) {
            platform::log(&format!("pcap write failed: {error}"));
        }
        if bytes.first().is_some_and(|first| first >> 4 == 4)
            && !packet::ipv4_fixed_header_checksum_valid(bytes)
        {
            platform::log("discarding IPv4 packet with invalid fixed-header checksum");
            return;
        }
        let packet = match packet::parse(bytes) {
            Ok(packet) => packet,
            Err(packet::ParseError::Fragmented) => {
                if let Some(reassembled) = self.fragments.push(bytes) {
                    self.handle_tun_packet_inner(tun, &reassembled, callbacks, false);
                }
                return;
            }
            Err(error) => {
                platform::log(&format!("discarding invalid VPN packet: {error:?}"));
                return;
            }
        };
        if !packet::transport_checksum_valid(bytes, &packet) {
            platform::log("discarding VPN packet with invalid transport checksum");
            return;
        }
        self.handle_packet(tun, packet, callbacks);
    }

    // Policy is intentionally evaluated before dispatch so every transport
    // follows the same admission, DNS, and capacity rules.
    #[allow(clippy::too_many_lines)]
    fn handle_packet(&mut self, tun: RawFd, packet: Packet<'_>, callbacks: &mut impl Callbacks) {
        let flow = packet.flow;
        // Capacity limits apply only to flows that would allocate a new
        // session. Existing traffic must continue to make progress.
        if self.sessions.active_len() >= self.max_sessions {
            let drop = match packet.transport {
                Transport::Icmp { .. } => self.sessions.active_icmp_flow(flow).is_none(),
                Transport::Udp { .. } => {
                    self.sessions.get_mut(flow).is_none()
                        && (self.forward_dns || flow.destination_port != 53)
                }
                Transport::Tcp(header) => header.flags.syn && self.sessions.get_mut(flow).is_none(),
                Transport::Other => false,
            };
            if drop {
                return;
            }
        }
        let (flags, sni, new_session) = match packet.transport {
            Transport::Udp { .. } => (
                String::new(),
                String::new(),
                self.sessions.get_mut(flow).is_none(),
            ),
            Transport::Tcp(header) => (
                header.flags.display(),
                tls::server_name(header.payload).unwrap_or_default(),
                header.flags.syn,
            ),
            Transport::Icmp {
                kind,
                code,
                message: _,
            } => (
                String::new(),
                format!("type {kind}/{code}"),
                self.sessions.get_mut(flow).is_none(),
            ),
            Transport::Other => (String::new(), String::new(), false),
        };
        let uid =
            if new_session || !sni.is_empty() || matches!(packet.transport, Transport::Icmp { .. })
            {
                callbacks.uid_for(flow)
            } else {
                -1
            };
        if !sni.is_empty() {
            callbacks.dns_resolved(
                &dns::ResolvedRecord {
                    question: sni.clone(),
                    answer_name: sni.clone(),
                    resource: flow.destination,
                    ttl: -1,
                },
                uid,
            );
        }
        let data = if sni.is_empty() {
            String::new()
        } else {
            "sni".to_owned()
        };
        let info = PacketInfo {
            flow,
            flags,
            data: data.clone(),
            uid,
        };
        let policy_required = match packet.transport {
            Transport::Udp { .. } => {
                new_session && (self.forward_dns || flow.destination_port != 53)
            }
            Transport::Tcp(header) => {
                !sni.is_empty() || (header.flags.syn && !(uid == 0 && flow.destination_port == 53))
            }
            Transport::Icmp { .. } | Transport::Other => true,
        };
        let redirect =
            if flow.protocol == packet::UDP && flow.destination_port == 53 && !self.forward_dns {
                Some(Redirect {
                    address: flow.destination,
                    port: flow.destination_port,
                    redirected: false,
                })
            } else if policy_required {
                callbacks.allow(&info)
            } else {
                Some(Redirect {
                    address: flow.destination,
                    port: flow.destination_port,
                    redirected: false,
                })
            };
        let allowed = redirect.is_some() && (sni.is_empty() || !callbacks.domain_blocked(&sni));
        if !allowed {
            match packet.transport {
                Transport::Udp { .. } => {
                    if self.sessions.get_mut(flow).is_none() {
                        self.sessions.insert(
                            flow,
                            Session::BlockedUdp {
                                last_activity: Instant::now(),
                            },
                        );
                    }
                }
                Transport::Tcp(header) => {
                    if sni.is_empty() {
                        return;
                    }
                    let acknowledgement = header
                        .sequence
                        .wrapping_add(u32::try_from(header.payload.len()).unwrap_or(u32::MAX))
                        .wrapping_add(u32::from(header.flags.syn))
                        .wrapping_add(u32::from(header.flags.fin));
                    self.write_tcp(
                        tun,
                        flow,
                        header.acknowledgement,
                        acknowledgement,
                        0,
                        TcpFlags {
                            rst: true,
                            ack: true,
                            ..TcpFlags::default()
                        },
                        &[],
                    );
                }
                _ => {}
            }
            return;
        }
        let target = redirect.unwrap_or(Redirect {
            address: flow.destination,
            port: flow.destination_port,
            redirected: false,
        });
        match packet.transport {
            Transport::Udp { payload } => {
                self.handle_udp(tun, flow, target, uid, payload, callbacks);
            }
            Transport::Tcp(header) => {
                self.handle_tcp(tun, flow, target, uid, header, callbacks, true);
            }
            Transport::Icmp { kind, message, .. } => {
                self.handle_icmp(flow, kind, message, uid, callbacks);
            }
            Transport::Other => {}
        }
    }

    fn handle_icmp(
        &mut self,
        flow: Flow,
        kind: u8,
        message: &[u8],
        uid: i32,
        callbacks: &mut impl Callbacks,
    ) {
        let echo_request = is_echo_request(flow.version, kind);
        let Some(identifier) = message
            .get(4..6)
            .and_then(|id| id.try_into().ok())
            .map(u16::from_be_bytes)
        else {
            return;
        };
        if !echo_request {
            return;
        }
        // C's ICMP session lookup ignores icmp_id, retaining the identifier
        // from the first echo for this address pair.
        let session_flow = self.sessions.active_icmp_flow(flow).unwrap_or(flow);
        if matches!(self.sessions.get_mut(session_flow), Some(Session::Icmp(session)) if session.stop)
        {
            // The linked-list C implementation briefly retains this stopped
            // entry while adding a replacement. HashMap ownership makes the
            // replacement itself the observable equivalent.
            self.sessions.remove(session_flow);
        }
        if self.sessions.get_mut(session_flow).is_none() {
            if self.sessions.active_len() >= self.max_sessions {
                return;
            }
            let session = match IcmpSession::open(flow.destination, identifier, uid, |fd| {
                callbacks.protect_socket(fd)
            }) {
                Ok(session) => session,
                Err(error) => {
                    platform::log(&format!("ICMP socket setup failed: {error}"));
                    return;
                }
            };
            self.sessions.insert(session_flow, Session::Icmp(session));
        }
        let Some(Session::Icmp(session)) = self.sessions.get_mut(session_flow) else {
            return;
        };
        let mut forwarded = message.to_vec();
        forwarded[4..6].copy_from_slice(&(!identifier).to_be_bytes());
        forwarded[2..4].fill(0);
        if let Some(checksum) = packet::icmp_message_checksum(flow, &forwarded) {
            forwarded[2..4].copy_from_slice(&checksum.to_be_bytes());
        }
        let target = socket2::SockAddr::from(SocketAddr::new(flow.destination, 0));
        // C refreshes this timestamp immediately before `sendto`, including
        // when a nonblocking socket asks it to retry later.
        session.last_activity = Instant::now();
        match session.socket.send_to(&forwarded, &target) {
            Ok(_) => {}
            Err(error)
                if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) => {}
            Err(error) => {
                platform::log(&format!("ICMP send failed: {error}"));
                session.stop = true;
            }
        }
    }

    fn handle_udp(
        &mut self,
        tun: RawFd,
        flow: Flow,
        target: Redirect,
        uid: i32,
        payload: &[u8],
        callbacks: &mut impl Callbacks,
    ) {
        if self.sessions.get_mut(flow).is_none() {
            if self.sessions.active_len() >= self.max_sessions {
                return;
            }
            let address = SocketAddr::new(target.address, target.port);
            let session = match UdpSession::open(
                address,
                flow.destination_port,
                |fd| callbacks.protect_socket(fd),
                uid,
            ) {
                Ok(session) => session,
                Err(error) => {
                    platform::log(&format!("UDP socket setup failed: {error}"));
                    return;
                }
            };
            self.sessions.insert(flow, Session::Udp(session));
        }
        // This follows C's ordering: opening/registering the UDP session
        // happens first, then DHCP is consumed locally without send()ing its
        // request through that socket.
        if (flow.source_port == 68 || flow.destination_port == 67) && dhcp::is_recognized(payload) {
            if let Some(reply) = dhcp::response(payload) {
                // check_dhcp writes 10.1.10.1 into udp_session.saddr before
                // write_udp reverses the session endpoints. Preserve the
                // resulting wire packet and changed lookup key safely.
                let mutated_flow = Flow {
                    source: IpAddr::V4(std::net::Ipv4Addr::new(10, 1, 10, 1)),
                    ..flow
                };
                self.sessions.rekey(flow, mutated_flow);
                let c_reply_flow = Flow {
                    source: flow.destination,
                    destination: mutated_flow.source,
                    source_port: 67,
                    destination_port: 68,
                    ..flow
                };
                self.write_to_tun(tun, &packet::udp_packet(c_reply_flow, &reply));
            }
            return;
        }
        let mut finish = false;
        if let Some(Session::Udp(session)) = self.sessions.get_mut(flow) {
            // C records the activity before `sendto`, including an EAGAIN
            // retry, and keeps the socket unconnected so replies from any
            // source remain visible to the VPN flow.
            session.last_activity = Instant::now();
            match session.socket.send_to(payload, session.target) {
                Ok(sent) => {
                    session.sent += sent as u64;
                }
                Err(error)
                    if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) => {}
                // `handle_udp` moves the flow to UDP_FINISHING on a hard
                // sendto error; it does not call the Java error callback.
                Err(_) => finish = true,
            }
        }
        if finish
            && let Some(usage) = self.sessions.finish_udp(flow)
            && (usage.sent != 0 || usage.received != 0)
        {
            callbacks.usage(flow, usage);
        }
    }

    /// Mirror `parse_dns_response`: report resolved records and, when the
    /// policy blocks the answer, return C's minimal replacement DNS message.
    fn rewrite_dns_response(
        rcode: u8,
        flow: Flow,
        response: &[u8],
        callbacks: &mut impl Callbacks,
    ) -> Option<Vec<u8>> {
        let parsed = dns::parse_response(response)?;
        for record in &parsed.records {
            callbacks.dns_resolved(record, -1);
        }
        let blocked = parsed.complete
            && (parsed.contains_service_binding
                || parsed
                    .question
                    .as_ref()
                    .is_some_and(|(name, _)| callbacks.domain_blocked(name)));
        if !blocked {
            return None;
        }
        let reply = dns::blocked_response(response, rcode)?;
        if let Some((name, question_type)) = parsed.question.as_ref() {
            callbacks.log_dns_blocked(&PacketInfo {
                flow,
                flags: String::new(),
                data: format!("qtype {question_type} qname {name} rcode {rcode}"),
                uid: 0,
            });
        }
        Some(reply)
    }

    // TCP state transitions must remain ordered with packet emission; splitting
    // this routine would force aliasing the session table during socket I/O.
    #[allow(clippy::too_many_arguments, clippy::too_many_lines)]
    fn handle_tcp(
        &mut self,
        tun: RawFd,
        flow: Flow,
        target: Redirect,
        uid: i32,
        header: packet::TcpHeader<'_>,
        callbacks: &mut impl Callbacks,
        allowed: bool,
    ) {
        // Urgent data is still ordinary in-band TCP payload for this stream
        // proxy; preserve it instead of dropping the complete segment.
        if header.flags.syn && self.sessions.get_mut(flow).is_none() {
            if self.sessions.active_len() >= self.max_sessions {
                return;
            }
            let local_sequence = match platform::secure_random_u32() {
                Ok(sequence) => sequence,
                Err(error) => {
                    platform::log(&format!("TCP random sequence setup failed: {error}"));
                    return;
                }
            };
            let address = SocketAddr::new(target.address, target.port);
            let proxy = (!target.redirected).then(|| self.socks5.clone()).flatten();
            let connect_address = proxy.as_ref().map_or(address, |config| config.endpoint);
            let mss = header
                .mss
                .unwrap_or_else(|| packet::default_mss(flow.version));
            let window_scale = header.window_scale.unwrap_or(0).min(14);
            let session = match TcpSession::open(
                connect_address,
                |fd| callbacks.protect_socket(fd),
                uid,
                local_sequence,
                header.sequence.wrapping_add(1),
                mss,
                window_scale,
                u32::from(header.window) << window_scale,
                window_scale,
                proxy.map(|config| crate::session::Socks5Session {
                    username: config.username,
                    password: config.password,
                }),
            ) {
                Ok(session) => session,
                Err(error) => {
                    // `open_tcp_socket` returns without an RST on setup or
                    // connect failure so the peer can retry its SYN.
                    platform::log(&format!("TCP socket setup failed: {error}"));
                    return;
                }
            };
            self.sessions.insert(flow, Session::Tcp(session));
            if !allowed {
                // C's blocked-SNI new-session path opens the socket first,
                // then writes a LISTEN-state RST/ACK and retains it briefly.
                let Some(Session::Tcp(session)) = self.sessions.get_mut(flow) else {
                    return;
                };
                let sequence = session.local_sequence;
                let acknowledgement = session.remote_sequence;
                session.state = TcpState::Closing;
                let _ = session;
                self.write_tcp(
                    tun,
                    flow,
                    sequence,
                    acknowledgement,
                    0,
                    TcpFlags {
                        rst: true,
                        ack: true,
                        ..TcpFlags::default()
                    },
                    &[],
                );
            }
            return;
        }
        let Some(Session::Tcp(session)) = self.sessions.get_mut(flow) else {
            self.write_tcp(
                tun,
                flow,
                header.acknowledgement,
                0,
                0,
                TcpFlags {
                    rst: true,
                    ..TcpFlags::default()
                },
                &[],
            );
            return;
        };
        if matches!(session.state, TcpState::Closing | TcpState::Closed) {
            let sequence = session.local_sequence;
            let acknowledgement = session.remote_sequence;
            let _ = session;
            self.write_tcp(
                tun,
                flow,
                sequence,
                acknowledgement,
                0,
                TcpFlags {
                    rst: true,
                    ..TcpFlags::default()
                },
                &[],
            );
            return;
        }
        // C does not refresh its session timer for a repeated SYN.
        if !header.flags.syn {
            session.last_activity = Instant::now();
        }
        // C clears this count for every packet from the tunnel; it charges the
        // headers of outstanding downstream packets against the peer window.
        session.unconfirmed = 0;
        session.send_window = u32::from(header.window) << session.send_scale;
        let mut blocked_sni = None;
        // C queues payload before ACK processing, but rejects payload after
        // its downstream fd is closed or after the remote FIN.
        if !header.payload.is_empty() {
            if session.local_socket_closed || session.state == TcpState::CloseWait {
                let sequence = session.local_sequence;
                let acknowledgement = session.remote_sequence;
                session.state = TcpState::Closing;
                let _ = session;
                self.write_tcp(
                    tun,
                    flow,
                    sequence,
                    acknowledgement,
                    0,
                    TcpFlags {
                        rst: true,
                        ..TcpFlags::default()
                    },
                    &[],
                );
                return;
            }
            let payload_sequence = header.sequence.wrapping_add(u32::from(header.flags.syn));
            if session.tls_inspected {
                session.queue(payload_sequence, header.payload, header.flags.psh);
            } else if session.tls_client_hello.is_empty() && header.payload[0] != 22 {
                session.tls_inspected = true;
                session.queue(payload_sequence, header.payload, header.flags.psh);
            } else {
                let expected = session.tls_sequence.map_or(payload_sequence, |start| {
                    start.wrapping_add(
                        u32::try_from(session.tls_client_hello.len()).unwrap_or(u32::MAX),
                    )
                });
                if payload_sequence == expected {
                    session.tls_sequence.get_or_insert(payload_sequence);
                    let total = session
                        .tls_client_hello
                        .len()
                        .saturating_add(header.payload.len());
                    if total > u16::MAX as usize {
                        // SNI inspection is best-effort.  An oversized hello
                        // must not retain unbounded memory or stall the TCP
                        // stream; pass the buffered bytes through unchanged.
                        let sequence = session.tls_sequence.take().unwrap_or(payload_sequence);
                        let buffered = std::mem::take(&mut session.tls_client_hello);
                        session.tls_inspected = true;
                        session.queue(sequence, &buffered, false);
                        session.queue(payload_sequence, header.payload, header.flags.psh);
                    } else {
                        session.tls_client_hello.extend_from_slice(header.payload);
                    }
                    if let Some(record_length) =
                        tls::record_length_if_complete(&session.tls_client_hello)
                    {
                        let sequence = session.tls_sequence.take().unwrap_or(payload_sequence);
                        let buffered = std::mem::take(&mut session.tls_client_hello);
                        session.tls_inspected = true;
                        let sni = tls::server_name(&buffered[..record_length]).unwrap_or_default();
                        if !sni.is_empty() {
                            callbacks.dns_resolved(
                                &dns::ResolvedRecord {
                                    question: sni.clone(),
                                    answer_name: sni.clone(),
                                    resource: flow.destination,
                                    ttl: -1,
                                },
                                uid,
                            );
                            if callbacks.domain_blocked(&sni) {
                                session.state = TcpState::Closing;
                                blocked_sni =
                                    Some((session.local_sequence, session.remote_sequence));
                            }
                        }
                        if blocked_sni.is_none() {
                            session.queue(sequence, &buffered, header.flags.psh);
                        }
                    }
                }
            }
        }
        if let Some((sequence, acknowledgement)) = blocked_sni {
            let _ = session;
            self.write_tcp(
                tun,
                flow,
                sequence,
                acknowledgement,
                0,
                TcpFlags {
                    rst: true,
                    ack: true,
                    ..TcpFlags::default()
                },
                &[],
            );
            return;
        }
        if header.flags.rst {
            session.state = TcpState::Closing;
            return;
        }
        // A SYN for an existing flow is only logged by C; it does not advance
        // handshake state even if it also carries ACK.
        if header.flags.syn {
            return;
        }
        if header.flags.ack {
            if header.acknowledgement.wrapping_add(1) == session.local_sequence {
                if session.state == TcpState::Established {
                    let _ = platform::enable_tcp_keepalive(session.socket.as_raw_fd());
                }
            } else {
                match sequence_order(header.acknowledgement, session.local_sequence) {
                    SequenceOrdering::Equal => session.acknowledged = header.acknowledgement,
                    SequenceOrdering::Less => {
                        if sequence_order(session.acknowledged, header.acknowledgement)
                            == SequenceOrdering::Less
                        {
                            session.acknowledged = header.acknowledgement;
                        }
                        return;
                    }
                    SequenceOrdering::Greater => {
                        session.state = TcpState::Closing;
                        let sequence = session.local_sequence;
                        let acknowledgement = session.remote_sequence;
                        let _ = session;
                        self.write_tcp(
                            tun,
                            flow,
                            sequence,
                            acknowledgement,
                            0,
                            TcpFlags {
                                rst: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        );
                        return;
                    }
                }
            }
        }
        if header.flags.ack {
            match session.state {
                TcpState::SynReceived => session.state = TcpState::Established,
                TcpState::LastAck => session.state = TcpState::Closing,
                _ => {}
            }
        }
        let fin = if header.flags.fin {
            session.pending_fin = Some(
                header
                    .sequence
                    .wrapping_add(u32::try_from(header.payload.len()).unwrap_or(u32::MAX)),
            );
            session.state = match session.state {
                TcpState::Established => TcpState::CloseWait,
                TcpState::FinWait1 => TcpState::Closed,
                state => state,
            };
            if session.forward.is_empty() {
                let acknowledgement = session
                    .pending_fin
                    .take()
                    .unwrap_or(session.remote_sequence)
                    .wrapping_add(1);
                session.remote_sequence = acknowledgement;
                Some((
                    session.local_sequence,
                    acknowledgement,
                    session.advertised_receive_window(),
                ))
            } else {
                None
            }
        } else {
            None
        };
        let _ = session;
        if let Some((sequence, acknowledgement, window)) = fin {
            self.write_tcp(
                tun,
                flow,
                sequence,
                acknowledgement,
                window,
                TcpFlags {
                    ack: true,
                    ..TcpFlags::default()
                },
                &[],
            );
        }
    }

    // A single drain owns the session-table iteration, preserving bounded
    // fairness across ICMP, UDP, TCP and SOCKS5 without aliasing sessions.
    #[allow(clippy::too_many_lines)]
    fn drain_sockets(&mut self, tun: RawFd, callbacks: &mut impl Callbacks) {
        let mut packets = Vec::new();
        let mut finish_udp = Vec::new();
        let dns_rcode = self.dns_rcode;
        for (flow, session) in self.sessions.icmp_sessions_mut() {
            if !matches!(platform::readable(session.socket.as_raw_fd()), Ok(true)) {
                continue;
            }
            let mut buffer = vec![0_u8; socket_datagram_limit(flow.version)];
            match platform::read(session.socket.as_raw_fd(), &mut buffer) {
                Ok(length) if length >= 6 => {
                    buffer.truncate(length);
                    buffer[4..6].copy_from_slice(&session.identifier.to_be_bytes());
                    if let Some(reply) = packet::icmp_reply(*flow, &buffer) {
                        session.last_activity = Instant::now();
                        packets.push(reply);
                    }
                }
                // Datagram sockets reporting EOF are terminal in C too.
                Ok(0) => session.stop = true,
                Ok(_) => {}
                Err(error)
                    if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) => {}
                Err(error) => {
                    platform::log(&format!("ICMP receive failed: {error}"));
                    session.stop = true;
                }
            }
        }
        for (flow, session) in self.sessions.udp_sessions_mut() {
            // UDP_YIELD in C bounds one ready socket to ten datagrams before
            // returning to the rest of the event loop.
            for _ in 0..10 {
                if !matches!(platform::readable(session.socket.as_raw_fd()), Ok(true)) {
                    break;
                }
                let mut buffer = vec![0_u8; session.receive_limit];
                match session.socket.recv_from(&mut buffer) {
                    Ok((length, source)) => {
                        if !session.accepts_reply_from(source) {
                            continue;
                        }
                        session.received += length as u64;
                        session.last_activity = Instant::now();
                        let mut response = buffer[..length].to_vec();
                        if flow.destination_port == 53
                            && let Some(reply) =
                                Self::rewrite_dns_response(dns_rcode, *flow, &response, callbacks)
                        {
                            response = reply;
                        }
                        packets.push(packet::udp_reply(*flow, &response));
                        // C changes DNS sessions to UDP_FINISHING after their
                        // first response, then retains UDP_CLOSED for one minute.
                        if flow.destination_port == 53 {
                            finish_udp.push(*flow);
                            break;
                        }
                    }
                    Err(error)
                        if matches!(
                            error.kind(),
                            ErrorKind::WouldBlock | ErrorKind::Interrupted
                        ) =>
                    {
                        break;
                    }
                    Err(error) => {
                        platform::log(&format!("UDP receive failed: {error}"));
                        finish_udp.push(*flow);
                        break;
                    }
                }
            }
        }
        for (flow, session) in self.sessions.tcp_sessions_mut() {
            if session.state == TcpState::Connecting {
                match platform::writable(session.socket.as_raw_fd()) {
                    Ok(false) => continue,
                    Err(error) => {
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ack: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        session.state = TcpState::Closing;
                        platform::log(&format!("TCP connect poll failed: {error}"));
                        continue;
                    }
                    Ok(true) => {}
                }
                match platform::socket_error(session.socket.as_raw_fd()) {
                    Ok(None) => {
                        if session.socks5.is_some() {
                            session.state = TcpState::SocksHello;
                            write_socks_hello(session);
                        } else {
                            packets.push(syn_ack(*flow, session));
                        }
                    }
                    Ok(Some(error)) => {
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ack: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        session.state = TcpState::Closing;
                        platform::log(&format!(
                            "TCP connect failed: {}",
                            io::Error::from_raw_os_error(error)
                        ));
                    }
                    Err(error) => {
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ack: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        session.state = TcpState::Closing;
                        platform::log(&format!("TCP connect status failed: {error}"));
                    }
                }
                continue;
            }
            if matches!(
                session.state,
                TcpState::SocksHello | TcpState::SocksAuthenticate | TcpState::SocksConnect
            ) {
                match flush_socks_write(session) {
                    Ok(false) => continue,
                    Ok(true) => {}
                    Err(error) => {
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ack: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        session.state = TcpState::Closing;
                        platform::log(&format!("SOCKS5 write failed: {error}"));
                        continue;
                    }
                }
                match progress_socks5(session, *flow) {
                    Ok(true) => packets.push(syn_ack(*flow, session)),
                    Ok(false) => {}
                    Err(error) => {
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ack: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        session.state = TcpState::Closing;
                        platform::log(&format!("SOCKS5 negotiation failed: {error}"));
                    }
                }
                continue;
            }
            if !matches!(session.state, TcpState::Established | TcpState::CloseWait) {
                continue;
            }
            let mut acknowledged = false;
            let mut reset = false;
            let mut receive_buffer =
                platform::tcp_send_buffer_available(session.socket.as_raw_fd());
            loop {
                let expected = session.remote_sequence;
                let mut complete = None;
                let write_result = {
                    let Some(segment) = session.forward.get_mut(&expected) else {
                        break;
                    };
                    if u32::try_from(segment.data.len().saturating_sub(segment.sent))
                        .unwrap_or(u32::MAX)
                        >= receive_buffer
                    {
                        break;
                    }
                    let result = platform::send_tcp(
                        session.socket.as_raw_fd(),
                        &segment.data[segment.sent..],
                        !segment.psh,
                    );
                    if let Ok(sent) = result {
                        segment.sent += sent;
                        complete =
                            (segment.sent == segment.data.len()).then_some(segment.data.len());
                    }
                    result
                };
                match write_result {
                    Ok(sent) => {
                        session.sent += sent as u64;
                        receive_buffer =
                            receive_buffer.saturating_sub(u32::try_from(sent).unwrap_or(u32::MAX));
                        acknowledged = acknowledged || sent > 0;
                        if let Some(length) = complete {
                            session.forward.remove(&expected);
                            session.remote_sequence = session
                                .remote_sequence
                                .wrapping_add(u32::try_from(length).unwrap_or(u32::MAX));
                        } else {
                            break;
                        }
                    }
                    Err(error)
                        if matches!(
                            error.kind(),
                            ErrorKind::WouldBlock | ErrorKind::Interrupted
                        ) =>
                    {
                        break;
                    }
                    Err(_) => {
                        session.state = TcpState::Closing;
                        reset = true;
                        break;
                    }
                }
            }
            if reset {
                packets.push(packet::tcp_reply(
                    *flow,
                    session.local_sequence,
                    session.remote_sequence,
                    0,
                    TcpFlags {
                        rst: true,
                        ..TcpFlags::default()
                    },
                    &[],
                ));
                continue;
            }
            let previous_window = session.receive_window;
            session.refresh_receive_window();
            if acknowledged || (previous_window == 0 && session.receive_window > 0) {
                packets.push(packet::tcp_reply(
                    *flow,
                    session.local_sequence,
                    session.remote_sequence,
                    session.advertised_receive_window(),
                    TcpFlags {
                        ack: true,
                        ..TcpFlags::default()
                    },
                    &[],
                ));
            }
            if session.forward.is_empty()
                && let Some(fin_sequence) = session.pending_fin.take()
            {
                session.remote_sequence = fin_sequence.wrapping_add(1);
                packets.push(packet::tcp_reply(
                    *flow,
                    session.local_sequence,
                    session.remote_sequence,
                    session.advertised_receive_window(),
                    TcpFlags {
                        ack: true,
                        ..TcpFlags::default()
                    },
                    &[],
                ));
            }
            let available_window = session.available_send_window();
            if available_window == 0 {
                let now = Instant::now();
                if session
                    .last_keep_alive
                    .is_none_or(|last| now.duration_since(last).as_millis() >= 100)
                {
                    // C temporarily decrements remote_seq for this ACK, then
                    // restores it. The packet is a TCP keep-alive probe that
                    // asks the peer to refresh its advertised send window.
                    packets.push(packet::tcp_reply(
                        *flow,
                        session.local_sequence,
                        session.remote_sequence.wrapping_sub(1),
                        session.advertised_receive_window(),
                        TcpFlags {
                            ack: true,
                            ..TcpFlags::default()
                        },
                        &[],
                    ));
                    session.last_keep_alive = Some(now);
                }
                continue;
            }
            let read_limit = usize::try_from(available_window)
                .unwrap_or(MTU)
                .min(usize::from(session.mss))
                .min(MTU);
            let mut buffer = vec![0_u8; read_limit];
            match session.socket.read(&mut buffer) {
                Ok(0) => {
                    if !session.forward.is_empty() {
                        session.state = TcpState::Closing;
                        packets.push(packet::tcp_reply(
                            *flow,
                            session.local_sequence,
                            session.remote_sequence,
                            0,
                            TcpFlags {
                                rst: true,
                                ..TcpFlags::default()
                            },
                            &[],
                        ));
                        continue;
                    }
                    let pending_dns = if flow.destination_port == 53 {
                        std::mem::take(&mut session.dns_response)
                    } else {
                        Vec::new()
                    };
                    packets.push(packet::tcp_reply(
                        *flow,
                        session.local_sequence,
                        session.remote_sequence,
                        session.advertised_receive_window(),
                        TcpFlags {
                            ack: true,
                            fin: true,
                            ..TcpFlags::default()
                        },
                        &pending_dns,
                    ));
                    session.local_sequence = session
                        .local_sequence
                        .wrapping_add(u32::try_from(pending_dns.len()).unwrap_or(u32::MAX))
                        .wrapping_add(1);
                    let _ = session.socket.shutdown(Shutdown::Both);
                    session.local_socket_closed = true;
                    session.state = match session.state {
                        TcpState::Established => TcpState::FinWait1,
                        TcpState::CloseWait => TcpState::LastAck,
                        state => state,
                    };
                }
                Ok(length) => {
                    session.received += length as u64;
                    session.last_activity = Instant::now();
                    let response = if flow.destination_port == 53 {
                        session.dns_response.extend_from_slice(&buffer[..length]);
                        rewrite_dns_tcp_frames(
                            &mut session.dns_response,
                            dns_rcode,
                            *flow,
                            callbacks,
                        )
                    } else {
                        buffer[..length].to_vec()
                    };
                    if response.is_empty() {
                        continue;
                    }
                    packets.push(packet::tcp_reply(
                        *flow,
                        session.local_sequence,
                        session.remote_sequence,
                        session.advertised_receive_window(),
                        TcpFlags {
                            ack: true,
                            psh: true,
                            ..TcpFlags::default()
                        },
                        &response,
                    ));
                    session.local_sequence = session
                        .local_sequence
                        .wrapping_add(u32::try_from(length).unwrap_or(u32::MAX));
                    session.unconfirmed = session.unconfirmed.saturating_add(1);
                }
                Err(error)
                    if matches!(error.kind(), ErrorKind::WouldBlock | ErrorKind::Interrupted) => {}
                Err(_) => {
                    session.state = TcpState::Closing;
                    packets.push(packet::tcp_reply(
                        *flow,
                        session.local_sequence,
                        session.remote_sequence,
                        0,
                        TcpFlags {
                            rst: true,
                            ..TcpFlags::default()
                        },
                        &[],
                    ));
                }
            }
        }
        for packet in packets {
            self.write_to_tun(tun, &packet);
        }
        for flow in finish_udp {
            if let Some(usage) = self.sessions.finish_udp(flow)
                && (usage.sent != 0 || usage.received != 0)
            {
                callbacks.usage(flow, usage);
            }
        }
    }

    // TCP's seven wire fields are kept adjacent at the packet boundary.
    #[allow(clippy::too_many_arguments)]
    fn write_tcp(
        &mut self,
        tun: RawFd,
        flow: Flow,
        sequence: u32,
        acknowledgement: u32,
        window: u16,
        flags: TcpFlags,
        payload: &[u8],
    ) {
        self.write_to_tun(
            tun,
            &packet::tcp_reply(flow, sequence, acknowledgement, window, flags, payload),
        );
    }

    fn write_to_tun(&mut self, tun: RawFd, packet: &[u8]) {
        if let Err(error) = write_packet(tun, packet) {
            platform::log(&format!("VPN tunnel write failed: {error}"));
            return;
        }
        if let Err(error) = self.pcap.write(packet) {
            platform::log(&format!("pcap write failed: {error}"));
        }
    }
}

#[cfg(test)]
mod tests {
    use std::{
        io::{ErrorKind, Read, Write},
        net::{IpAddr, Ipv4Addr, TcpListener},
        os::fd::AsRawFd,
        os::unix::net::UnixStream,
        sync::mpsc,
        thread,
    };

    use super::*;
    use crate::packet::{Packet, TcpHeader, Version};

    #[derive(Default)]
    struct TestCallbacks {
        block_domains: bool,
        deny_all: bool,
        blocked_dns: Vec<PacketInfo>,
        allowed_packets: Vec<PacketInfo>,
        resolved_records: Vec<dns::ResolvedRecord>,
        exits: Vec<String>,
    }

    impl Callbacks for TestCallbacks {
        fn uid_for(&mut self, _flow: Flow) -> i32 {
            42
        }

        fn allow(&mut self, packet: &PacketInfo) -> Option<Redirect> {
            self.allowed_packets.push(packet.clone());
            if self.deny_all {
                return None;
            }
            Some(Redirect {
                address: packet.flow.destination,
                port: packet.flow.destination_port,
                redirected: false,
            })
        }

        fn domain_blocked(&mut self, _name: &str) -> bool {
            self.block_domains
        }

        fn log_dns_blocked(&mut self, packet: &PacketInfo) {
            self.blocked_dns.push(packet.clone());
        }

        fn dns_resolved(&mut self, record: &dns::ResolvedRecord, _uid: i32) {
            self.resolved_records.push(record.clone());
        }

        fn protect_socket(&mut self, _fd: RawFd) -> io::Result<()> {
            Ok(())
        }

        fn usage(&mut self, _flow: Flow, _usage: Usage) {}

        fn report_exit(&mut self, message: &str) {
            self.exits.push(message.to_owned());
        }
    }

    #[test]
    fn accepts_the_correct_echo_request_type_for_each_ip_version() {
        assert!(is_echo_request(Version::V4, 8));
        assert!(!is_echo_request(Version::V4, 128));
        assert!(is_echo_request(Version::V6, 128));
        assert!(!is_echo_request(Version::V6, 8));
    }

    #[test]
    fn clears_sessions_after_a_run_exit() {
        let (tun, peer) = UnixStream::pair().expect("tunnel pair");
        drop(peer); // Make the engine observe C's tunnel-EOF exit path.
        let flow = Flow {
            version: Version::V4,
            protocol: packet::UDP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(203, 0, 113, 1)),
            source_port: 40000,
            destination_port: 123,
        };
        let mut engine = Engine::new();
        engine.sessions.insert(
            flow,
            Session::ClosedUdp {
                last_activity: Instant::now(),
            },
        );
        let wake = platform::WakePipe::new().expect("wake pipe");
        let stopping = AtomicBool::new(false);
        let clear = AtomicBool::new(false);
        let stats = std::array::from_fn(|_| AtomicI32::new(0));
        let mut callbacks = TestCallbacks::default();
        engine.run(
            tun.as_raw_fd(),
            wake.reader(),
            &stopping,
            &clear,
            &stats,
            &mut callbacks,
            &mut |_| {},
        );

        assert!(engine.sessions.get_mut(flow).is_none());
        assert_eq!(callbacks.exits.len(), 1);
    }

    #[test]
    fn uses_the_original_socket_datagram_limits() {
        assert_eq!(socket_datagram_limit(Version::V4), 65_507);
        assert_eq!(socket_datagram_limit(Version::V6), 65_487);
    }

    #[test]
    fn validates_socks5_connect_reply_address_forms() {
        assert!(socks_connect_reply(&[5, 0, 0, 1, 127, 0, 0, 1, 0, 80]));
        assert!(socks_connect_reply(&[
            5, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 80
        ]));
        assert!(socks_connect_reply(&[
            5, 0, 0, 3, 3, b'f', b'o', b'o', 0, 80
        ]));
        assert!(!socks_connect_reply(&[5, 5, 0, 1, 127, 0, 0, 1, 0, 80]));
        assert!(!socks_connect_reply(&[
            5, 0, 42, 3, 3, b'f', b'o', b'o', 0, 80
        ]));
        assert_eq!(
            socks_connect_reply_length(&[5, 0, 0, 1]).expect("partial reply"),
            None
        );
    }

    #[test]
    fn orders_wrapping_tcp_sequences_like_rfc_1982() {
        assert_eq!(sequence_order(10, 10), SequenceOrdering::Equal);
        assert_eq!(sequence_order(10, 11), SequenceOrdering::Less);
        assert_eq!(sequence_order(u32::MAX, 0), SequenceOrdering::Less);
        assert_eq!(sequence_order(0, u32::MAX), SequenceOrdering::Greater);
    }

    #[test]
    fn rewrites_blocked_dns_for_udp_and_tcp_payloads() {
        let dns = [
            0x12, 0x34, 0x81, 0x80, 0, 1, 0, 1, 0, 0, 0, 0, 3, b'w', b'w', b'w', 7, b'e', b'x',
            b'a', b'm', b'p', b'l', b'e', 3, b'c', b'o', b'm', 0, 0, 1, 0, 1, 0xc0, 0x0c, 0, 1, 0,
            1, 0, 0, 0, 60, 0, 4, 93, 184, 216, 34,
        ];
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(8, 8, 8, 8)),
            source_port: 42000,
            destination_port: 53,
        };
        let mut callbacks = TestCallbacks {
            block_domains: true,
            ..TestCallbacks::default()
        };
        let replacement = Engine::rewrite_dns_response(3, flow, &dns, &mut callbacks)
            .expect("blocked DNS response");
        assert_eq!(replacement.len(), 33);
        assert_eq!(&replacement[6..12], &[0; 6]);
        assert_eq!(callbacks.blocked_dns.len(), 1);
        assert_eq!(
            callbacks.blocked_dns[0].data,
            "qtype 1 qname www.example.com rcode 3"
        );

        let mut tcp_payload = Vec::from((dns.len() as u16).to_be_bytes());
        tcp_payload.extend_from_slice(&dns);
        tcp_payload[2..2 + replacement.len()].copy_from_slice(&replacement);
        // C preserves the framing length while replacing the DNS header and
        // question in-place for a TCP read.
        assert_eq!(&tcp_payload[..2], &(dns.len() as u16).to_be_bytes());
        assert_eq!(&tcp_payload[8..14], &[0; 6]);

        let mut framed = Vec::from((dns.len() as u16).to_be_bytes());
        framed.extend_from_slice(&dns);
        let split = 9;
        let mut buffered = framed[..split].to_vec();
        assert!(rewrite_dns_tcp_frames(&mut buffered, 3, flow, &mut callbacks).is_empty());
        assert_eq!(buffered, framed[..split]);
        buffered.extend_from_slice(&framed[split..]);
        let rewritten = rewrite_dns_tcp_frames(&mut buffered, 3, flow, &mut callbacks);
        assert!(buffered.is_empty());
        let length = usize::from(u16::from_be_bytes([rewritten[0], rewritten[1]]));
        assert_eq!(length + 2, rewritten.len());
        assert_eq!(&rewritten[8..14], &[0; 6]);
    }

    #[test]
    fn reports_tls_sni_and_uses_the_c_policy_packet_marker() {
        let host = b"example.com";
        let extension_length = 2 + 1 + 2 + host.len();
        let extensions_length = 4 + extension_length + 4;
        let handshake_length = 2 + 32 + 1 + 2 + 2 + 1 + 1 + 2 + extensions_length;
        let record_length = 1 + 3 + handshake_length;
        let mut hello = vec![22, 3, 3];
        hello.extend_from_slice(&(record_length as u16).to_be_bytes());
        hello.push(1);
        hello.extend_from_slice(&(handshake_length as u32).to_be_bytes()[1..]);
        hello.extend_from_slice(&[3, 3]);
        hello.extend_from_slice(&[0; 32]);
        hello.push(0);
        hello.extend_from_slice(&2_u16.to_be_bytes());
        hello.extend_from_slice(&[0x13, 1]);
        hello.extend_from_slice(&[1, 0]);
        hello.extend_from_slice(&(extensions_length as u16).to_be_bytes());
        hello.extend_from_slice(&0_u16.to_be_bytes());
        hello.extend_from_slice(&(extension_length as u16).to_be_bytes());
        hello.extend_from_slice(&(1 + 2 + host.len() as u16).to_be_bytes());
        hello.push(0);
        hello.extend_from_slice(&(host.len() as u16).to_be_bytes());
        hello.extend_from_slice(host);
        hello.extend_from_slice(&[0, 10, 0, 0]);

        let (tun, _peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(203, 0, 113, 1)),
            source_port: 42000,
            destination_port: 443,
        };
        let packet = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 100,
                acknowledgement: 0,
                window: u16::MAX,
                flags: TcpFlags {
                    syn: true,
                    ..TcpFlags::default()
                },
                payload: &hello,
                mss: Some(1460),
                window_scale: Some(0),
            }),
        };
        let mut engine = Engine::new();
        let mut callbacks = TestCallbacks {
            block_domains: true,
            ..TestCallbacks::default()
        };
        engine.handle_packet(tun.as_raw_fd(), packet, &mut callbacks);

        assert_eq!(callbacks.allowed_packets.len(), 1);
        assert_eq!(callbacks.allowed_packets[0].data, "sni");
        assert_eq!(callbacks.resolved_records.len(), 1);
        assert_eq!(callbacks.resolved_records[0].question, "example.com");
        assert_eq!(callbacks.resolved_records[0].ttl, -1);
    }

    #[test]
    fn resets_a_tcp_packet_for_an_unknown_session() {
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(198, 51, 100, 1)),
            source_port: 42000,
            destination_port: 443,
        };
        let packet = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 123,
                acknowledgement: 456,
                window: 0,
                flags: TcpFlags {
                    ack: true,
                    ..TcpFlags::default()
                },
                payload: &[],
                mss: None,
                window_scale: None,
            }),
        };
        let mut engine = Engine::new();
        engine.handle_packet(tun.as_raw_fd(), packet, &mut TestCallbacks::default());
        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("RST packet");
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = packet::parse(&reply[..length]).expect("parsed RST")
        else {
            panic!("expected TCP RST");
        };
        assert!(header.flags.rst);
        assert!(!header.flags.ack);
        assert_eq!(header.sequence, 456);
    }

    #[test]
    fn dhcp_reply_retains_c_session_mutation_and_wire_endpoints() {
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::UDP,
            source: IpAddr::V4(Ipv4Addr::UNSPECIFIED),
            destination: IpAddr::V4(Ipv4Addr::BROADCAST),
            source_port: 68,
            destination_port: 67,
        };
        let mut request = vec![0_u8; 300];
        request[0] = 1;
        request[1] = 1;
        request[2] = 6;
        request[236..240].copy_from_slice(&[99, 130, 83, 99]);
        let mut engine = Engine::new();
        engine.handle_packet(
            tun.as_raw_fd(),
            Packet {
                flow,
                transport: Transport::Udp { payload: &request },
            },
            &mut TestCallbacks::default(),
        );
        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("DHCP reply");
        let packet = packet::parse(&reply[..length]).expect("parsed DHCP reply");
        assert_eq!(packet.flow.source, flow.destination);
        assert_eq!(
            packet.flow.destination,
            IpAddr::V4(Ipv4Addr::new(10, 1, 10, 1))
        );
        assert!(engine.sessions.get_mut(flow).is_none());
        assert!(
            engine
                .sessions
                .get_mut(Flow {
                    source: IpAddr::V4(Ipv4Addr::new(10, 1, 10, 1)),
                    ..flow
                })
                .is_some()
        );
    }

    #[test]
    fn rechecks_retained_sessions_before_a_new_run() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("listener");
        let port = listener.local_addr().expect("listener address").port();
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 42000,
            destination_port: port,
        };
        let mut tcp = TcpSession::open(
            SocketAddr::new(flow.destination, port),
            |_| Ok(()),
            42,
            200,
            101,
            1460,
            0,
            u16::MAX.into(),
            0,
            None,
        )
        .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");
        tcp.state = TcpState::Established;
        let blocked_flow = Flow {
            protocol: packet::UDP,
            destination_port: 53,
            ..flow
        };
        let mut engine = Engine::new();
        engine.sessions.insert(flow, Session::Tcp(tcp));
        engine.sessions.insert(
            blocked_flow,
            Session::BlockedUdp {
                last_activity: Instant::now(),
            },
        );
        let mut callbacks = TestCallbacks {
            deny_all: true,
            ..TestCallbacks::default()
        };
        engine.recheck_allowed(tun.as_raw_fd(), &mut callbacks);

        assert!(engine.sessions.get_mut(blocked_flow).is_none());
        assert!(matches!(
            engine.sessions.get_mut(flow),
            Some(Session::Tcp(TcpSession {
                state: TcpState::Closing,
                ..
            }))
        ));
        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("RST packet");
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = packet::parse(&reply[..length]).expect("parsed RST")
        else {
            panic!("expected TCP RST");
        };
        assert!(header.flags.rst && !header.flags.ack);
        assert_eq!(callbacks.allowed_packets.len(), 1);
    }

    #[test]
    fn resets_packets_for_lingering_closed_tcp_sessions() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("listener");
        let port = listener.local_addr().expect("listener address").port();
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 42000,
            destination_port: port,
        };
        let mut session = TcpSession::open(
            SocketAddr::new(flow.destination, port),
            |_| Ok(()),
            42,
            200,
            101,
            1460,
            0,
            u16::MAX.into(),
            0,
            None,
        )
        .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");
        session.state = TcpState::Closed;
        let mut engine = Engine::new();
        engine.sessions.insert(flow, Session::Tcp(session));
        engine.handle_packet(
            tun.as_raw_fd(),
            Packet {
                flow,
                transport: Transport::Tcp(TcpHeader {
                    sequence: 101,
                    acknowledgement: 200,
                    window: u16::MAX,
                    flags: TcpFlags {
                        ack: true,
                        ..TcpFlags::default()
                    },
                    payload: &[],
                    mss: None,
                    window_scale: None,
                }),
            },
            &mut TestCallbacks::default(),
        );
        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("RST packet");
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = packet::parse(&reply[..length]).expect("parsed RST")
        else {
            panic!("expected TCP RST");
        };
        assert!(header.flags.rst && !header.flags.ack);
        assert_eq!((header.sequence, header.acknowledgement), (200, 101));
    }

    #[test]
    fn resets_future_tcp_acknowledgements() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("listener");
        let port = listener.local_addr().expect("listener address").port();
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 42000,
            destination_port: port,
        };
        let mut session = TcpSession::open(
            SocketAddr::new(flow.destination, port),
            |_| Ok(()),
            42,
            200,
            101,
            1460,
            0,
            u16::MAX.into(),
            0,
            None,
        )
        .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");
        session.state = TcpState::Established;
        let mut engine = Engine::new();
        engine.sessions.insert(flow, Session::Tcp(session));
        engine.handle_packet(
            tun.as_raw_fd(),
            Packet {
                flow,
                transport: Transport::Tcp(TcpHeader {
                    sequence: 101,
                    acknowledgement: 201,
                    window: u16::MAX,
                    flags: TcpFlags {
                        ack: true,
                        ..TcpFlags::default()
                    },
                    payload: &[],
                    mss: None,
                    window_scale: None,
                }),
            },
            &mut TestCallbacks::default(),
        );
        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("RST packet");
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = packet::parse(&reply[..length]).expect("parsed RST")
        else {
            panic!("expected TCP RST");
        };
        assert!(header.flags.rst && !header.flags.ack);
        assert!(matches!(
            engine.sessions.get_mut(flow),
            Some(Session::Tcp(TcpSession {
                state: TcpState::Closing,
                ..
            }))
        ));
    }

    #[test]
    fn probes_a_zero_tcp_send_window_like_c() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("listener");
        let port = listener.local_addr().expect("listener address").port();
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 42000,
            destination_port: port,
        };
        let mut session = TcpSession::open(
            SocketAddr::new(flow.destination, port),
            |_| Ok(()),
            42,
            200,
            100,
            1460,
            0,
            0,
            0,
            None,
        )
        .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");
        session.state = TcpState::Established;
        session.acknowledged = 200;
        session.send_window = 0;
        let mut engine = Engine::new();
        engine.sessions.insert(flow, Session::Tcp(session));
        engine.drain_sockets(tun.as_raw_fd(), &mut TestCallbacks::default());

        let mut reply = [0_u8; MTU];
        let length = peer.read(&mut reply).expect("keep-alive probe");
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = packet::parse(&reply[..length]).expect("parsed probe")
        else {
            panic!("expected TCP probe");
        };
        assert!(header.flags.ack);
        assert_eq!(header.sequence, 200);
        assert_eq!(header.acknowledgement, 99);
    }

    #[test]
    fn proxies_a_tcp_handshake_and_downstream_data_to_the_tunnel() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("listener");
        listener
            .set_nonblocking(true)
            .expect("nonblocking listener");
        let port = listener.local_addr().expect("listener address").port();
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        peer.set_nonblocking(true).expect("nonblocking peer");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 12345,
            destination_port: port,
        };
        let syn = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 100,
                acknowledgement: 0,
                window: u16::MAX,
                flags: TcpFlags {
                    syn: true,
                    ..TcpFlags::default()
                },
                payload: &[],
                mss: Some(1460),
                window_scale: Some(0),
            }),
        };
        let mut engine = Engine::new();
        let mut callbacks = TestCallbacks::default();
        engine.handle_packet(tun.as_raw_fd(), syn, &mut callbacks);

        let mut reply = [0_u8; MTU];
        let syn_ack = loop {
            engine.drain_sockets(tun.as_raw_fd(), &mut callbacks);
            match peer.read(&mut reply) {
                Ok(length) => break packet::parse(&reply[..length]).expect("SYN/ACK packet"),
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("tunnel read failed: {error}"),
            }
        };
        let Transport::Tcp(syn_ack_header) = syn_ack.transport else {
            panic!("expected TCP SYN/ACK");
        };
        assert!(syn_ack_header.flags.syn && syn_ack_header.flags.ack);

        let ack = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 101,
                acknowledgement: syn_ack_header.sequence.wrapping_add(1),
                window: u16::MAX,
                flags: TcpFlags {
                    ack: true,
                    ..TcpFlags::default()
                },
                payload: &[],
                mss: None,
                window_scale: None,
            }),
        };
        engine.handle_packet(tun.as_raw_fd(), ack, &mut callbacks);
        let mut server = loop {
            match listener.accept() {
                Ok((stream, _)) => break stream,
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("accept failed: {error}"),
            }
        };
        let client_data = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 101,
                acknowledgement: syn_ack_header.sequence.wrapping_add(1),
                window: u16::MAX,
                flags: TcpFlags {
                    ack: true,
                    psh: true,
                    ..TcpFlags::default()
                },
                payload: b"in",
                mss: None,
                window_scale: None,
            }),
        };
        engine.handle_packet(tun.as_raw_fd(), client_data, &mut callbacks);
        let mut upstream = [0_u8; 2];
        loop {
            engine.drain_sockets(tun.as_raw_fd(), &mut callbacks);
            match server.read_exact(&mut upstream) {
                Ok(()) => break,
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("upstream data failed: {error}"),
            }
        }
        assert_eq!(&upstream, b"in");
        let acknowledgement = loop {
            match peer.read(&mut reply) {
                Ok(length) => {
                    break packet::parse(&reply[..length]).expect("acknowledgement packet");
                }
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("tunnel read failed: {error}"),
            }
        };
        let Transport::Tcp(acknowledgement_header) = acknowledgement.transport else {
            panic!("expected TCP acknowledgement");
        };
        assert!(acknowledgement_header.flags.ack);
        assert_eq!(acknowledgement_header.acknowledgement, 103);
        server.write_all(b"ok").expect("server write");

        let downstream = loop {
            engine.drain_sockets(tun.as_raw_fd(), &mut callbacks);
            match peer.read(&mut reply) {
                Ok(length) => break packet::parse(&reply[..length]).expect("downstream packet"),
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("tunnel read failed: {error}"),
            }
        };
        let Transport::Tcp(header) = downstream.transport else {
            panic!("expected TCP data packet");
        };
        assert_eq!(header.payload, b"ok");
        assert!(header.flags.ack && header.flags.psh);
        drop(server);
        let fin = loop {
            engine.drain_sockets(tun.as_raw_fd(), &mut callbacks);
            match peer.read(&mut reply) {
                Ok(length) => break packet::parse(&reply[..length]).expect("FIN packet"),
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("tunnel read failed: {error}"),
            }
        };
        let Transport::Tcp(fin_header) = fin.transport else {
            panic!("expected TCP FIN");
        };
        assert!(fin_header.flags.fin && fin_header.flags.ack);
        let client_fin = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 103,
                acknowledgement: fin_header.sequence.wrapping_add(1),
                window: u16::MAX,
                flags: TcpFlags {
                    ack: true,
                    fin: true,
                    ..TcpFlags::default()
                },
                payload: &[],
                mss: None,
                window_scale: None,
            }),
        };
        engine.handle_packet(tun.as_raw_fd(), client_fin, &mut callbacks);
        let length = peer.read(&mut reply).expect("final ACK");
        let Packet {
            transport: Transport::Tcp(final_ack),
            ..
        } = packet::parse(&reply[..length]).expect("parsed final ACK")
        else {
            panic!("expected TCP final ACK");
        };
        assert!(final_ack.flags.ack && !final_ack.flags.fin);
        assert_eq!(final_ack.acknowledgement, 104);
    }

    #[test]
    fn negotiates_socks5_before_exposing_the_tunnel_syn_ack() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("SOCKS listener");
        let endpoint = listener.local_addr().expect("SOCKS address");
        let (request_tx, request_rx) = mpsc::channel();
        let server = thread::spawn(move || {
            let (mut stream, _) = listener.accept().expect("SOCKS accept");
            let mut hello = [0_u8; 4];
            stream.read_exact(&mut hello).expect("SOCKS hello");
            assert_eq!(hello, [5, 2, 0, 2]);
            stream.write_all(&[5, 0]).expect("SOCKS method response");
            let mut connect = [0_u8; 10];
            stream.read_exact(&mut connect).expect("SOCKS connect");
            request_tx.send(connect).expect("send connect request");
            stream
                .write_all(&[5, 0, 0, 1, 0, 0, 0, 0, 0, 0])
                .expect("SOCKS success response");
        });
        let (tun, mut peer) = UnixStream::pair().expect("tunnel pair");
        peer.set_nonblocking(true).expect("nonblocking peer");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(203, 0, 113, 7)),
            source_port: 32100,
            destination_port: 443,
        };
        let syn = Packet {
            flow,
            transport: Transport::Tcp(TcpHeader {
                sequence: 500,
                acknowledgement: 0,
                window: u16::MAX,
                flags: TcpFlags {
                    syn: true,
                    ..TcpFlags::default()
                },
                payload: &[],
                mss: Some(1460),
                window_scale: Some(0),
            }),
        };
        let mut engine = Engine::new();
        engine.configure_socks5(Some(Socks5Config {
            endpoint,
            username: String::new(),
            password: String::new(),
        }));
        let mut callbacks = TestCallbacks::default();
        engine.handle_packet(tun.as_raw_fd(), syn, &mut callbacks);

        let mut reply = [0_u8; MTU];
        let packet = loop {
            engine.drain_sockets(tun.as_raw_fd(), &mut callbacks);
            match peer.read(&mut reply) {
                Ok(length) => break packet::parse(&reply[..length]).expect("SYN/ACK packet"),
                Err(error) if error.kind() == ErrorKind::WouldBlock => thread::yield_now(),
                Err(error) => panic!("tunnel read failed: {error}"),
            }
        };
        let Transport::Tcp(header) = packet.transport else {
            panic!("expected TCP SYN/ACK");
        };
        assert!(header.flags.syn && header.flags.ack);
        assert_eq!(
            request_rx.recv().expect("SOCKS connect request"),
            [5, 1, 0, 1, 203, 0, 113, 7, 1, 187]
        );
        server.join().expect("SOCKS server");
    }
}
