//! Session ownership and protocol state. `HashMap` replaces the C linked list,
//! so every session and queued byte buffer has one clear owner.

use std::{
    collections::{BTreeMap, HashMap},
    io,
    net::{IpAddr, Shutdown, SocketAddr},
    os::fd::AsRawFd,
    time::{Duration, Instant},
};

use socket2::{Domain, Protocol, Socket, Type};

use crate::{packet::Flow, platform};

const MAX_REASSEMBLY_AHEAD: u64 = 1 << 20;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct SessionKey(pub(crate) Flow);

#[derive(Debug, Default, Clone, Copy)]
pub(crate) struct Statistics {
    pub(crate) icmp: i32,
    pub(crate) udp: i32,
    pub(crate) tcp: i32,
}

#[derive(Debug, Default)]
pub(crate) struct TimeoutActions {
    pub(crate) tcp_resets: Vec<(Flow, u32, u32)>,
    pub(crate) usages: Vec<(Flow, Usage)>,
}

#[derive(Debug)]
pub(crate) struct SessionTable {
    sessions: HashMap<SessionKey, Session>,
}

impl SessionTable {
    pub(crate) fn new() -> Self {
        Self {
            sessions: HashMap::new(),
        }
    }

    pub(crate) fn statistics(&self) -> Statistics {
        self.sessions
            .values()
            .fold(Statistics::default(), |mut stats, session| {
                match session {
                    Session::Icmp(icmp) if !icmp.stop => stats.icmp += 1,
                    Session::Udp(_) => stats.udp += 1,
                    Session::Tcp(tcp)
                        if !matches!(tcp.state, TcpState::Closing | TcpState::Closed) =>
                    {
                        stats.tcp += 1;
                    }
                    Session::Icmp(_)
                    | Session::BlockedUdp { .. }
                    | Session::ClosedUdp { .. }
                    | Session::Tcp(_) => {}
                }
                stats
            })
    }

    pub(crate) fn get_mut(&mut self, flow: Flow) -> Option<&mut Session> {
        self.sessions.get_mut(&SessionKey(flow))
    }
    pub(crate) fn insert(&mut self, flow: Flow, session: Session) {
        self.sessions.insert(SessionKey(flow), session);
    }
    pub(crate) fn remove(&mut self, flow: Flow) -> Option<Session> {
        self.sessions.remove(&SessionKey(flow))
    }
    pub(crate) fn rekey(&mut self, from: Flow, to: Flow) {
        if let Some(session) = self.sessions.remove(&SessionKey(from)) {
            self.sessions.insert(SessionKey(to), session);
        }
    }
    pub(crate) fn clear(&mut self) {
        self.sessions.clear();
    }
    pub(crate) fn flows(&self) -> Vec<Flow> {
        self.sessions.keys().map(|key| key.0).collect()
    }
    pub(crate) fn active_icmp_flow(&self, incoming: Flow) -> Option<Flow> {
        self.sessions.iter().find_map(|(key, session)| {
            (matches!(session, Session::Icmp(icmp) if !icmp.stop) && key.0 == incoming)
                .then_some(key.0)
        })
    }
    pub(crate) fn active_len(&self) -> usize {
        self.sessions
            .values()
            .filter(|session| match session {
                Session::Icmp(icmp) => !icmp.stop,
                Session::Udp(_) => true,
                Session::BlockedUdp { .. } | Session::ClosedUdp { .. } => false,
                Session::Tcp(tcp) => !matches!(tcp.state, TcpState::Closing | TcpState::Closed),
            })
            .count()
    }

    /// Advance sessions through the same timeout states as C rather than
    /// dropping active sockets immediately.  TCP closes linger for five
    /// minutes; UDP closed/blocked flows linger for one minute.
    pub(crate) fn advance_timeouts(&mut self, maximum_sessions: usize) -> TimeoutActions {
        let now = Instant::now();
        let sessions = self.active_len();
        let expired: Vec<_> = self
            .sessions
            .iter()
            .filter_map(|(key, session)| {
                session
                    .expired(now, sessions, maximum_sessions)
                    .then_some(*key)
            })
            .collect();
        let mut actions = TimeoutActions::default();
        for key in expired {
            let Some(session) = self.sessions.remove(&key) else {
                continue;
            };
            match session {
                Session::Icmp(_) | Session::BlockedUdp { .. } | Session::ClosedUdp { .. } => {}
                Session::Udp(udp) => {
                    let usage = udp.usage();
                    self.sessions
                        .insert(key, Session::ClosedUdp { last_activity: now });
                    if usage.sent != 0 || usage.received != 0 {
                        actions.usages.push((key.0, usage));
                    }
                }
                Session::Tcp(mut tcp) => {
                    // TCP_LISTEN (Connecting and SOCKS negotiation) is
                    // closed silently in C. All other active states send RST.
                    if !matches!(
                        tcp.state,
                        TcpState::Connecting
                            | TcpState::SocksHello
                            | TcpState::SocksAuthenticate
                            | TcpState::SocksConnect
                    ) {
                        actions
                            .tcp_resets
                            .push((key.0, tcp.local_sequence, tcp.remote_sequence));
                    }
                    tcp.state = TcpState::Closing;
                    self.sessions.insert(key, Session::Tcp(tcp));
                }
            }
        }
        actions
    }

    pub(crate) fn finish_closing(&mut self) -> Vec<(Flow, Usage)> {
        let now = Instant::now();
        self.sessions
            .iter_mut()
            .filter_map(|(key, session)| match session {
                Session::Tcp(tcp) if tcp.state == TcpState::Closing => {
                    let _ = tcp.socket.shutdown(Shutdown::Both);
                    tcp.state = TcpState::Closed;
                    tcp.last_activity = now;
                    let usage = Usage {
                        uid: tcp.uid,
                        sent: std::mem::take(&mut tcp.sent),
                        received: std::mem::take(&mut tcp.received),
                    };
                    (usage.sent != 0 || usage.received != 0).then_some((key.0, usage))
                }
                _ => None,
            })
            .collect()
    }

    /// Drop the UDP socket but retain the flow key through `UDP_KEEP_TIMEOUT`,
    /// matching the C `UDP_CLOSED` state and absorbing retransmissions.
    pub(crate) fn finish_udp(&mut self, flow: Flow) -> Option<Usage> {
        let session = self.sessions.remove(&SessionKey(flow))?;
        let Session::Udp(udp) = session else {
            self.sessions.insert(SessionKey(flow), session);
            return None;
        };
        let usage = udp.usage();
        self.sessions.insert(
            SessionKey(flow),
            Session::ClosedUdp {
                last_activity: Instant::now(),
            },
        );
        Some(usage)
    }

    pub(crate) fn udp_sessions_mut(&mut self) -> impl Iterator<Item = (&Flow, &mut UdpSession)> {
        self.sessions
            .iter_mut()
            .filter_map(|(key, session)| match session {
                Session::Udp(udp) => Some((&key.0, udp)),
                _ => None,
            })
    }

    pub(crate) fn icmp_sessions_mut(&mut self) -> impl Iterator<Item = (&Flow, &mut IcmpSession)> {
        self.sessions
            .iter_mut()
            .filter_map(|(key, session)| match session {
                Session::Icmp(icmp) if !icmp.stop => Some((&key.0, icmp)),
                _ => None,
            })
    }

    pub(crate) fn tcp_sessions_mut(&mut self) -> impl Iterator<Item = (&Flow, &mut TcpSession)> {
        self.sessions
            .iter_mut()
            .filter_map(|(key, session)| match session {
                Session::Tcp(tcp) => Some((&key.0, tcp)),
                _ => None,
            })
    }
}

#[derive(Debug)]
pub(crate) enum Session {
    Icmp(IcmpSession),
    Udp(UdpSession),
    BlockedUdp { last_activity: Instant },
    ClosedUdp { last_activity: Instant },
    Tcp(TcpSession),
}

impl Session {
    fn expired(&self, now: Instant, sessions: usize, maximum_sessions: usize) -> bool {
        let (last_activity, timeout) = match self {
            Self::Icmp(session) if session.stop => (session.last_activity, Duration::ZERO),
            Self::Icmp(session) => (
                session.last_activity,
                scaled_timeout(5, sessions, maximum_sessions),
            ),
            Self::Udp(session) => (
                session.last_activity,
                scaled_timeout(
                    if session.destination_port == 53 {
                        15
                    } else {
                        300
                    },
                    sessions,
                    maximum_sessions,
                ),
            ),
            Self::BlockedUdp { last_activity, .. } | Self::ClosedUdp { last_activity } => {
                (*last_activity, Duration::from_mins(1))
            }
            Self::Tcp(session) if session.state == TcpState::Closed => {
                (session.last_activity, Duration::from_mins(5))
            }
            Self::Tcp(session) => (
                session.last_activity,
                scaled_timeout(session.timeout_seconds(), sessions, maximum_sessions),
            ),
        };
        now.duration_since(last_activity) > timeout
    }
}

fn scaled_timeout(seconds: u64, sessions: usize, maximum_sessions: usize) -> Duration {
    let maximum = maximum_sessions.max(1);
    // Preserve C's integer operation order: `100 - sessions * 100 / max`.
    // Algebraically equivalent real-number forms round differently here.
    let scale = 100_usize.saturating_sub(sessions.saturating_mul(100) / maximum);
    Duration::from_secs(seconds.saturating_mul(scale as u64) / 100)
}

#[derive(Debug)]
pub(crate) struct IcmpSession {
    pub(crate) socket: Socket,
    pub(crate) identifier: u16,
    pub(crate) uid: i32,
    pub(crate) last_activity: Instant,
    pub(crate) stop: bool,
}

impl IcmpSession {
    // TCP setup mirrors the wire handshake fields one-for-one; a parameter
    // object would obscure that correspondence at its sole call site.
    #[allow(clippy::too_many_arguments)]
    pub(crate) fn open(
        destination: IpAddr,
        identifier: u16,
        uid: i32,
        protect: impl FnOnce(i32) -> io::Result<()>,
    ) -> io::Result<Self> {
        let (domain, protocol) = match destination {
            IpAddr::V4(_) => (Domain::IPV4, Protocol::ICMPV4),
            IpAddr::V6(_) => (Domain::IPV6, Protocol::ICMPV6),
        };
        let socket = Socket::new(domain, Type::DGRAM, Some(protocol))?;
        protect(socket.as_raw_fd())?;
        Ok(Self {
            socket,
            identifier,
            uid,
            last_activity: Instant::now(),
            stop: false,
        })
    }
}

#[derive(Debug)]
pub(crate) struct UdpSession {
    pub(crate) socket: std::net::UdpSocket,
    /// Destination selected by policy. C keeps its UDP socket unconnected and
    /// passes this address to `sendto` for each payload.
    pub(crate) target: SocketAddr,
    /// The tunnel flow's destination port, retained even when policy
    /// redirects to another endpoint; C uses this for DNS timeout handling.
    pub(crate) destination_port: u16,
    pub(crate) receive_limit: usize,
    pub(crate) uid: i32,
    pub(crate) last_activity: Instant,
    pub(crate) sent: u64,
    pub(crate) received: u64,
}

impl UdpSession {
    pub(crate) fn open(
        target: SocketAddr,
        destination_port: u16,
        protect: impl FnOnce(i32) -> io::Result<()>,
        uid: i32,
    ) -> io::Result<Self> {
        let domain = match target {
            SocketAddr::V4(_) => Domain::IPV4,
            SocketAddr::V6(_) => Domain::IPV6,
        };
        let socket = Socket::new(domain, Type::DGRAM, Some(Protocol::UDP))?;
        protect(socket.as_raw_fd())?;
        if matches!(target, SocketAddr::V4(address) if address.ip().is_broadcast()) {
            let _ = socket.set_broadcast(true);
        }
        let socket: std::net::UdpSocket = socket.into();
        if let SocketAddr::V6(address) = target
            && address.ip().is_multicast()
        {
            let _ = socket.set_multicast_loop_v6(true);
            let _ = socket.join_multicast_v6(address.ip(), 0);
        }
        Ok(Self {
            socket,
            target,
            destination_port,
            receive_limit: match target {
                SocketAddr::V4(_) => 65_535 - 20 - 8,
                SocketAddr::V6(_) => 65_535 - 40 - 8,
            },
            uid,
            last_activity: Instant::now(),
            sent: 0,
            received: 0,
        })
    }

    pub(crate) fn accepts_reply_from(&self, source: SocketAddr) -> bool {
        match self.target {
            SocketAddr::V4(target) if target.ip().is_broadcast() => true,
            SocketAddr::V6(target) if target.ip().is_multicast() => true,
            target => source == target,
        }
    }

    fn usage(self) -> Usage {
        Usage {
            uid: self.uid,
            sent: self.sent,
            received: self.received,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum TcpState {
    Connecting,
    SocksHello,
    SocksAuthenticate,
    SocksConnect,
    SynReceived,
    Established,
    FinWait1,
    CloseWait,
    LastAck,
    Closing,
    Closed,
}

#[derive(Debug)]
pub(crate) struct TcpSession {
    pub(crate) socket: std::net::TcpStream,
    pub(crate) uid: i32,
    pub(crate) local_sequence: u32,
    pub(crate) remote_sequence: u32,
    pub(crate) mss: u16,
    pub(crate) receive_window: u32,
    pub(crate) receive_scale: u8,
    pub(crate) send_window: u32,
    pub(crate) send_scale: u8,
    pub(crate) acknowledged: u32,
    pub(crate) unconfirmed: u32,
    pub(crate) last_keep_alive: Option<Instant>,
    pub(crate) state: TcpState,
    pub(crate) last_activity: Instant,
    pub(crate) sent: u64,
    pub(crate) received: u64,
    pub(crate) socks5: Option<Socks5Session>,
    pub(crate) socks_pending: Vec<u8>,
    pub(crate) socks_pending_offset: usize,
    pub(crate) socks_response: Vec<u8>,
    pub(crate) dns_response: Vec<u8>,
    pub(crate) tls_client_hello: Vec<u8>,
    pub(crate) tls_sequence: Option<u32>,
    pub(crate) tls_inspected: bool,
    pub(crate) forward: BTreeMap<u32, TcpSegment>,
    pub(crate) pending_fin: Option<u32>,
    /// C closes the downstream fd as soon as it observes EOF, while retaining
    /// the TCP state long enough to complete the tunnel handshake.
    pub(crate) local_socket_closed: bool,
}

#[derive(Debug)]
pub(crate) struct TcpSegment {
    pub(crate) data: Vec<u8>,
    pub(crate) sent: usize,
    pub(crate) psh: bool,
}

#[derive(Debug, Clone)]
pub(crate) struct Socks5Session {
    pub(crate) username: String,
    pub(crate) password: String,
}

impl TcpSession {
    // These values are the TCP handshake state itself. Keeping them explicit
    // avoids a short-lived builder that would be used at only one call site.
    #[allow(clippy::too_many_arguments)]
    pub(crate) fn open(
        target: SocketAddr,
        protect: impl FnOnce(i32) -> io::Result<()>,
        uid: i32,
        local_sequence: u32,
        remote_sequence: u32,
        mss: u16,
        receive_scale: u8,
        send_window: u32,
        send_scale: u8,
        socks5: Option<Socks5Session>,
    ) -> io::Result<Self> {
        let domain = match target {
            SocketAddr::V4(_) => Domain::IPV4,
            SocketAddr::V6(_) => Domain::IPV6,
        };
        let socket = Socket::new(domain, Type::STREAM, Some(Protocol::TCP))?;
        protect(socket.as_raw_fd())?;
        // C logs TCP_NODELAY failures but still attempts the connection.
        let _ = socket.set_tcp_nodelay(true);
        socket.set_nonblocking(true)?;
        match socket.connect(&target.into()) {
            Ok(()) => {}
            Err(error) if error.raw_os_error() == Some(libc::EINPROGRESS) => {}
            Err(error) => return Err(error),
        }
        let mut session = Self {
            socket: socket.into(),
            uid,
            local_sequence,
            remote_sequence,
            mss,
            receive_window: 0,
            receive_scale,
            send_window,
            send_scale,
            acknowledged: 0,
            unconfirmed: 0,
            last_keep_alive: None,
            state: TcpState::Connecting,
            last_activity: Instant::now(),
            sent: 0,
            received: 0,
            socks5,
            socks_pending: Vec::new(),
            socks_pending_offset: 0,
            socks_response: Vec::new(),
            dns_response: Vec::new(),
            tls_client_hello: Vec::new(),
            tls_sequence: None,
            tls_inspected: false,
            forward: BTreeMap::new(),
            pending_fin: None,
            local_socket_closed: false,
        };
        session.refresh_receive_window();
        Ok(session)
    }

    fn timeout_seconds(&self) -> u64 {
        match self.state {
            TcpState::Established => 3600,
            TcpState::Connecting
            | TcpState::SocksHello
            | TcpState::SocksAuthenticate
            | TcpState::SocksConnect
            | TcpState::SynReceived
            | TcpState::FinWait1
            | TcpState::CloseWait
            | TcpState::LastAck
            | TcpState::Closing
            | TcpState::Closed => 20,
        }
    }
}

impl TcpSession {
    pub(crate) fn refresh_receive_window(&mut self) {
        let queued = self.forward.values().fold(0_u32, |total, segment| {
            total.saturating_add(
                u32::try_from(segment.data.len().saturating_sub(segment.sent)).unwrap_or(u32::MAX),
            )
        });
        let maximum = u32::from(u16::MAX) << self.receive_scale.min(14);
        self.receive_window = platform::tcp_send_buffer_available(self.socket.as_raw_fd())
            .min(maximum)
            .saturating_sub(queued);
    }

    pub(crate) fn advertised_receive_window(&self) -> u16 {
        u16::try_from(self.receive_window >> self.receive_scale.min(14)).unwrap_or(u16::MAX)
    }

    pub(crate) fn available_send_window(&self) -> u32 {
        let behind = self.local_sequence.wrapping_sub(self.acknowledged);
        let headers = self.unconfirmed.saturating_add(1).saturating_mul(40);
        self.send_window
            .saturating_sub(behind.saturating_add(headers))
    }

    /// Queue only the previously unseen intervals in a tunnel TCP segment.
    ///
    /// TCP retransmissions may partially overlap both queued and already
    /// forwarded data.  Keeping the first received byte for every sequence
    /// number prevents a later conflicting retransmission from rewriting an
    /// in-flight stream, while still accepting either non-overlapping tail.
    pub(crate) fn queue(&mut self, sequence: u32, payload: &[u8], psh: bool) {
        if payload.is_empty() {
            return;
        }
        // TCP serial arithmetic is only unambiguous within half the sequence
        // space.  The receive window is orders of magnitude smaller, so map
        // every live interval onto a monotonic offset from remote_sequence.
        let relative = |value: u32| u64::from(value.wrapping_sub(self.remote_sequence));
        let start = relative(sequence);
        if start >= (1_u64 << 31) || start >= MAX_REASSEMBLY_AHEAD {
            return;
        }
        let end = start
            .saturating_add(payload.len() as u64)
            .min(MAX_REASSEMBLY_AHEAD);
        if end <= start {
            return;
        }
        let mut existing: Vec<_> = self
            .forward
            .iter()
            .filter_map(|(segment_sequence, segment)| {
                let segment_start = relative(*segment_sequence);
                (segment_start < (1_u64 << 31)).then_some((
                    segment_start,
                    segment_start.saturating_add(segment.data.len() as u64),
                ))
            })
            .collect();
        existing.sort_unstable_by_key(|(segment_start, _)| *segment_start);
        let mut cursor = start;
        for (segment_start, segment_end) in existing {
            if segment_end <= cursor || segment_start >= end {
                continue;
            }
            if cursor < segment_start {
                self.insert_tcp_gap(
                    sequence,
                    start,
                    cursor,
                    segment_start.min(end),
                    payload,
                    psh,
                    end,
                );
            }
            cursor = cursor.max(segment_end);
            if cursor >= end {
                return;
            }
        }
        self.insert_tcp_gap(sequence, start, cursor, end, payload, psh, end);
    }

    #[allow(clippy::too_many_arguments)]
    fn insert_tcp_gap(
        &mut self,
        sequence: u32,
        input_start: u64,
        gap_start: u64,
        gap_end: u64,
        payload: &[u8],
        psh: bool,
        input_end: u64,
    ) {
        if gap_end <= gap_start {
            return;
        }
        let offset = usize::try_from(gap_start.saturating_sub(input_start)).unwrap_or(usize::MAX);
        let length = usize::try_from(gap_end.saturating_sub(gap_start)).unwrap_or(0);
        let Some(data) = payload.get(offset..offset.saturating_add(length)) else {
            return;
        };
        self.forward.insert(
            sequence.wrapping_add(
                u32::try_from(gap_start.saturating_sub(input_start)).unwrap_or(u32::MAX),
            ),
            TcpSegment {
                data: data.to_vec(),
                sent: 0,
                psh: psh && gap_end == input_end,
            },
        );
    }
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct Usage {
    pub(crate) uid: i32,
    pub(crate) sent: u64,
    pub(crate) received: u64,
}

#[cfg(test)]
mod tests {
    use std::{
        io,
        net::{IpAddr, Ipv4Addr, TcpListener, TcpStream, UdpSocket},
        time::{Duration, Instant},
    };

    use super::*;
    use crate::packet::{self, Version};

    #[test]
    fn finished_udp_keeps_a_closed_flow_and_reports_its_usage_once() {
        let flow = Flow {
            version: Version::V4,
            protocol: packet::UDP,
            source: IpAddr::V4(Ipv4Addr::new(10, 1, 10, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(8, 8, 8, 8)),
            source_port: 40000,
            destination_port: 53,
        };
        let mut sessions = SessionTable::new();
        sessions.insert(
            flow,
            Session::Udp(UdpSession {
                socket: UdpSocket::bind((Ipv4Addr::LOCALHOST, 0)).expect("UDP socket"),
                target: std::net::SocketAddr::from((Ipv4Addr::new(8, 8, 8, 8), 53)),
                destination_port: 53,
                receive_limit: 65_507,
                uid: 17,
                last_activity: Instant::now(),
                sent: 12,
                received: 34,
            }),
        );

        let usage = sessions.finish_udp(flow).expect("active UDP session");
        assert_eq!((usage.uid, usage.sent, usage.received), (17, 12, 34));
        assert_eq!(sessions.statistics().udp, 0);
        assert_eq!(sessions.active_len(), 0);
        assert!(matches!(
            sessions.get_mut(flow),
            Some(Session::ClosedUdp { .. })
        ));
        assert!(sessions.finish_udp(flow).is_none());
    }

    #[test]
    fn udp_socket_filters_unexpected_unicast_responders() {
        let target = UdpSocket::bind((Ipv4Addr::LOCALHOST, 0)).expect("target socket");
        target.set_nonblocking(true).expect("target nonblocking");
        let target_address = target.local_addr().expect("target address");
        let session =
            UdpSession::open(target_address, 53, |_| Ok(()), 7).expect("unconnected UDP session");

        session
            .socket
            .send_to(b"request", session.target)
            .expect("sendto target");
        let mut request = [0_u8; 16];
        let (length, source) = loop {
            match target.recv_from(&mut request) {
                Ok(value) => break value,
                Err(error) if error.kind() == io::ErrorKind::WouldBlock => continue,
                Err(error) => panic!("target receive failed: {error}"),
            }
        };
        assert_eq!(&request[..length], b"request");

        let alternate = UdpSocket::bind((Ipv4Addr::LOCALHOST, 0)).expect("alternate socket");
        alternate
            .send_to(b"response", source)
            .expect("alternate response");
        assert!(!session.accepts_reply_from(alternate.local_addr().expect("alternate address")));
        assert!(session.accepts_reply_from(target_address));
        assert_eq!(session.destination_port, 53);
        assert_eq!(session.receive_limit, 65_507);
    }

    #[test]
    fn timeout_scaling_preserves_c_integer_rounding_order() {
        // C calculates `100 - 1 * 100 / 409` as 100, not 99.
        assert_eq!(scaled_timeout(300, 1, 409), Duration::from_secs(300));
        assert_eq!(scaled_timeout(300, 409, 409), Duration::ZERO);
    }

    #[test]
    fn send_window_uses_the_full_tcp_sequence_space() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("TCP listener");
        let client = TcpStream::connect(listener.local_addr().expect("listener address"))
            .expect("TCP client");
        let _server = listener.accept().expect("TCP accept");
        let session = TcpSession {
            socket: client,
            uid: 0,
            local_sequence: 0x10,
            remote_sequence: 0,
            mss: 1460,
            receive_window: 0,
            receive_scale: 0,
            send_window: 100_000,
            send_scale: 0,
            acknowledged: 0xffff_fff0,
            unconfirmed: 0,
            last_keep_alive: None,
            state: TcpState::Established,
            last_activity: Instant::now(),
            sent: 0,
            received: 0,
            socks5: None,
            socks_pending: Vec::new(),
            socks_pending_offset: 0,
            socks_response: Vec::new(),
            dns_response: Vec::new(),
            tls_client_hello: Vec::new(),
            tls_sequence: None,
            tls_inspected: false,
            forward: BTreeMap::new(),
            pending_fin: None,
            local_socket_closed: false,
        };
        // The sequence wrapped by 32 bytes, plus one 40-byte header allowance.
        assert_eq!(session.available_send_window(), 99_928);
    }

    #[test]
    fn retransmit_cannot_rewrite_an_in_flight_segment() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("TCP listener");
        let target = listener.local_addr().expect("listener address");
        let mut session = TcpSession::open(target, |_| Ok(()), 0, 0, 100, 1460, 0, 0, 0, None)
            .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");
        session.queue(100, b"abc", false);
        session.forward.get_mut(&100).expect("queued segment").sent = 2;
        session.queue(100, b"abcdef", false);
        let segment = session.forward.get(&100).expect("updated segment");
        assert_eq!(segment.data, b"abc");
        assert_eq!(segment.sent, 2);
        assert_eq!(
            session
                .forward
                .get(&103)
                .map(|segment| segment.data.as_slice()),
            Some(b"def".as_slice())
        );
    }

    #[test]
    fn tcp_queue_keeps_first_bytes_across_out_of_order_overlaps() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("TCP listener");
        let target = listener.local_addr().expect("listener address");
        let mut session = TcpSession::open(target, |_| Ok(()), 0, 0, 100, 1460, 0, 0, 0, None)
            .expect("TCP session");
        let _server = listener.accept().expect("TCP accept");

        session.queue(108, b"ijkl", true);
        session.queue(100, b"abcdefghEFGH", true);
        session.queue(104, b"XXXXijklTAIL", true);

        assert_eq!(
            session
                .forward
                .get(&100)
                .map(|segment| segment.data.as_slice()),
            Some(b"abcdefgh".as_slice())
        );
        assert_eq!(
            session
                .forward
                .get(&108)
                .map(|segment| segment.data.as_slice()),
            Some(b"ijkl".as_slice())
        );
        assert_eq!(
            session
                .forward
                .get(&112)
                .map(|segment| segment.data.as_slice()),
            Some(b"TAIL".as_slice())
        );
    }

    #[test]
    fn timed_out_tcp_sends_reset_then_lingers_closed() {
        let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0)).expect("TCP listener");
        let client = TcpStream::connect(listener.local_addr().expect("listener address"))
            .expect("TCP client");
        let _server = listener.accept().expect("TCP accept");
        let flow = Flow {
            version: Version::V4,
            protocol: packet::TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 1, 10, 2)),
            destination: IpAddr::V4(Ipv4Addr::LOCALHOST),
            source_port: 40000,
            destination_port: 443,
        };
        let mut sessions = SessionTable::new();
        sessions.insert(
            flow,
            Session::Tcp(TcpSession {
                socket: client,
                uid: 17,
                local_sequence: 123,
                remote_sequence: 456,
                mss: 1460,
                receive_window: 0,
                receive_scale: 0,
                send_window: u16::MAX.into(),
                send_scale: 0,
                acknowledged: 0,
                unconfirmed: 0,
                last_keep_alive: None,
                state: TcpState::Established,
                last_activity: Instant::now() - Duration::from_secs(3_601),
                sent: 12,
                received: 34,
                socks5: None,
                socks_pending: Vec::new(),
                socks_pending_offset: 0,
                socks_response: Vec::new(),
                dns_response: Vec::new(),
                tls_client_hello: Vec::new(),
                tls_sequence: None,
                tls_inspected: false,
                forward: BTreeMap::new(),
                pending_fin: None,
                local_socket_closed: false,
            }),
        );

        let actions = sessions.advance_timeouts(409);
        assert_eq!(actions.tcp_resets, vec![(flow, 123, 456)]);
        assert!(matches!(
            sessions.get_mut(flow),
            Some(Session::Tcp(TcpSession {
                state: TcpState::Closing,
                ..
            }))
        ));
        let usage = sessions.finish_closing();
        assert_eq!(usage.len(), 1);
        assert_eq!((usage[0].1.sent, usage[0].1.received), (12, 34));
        assert!(matches!(
            sessions.get_mut(flow),
            Some(Session::Tcp(TcpSession {
                state: TcpState::Closed,
                ..
            }))
        ));
    }
}
