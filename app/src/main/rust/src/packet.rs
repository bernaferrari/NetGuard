//! Checked parsing and serialisation of IPv4, IPv6, UDP, TCP and ICMP.

use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};

use crate::MTU;

pub(crate) const TCP: u8 = 6;
pub(crate) const UDP: u8 = 17;
pub(crate) const ICMP: u8 = 1;
pub(crate) const ICMPV6: u8 = 58;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) enum Version {
    V4,
    V6,
}

impl Version {
    pub(crate) const fn number(self) -> i32 {
        match self {
            Self::V4 => 4,
            Self::V6 => 6,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct Flow {
    pub(crate) version: Version,
    pub(crate) protocol: u8,
    pub(crate) source: IpAddr,
    pub(crate) destination: IpAddr,
    pub(crate) source_port: u16,
    pub(crate) destination_port: u16,
}

impl Flow {
    pub(crate) fn reverse(self) -> Self {
        Self {
            source: self.destination,
            destination: self.source,
            source_port: self.destination_port,
            destination_port: self.source_port,
            ..self
        }
    }
}

// A TCP header is a fixed collection of independent wire flags.
#[allow(clippy::struct_excessive_bools)]
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub(crate) struct TcpFlags {
    pub(crate) syn: bool,
    pub(crate) ack: bool,
    pub(crate) psh: bool,
    pub(crate) fin: bool,
    pub(crate) rst: bool,
    pub(crate) urg: bool,
}

impl TcpFlags {
    pub(crate) fn display(self) -> String {
        let mut flags = String::with_capacity(6);
        for (set, name) in [
            (self.syn, 'S'),
            (self.ack, 'A'),
            (self.psh, 'P'),
            (self.fin, 'F'),
            (self.rst, 'R'),
            (self.urg, 'U'),
        ] {
            if set {
                flags.push(name);
            }
        }
        flags
    }

    const fn bits(self) -> u8 {
        (self.fin as u8)
            | ((self.syn as u8) << 1)
            | ((self.rst as u8) << 2)
            | ((self.psh as u8) << 3)
            | ((self.ack as u8) << 4)
            | ((self.urg as u8) << 5)
    }
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct TcpHeader<'a> {
    pub(crate) sequence: u32,
    pub(crate) acknowledgement: u32,
    pub(crate) window: u16,
    pub(crate) flags: TcpFlags,
    pub(crate) payload: &'a [u8],
    pub(crate) mss: Option<u16>,
    pub(crate) window_scale: Option<u8>,
}

#[derive(Debug, Clone, Copy)]
pub(crate) enum Transport<'a> {
    Udp {
        payload: &'a [u8],
    },
    Tcp(TcpHeader<'a>),
    Icmp {
        kind: u8,
        code: u8,
        message: &'a [u8],
    },
    Other,
}

#[derive(Debug, Clone, Copy)]
pub(crate) struct Packet<'a> {
    pub(crate) flow: Flow,
    pub(crate) transport: Transport<'a>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ParseError {
    Empty,
    UnsupportedVersion(u8),
    Truncated,
    InvalidLength,
    Fragmented,
    InvalidHeader,
}

pub(crate) fn parse(bytes: &[u8]) -> Result<Packet<'_>, ParseError> {
    let Some(first) = bytes.first() else {
        return Err(ParseError::Empty);
    };
    match first >> 4 {
        4 => parse_ipv4(bytes),
        6 => parse_ipv6(bytes),
        version => Err(ParseError::UnsupportedVersion(version)),
    }
}

/// Validate the fixed IPv4 header using the Internet checksum's wire order.
pub(crate) fn ipv4_fixed_header_checksum_valid(bytes: &[u8]) -> bool {
    let Some(first) = bytes.first().copied() else {
        return false;
    };
    let header_length = usize::from(first & 0x0f) * 4;
    first >> 4 == 4
        && header_length >= 20
        && header_length <= bytes.len()
        && checksum(&bytes[..header_length]) == u16::MAX
}

fn parse_ipv4(bytes: &[u8]) -> Result<Packet<'_>, ParseError> {
    if bytes.len() < 20 {
        return Err(ParseError::Truncated);
    }
    let header_len = usize::from(bytes[0] & 0x0f) * 4;
    let total_len = usize::from(u16::from_be_bytes([bytes[2], bytes[3]]));
    if header_len < 20
        || header_len > bytes.len()
        || total_len < header_len
        || total_len != bytes.len()
    {
        return Err(ParseError::InvalidLength);
    }
    let fragments = u16::from_be_bytes([bytes[6], bytes[7]]);
    if fragments & 0x3fff != 0 {
        return Err(ParseError::Fragmented);
    }
    let protocol = bytes[9];
    let source = IpAddr::V4(Ipv4Addr::new(bytes[12], bytes[13], bytes[14], bytes[15]));
    let destination = IpAddr::V4(Ipv4Addr::new(bytes[16], bytes[17], bytes[18], bytes[19]));
    parse_transport(
        Version::V4,
        protocol,
        source,
        destination,
        &bytes[header_len..total_len],
    )
}

fn parse_ipv6(bytes: &[u8]) -> Result<Packet<'_>, ParseError> {
    if bytes.len() < 40 {
        return Err(ParseError::Truncated);
    }
    let total_len = 40 + usize::from(u16::from_be_bytes([bytes[4], bytes[5]]));
    if total_len != bytes.len() {
        return Err(ParseError::InvalidLength);
    }
    let source = IpAddr::V6(Ipv6Addr::from(
        <[u8; 16]>::try_from(&bytes[8..24]).map_err(|_| ParseError::Truncated)?,
    ));
    let destination = IpAddr::V6(Ipv6Addr::from(
        <[u8; 16]>::try_from(&bytes[24..40]).map_err(|_| ParseError::Truncated)?,
    ));
    let (protocol, offset) = ipv6_transport_offset(bytes, total_len)?;
    parse_transport(
        Version::V6,
        protocol,
        source,
        destination,
        &bytes[offset..total_len],
    )
}

fn ipv6_transport_offset(bytes: &[u8], total_len: usize) -> Result<(u8, usize), ParseError> {
    let mut next = bytes[6];
    let mut offset = 40;
    while matches!(next, 0 | 43 | 44 | 50 | 51 | 60 | 135) {
        if next == 44 {
            if offset + 8 > total_len {
                return Err(ParseError::Truncated);
            }
            let fragment = u16::from_be_bytes([bytes[offset + 2], bytes[offset + 3]]);
            if fragment != 0 {
                return Err(ParseError::Fragmented);
            }
            next = bytes[offset];
            offset += 8;
        } else if next == 50 {
            return Err(ParseError::InvalidHeader);
        } else {
            if offset + 2 > total_len {
                return Err(ParseError::Truncated);
            }
            let extension_len = (usize::from(bytes[offset + 1]) + 1) * 8;
            if offset + extension_len > total_len {
                return Err(ParseError::Truncated);
            }
            next = bytes[offset];
            offset += extension_len;
        }
    }
    Ok((next, offset))
}

fn parse_transport(
    version: Version,
    protocol: u8,
    source: IpAddr,
    destination: IpAddr,
    payload: &[u8],
) -> Result<Packet<'_>, ParseError> {
    let (source_port, destination_port, transport, _udp_length) = match protocol {
        UDP => {
            if payload.len() < 8 {
                return Err(ParseError::Truncated);
            }
            let length = usize::from(u16::from_be_bytes([payload[4], payload[5]]));
            if length < 8 || length > payload.len() {
                return Err(ParseError::InvalidLength);
            }
            (
                u16::from_be_bytes([payload[0], payload[1]]),
                u16::from_be_bytes([payload[2], payload[3]]),
                Transport::Udp {
                    payload: &payload[8..length],
                },
                Some(length),
            )
        }
        TCP => {
            if payload.len() < 20 {
                return Err(ParseError::Truncated);
            }
            let header_len = usize::from(payload[12] >> 4) * 4;
            if header_len < 20 || header_len > payload.len() {
                return Err(ParseError::InvalidLength);
            }
            let (mss, window_scale) = tcp_options(&payload[20..header_len])?;
            let flags = payload[13];
            (
                u16::from_be_bytes([payload[0], payload[1]]),
                u16::from_be_bytes([payload[2], payload[3]]),
                Transport::Tcp(TcpHeader {
                    sequence: u32::from_be_bytes(
                        payload[4..8]
                            .try_into()
                            .map_err(|_| ParseError::Truncated)?,
                    ),
                    acknowledgement: u32::from_be_bytes(
                        payload[8..12]
                            .try_into()
                            .map_err(|_| ParseError::Truncated)?,
                    ),
                    window: u16::from_be_bytes([payload[14], payload[15]]),
                    flags: TcpFlags {
                        syn: flags & 0x02 != 0,
                        ack: flags & 0x10 != 0,
                        psh: flags & 0x08 != 0,
                        fin: flags & 0x01 != 0,
                        rst: flags & 0x04 != 0,
                        urg: flags & 0x20 != 0,
                    },
                    payload: &payload[header_len..],
                    mss,
                    window_scale,
                }),
                None,
            )
        }
        ICMP | ICMPV6 => {
            // C requires ICMP_MINLEN and maps icmp_id to both Packet ports
            // before UID lookup and policy callbacks.
            if payload.len() < 8 {
                return Err(ParseError::Truncated);
            }
            let identifier = u16::from_be_bytes([payload[4], payload[5]]);
            (
                identifier,
                identifier,
                Transport::Icmp {
                    kind: payload[0],
                    code: payload[1],
                    message: payload,
                },
                None,
            )
        }
        _ => (0, 0, Transport::Other, None),
    };
    let flow = Flow {
        version,
        protocol,
        source,
        destination,
        source_port,
        destination_port,
    };
    Ok(Packet { flow, transport })
}

pub(crate) fn transport_checksum_valid(bytes: &[u8], packet: &Packet<'_>) -> bool {
    let segment = match packet.flow.version {
        Version::V4 => {
            let header_length = usize::from(bytes[0] & 0x0f) * 4;
            bytes.get(header_length..)
        }
        Version::V6 => {
            let total_length = 40 + usize::from(u16::from_be_bytes([bytes[4], bytes[5]]));
            ipv6_transport_offset(bytes, total_length)
                .ok()
                .and_then(|(_, offset)| bytes.get(offset..total_length))
        }
    };
    let Some(segment) = segment else {
        return false;
    };
    let segment = if packet.flow.protocol == UDP {
        let Some(length) = segment
            .get(4..6)
            .map(|value| usize::from(u16::from_be_bytes([value[0], value[1]])))
        else {
            return false;
        };
        let Some(segment) = segment.get(..length) else {
            return false;
        };
        segment
    } else {
        segment
    };
    match packet.flow.protocol {
        UDP => {
            if segment.len() < 8 {
                return false;
            }
            let checksum = u16::from_be_bytes([segment[6], segment[7]]);
            (packet.flow.version == Version::V4 && checksum == 0)
                || (checksum != 0 && transport_checksum(packet.flow, UDP, segment) == 0)
        }
        TCP => segment.len() >= 20 && transport_checksum(packet.flow, TCP, segment) == 0,
        ICMP if packet.flow.version == Version::V4 => checksum(segment) == u16::MAX,
        ICMPV6 if packet.flow.version == Version::V6 => {
            transport_checksum(packet.flow, ICMPV6, segment) == 0
        }
        _ => true,
    }
}

fn tcp_options(options: &[u8]) -> Result<(Option<u16>, Option<u8>), ParseError> {
    let mut cursor = 0;
    let mut mss = None;
    let mut window_scale = None;
    while cursor < options.len() {
        match options[cursor] {
            0 => break,
            1 => cursor += 1,
            kind => {
                let Some(&length) = options.get(cursor + 1) else {
                    return Err(ParseError::InvalidHeader);
                };
                let length = usize::from(length);
                if length < 2 || cursor + length > options.len() {
                    return Err(ParseError::InvalidHeader);
                }
                match (kind, length) {
                    (2, 4) => {
                        mss = Some(u16::from_be_bytes([
                            options[cursor + 2],
                            options[cursor + 3],
                        ]));
                    }
                    (3, 3) => window_scale = Some(options[cursor + 2].min(14)),
                    _ => {}
                }
                cursor += length;
            }
        }
    }
    Ok((mss, window_scale))
}

pub(crate) fn default_mss(version: Version) -> u16 {
    u16::try_from(MTU - if version == Version::V4 { 40 } else { 60 }).unwrap_or(u16::MAX)
}

pub(crate) fn udp_reply(flow: Flow, payload: &[u8]) -> Vec<u8> {
    udp_packet(flow.reverse(), payload)
}

pub(crate) fn udp_packet(flow: Flow, payload: &[u8]) -> Vec<u8> {
    let transport_len = 8 + payload.len();
    let mut result = ip_header(flow, transport_len, UDP);
    let start = result.len();
    result.extend_from_slice(&flow.source_port.to_be_bytes());
    result.extend_from_slice(&flow.destination_port.to_be_bytes());
    result.extend_from_slice(&u16_len(transport_len).to_be_bytes());
    result.extend_from_slice(&[0, 0]);
    result.extend_from_slice(payload);
    let checksum = transport_checksum(flow, UDP, &result[start..]);
    result[start + 6..start + 8].copy_from_slice(&checksum.to_be_bytes());
    result
}

pub(crate) fn tcp_reply(
    flow: Flow,
    sequence: u32,
    acknowledgement: u32,
    window: u16,
    flags: TcpFlags,
    payload: &[u8],
) -> Vec<u8> {
    tcp_reply_with_window_scale(flow, sequence, acknowledgement, window, flags, payload, 0)
}

pub(crate) fn tcp_reply_with_window_scale(
    flow: Flow,
    sequence: u32,
    acknowledgement: u32,
    window: u16,
    flags: TcpFlags,
    payload: &[u8],
    window_scale: u8,
) -> Vec<u8> {
    let flow = flow.reverse();
    let options = if flags.syn {
        let mss = default_mss(flow.version).to_be_bytes();
        vec![2, 4, mss[0], mss[1], 3, 3, window_scale.min(14), 0]
    } else {
        Vec::new()
    };
    let transport_len = 20 + options.len() + payload.len();
    let mut result = ip_header(flow, transport_len, TCP);
    let start = result.len();
    result.extend_from_slice(&flow.source_port.to_be_bytes());
    result.extend_from_slice(&flow.destination_port.to_be_bytes());
    result.extend_from_slice(&sequence.to_be_bytes());
    result.extend_from_slice(&acknowledgement.to_be_bytes());
    let header_words = u8::try_from(5 + options.len() / 4).unwrap_or(15);
    result.extend_from_slice(&[header_words << 4, flags.bits()]);
    result.extend_from_slice(&window.to_be_bytes());
    result.extend_from_slice(&[0, 0, 0, 0]);
    result.extend_from_slice(&options);
    result.extend_from_slice(payload);
    let checksum = transport_checksum(flow, TCP, &result[start..]);
    result[start + 16..start + 18].copy_from_slice(&checksum.to_be_bytes());
    result
}

pub(crate) fn icmp_reply(flow: Flow, message: &[u8]) -> Option<Vec<u8>> {
    if message.len() < 4 {
        return None;
    }
    let flow = flow.reverse();
    let protocol = if flow.version == Version::V4 {
        ICMP
    } else {
        ICMPV6
    };
    let mut result = ip_header(flow, message.len(), protocol);
    let start = result.len();
    result.extend_from_slice(message);
    result[start + 2..start + 4].fill(0);
    let checksum = if flow.version == Version::V4 {
        !checksum(&result[start..])
    } else {
        transport_checksum(flow, ICMPV6, &result[start..])
    };
    result[start + 2..start + 4].copy_from_slice(&checksum.to_be_bytes());
    Some(result)
}

pub(crate) fn icmp_message_checksum(flow: Flow, message: &[u8]) -> Option<u16> {
    if message.len() < 4 {
        return None;
    }
    let mut message = message.to_vec();
    message[2..4].fill(0);
    Some(if flow.version == Version::V4 {
        !checksum(&message)
    } else {
        transport_checksum(flow, ICMPV6, &message)
    })
}

fn ip_header(flow: Flow, transport_len: usize, protocol: u8) -> Vec<u8> {
    match (flow.version, flow.source, flow.destination) {
        (Version::V4, IpAddr::V4(source), IpAddr::V4(destination)) => {
            // C zero-initializes iphdr and never sets IP_DF.
            let mut header = vec![0x45, 0, 0, 0, 0, 0, 0, 0, 64, protocol, 0, 0];
            header[2..4].copy_from_slice(&u16_len(20 + transport_len).to_be_bytes());
            header.extend_from_slice(&source.octets());
            header.extend_from_slice(&destination.octets());
            let checksum = !checksum(&header);
            header[10..12].copy_from_slice(&checksum.to_be_bytes());
            header
        }
        (Version::V6, IpAddr::V6(source), IpAddr::V6(destination)) => {
            let mut header = vec![0x60, 0, 0, 0];
            header.extend_from_slice(&u16_len(transport_len).to_be_bytes());
            header.extend_from_slice(&[protocol, 64]);
            header.extend_from_slice(&source.octets());
            header.extend_from_slice(&destination.octets());
            header
        }
        _ => unreachable!("IP address family and packet version must agree"),
    }
}

fn transport_checksum(flow: Flow, protocol: u8, segment: &[u8]) -> u16 {
    let mut pseudo = Vec::with_capacity(40 + segment.len());
    match (flow.source, flow.destination) {
        (IpAddr::V4(source), IpAddr::V4(destination)) => {
            pseudo.extend_from_slice(&source.octets());
            pseudo.extend_from_slice(&destination.octets());
            pseudo.extend_from_slice(&[0, protocol]);
            pseudo.extend_from_slice(&u16_len(segment.len()).to_be_bytes());
        }
        (IpAddr::V6(source), IpAddr::V6(destination)) => {
            pseudo.extend_from_slice(&source.octets());
            pseudo.extend_from_slice(&destination.octets());
            pseudo.extend_from_slice(&u32_len(segment.len()).to_be_bytes());
            pseudo.extend_from_slice(&[0, 0, 0, protocol]);
        }
        _ => unreachable!("IP address family and packet version must agree"),
    }
    pseudo.extend_from_slice(segment);
    !checksum(&pseudo)
}

pub(crate) fn checksum(bytes: &[u8]) -> u16 {
    let mut sum = 0u32;
    for word in bytes.chunks(2) {
        sum += u32::from(if word.len() == 2 {
            u16::from_be_bytes([word[0], word[1]])
        } else {
            u16::from(word[0]) << 8
        });
    }
    while sum >> 16 != 0 {
        sum = (sum & 0xffff) + (sum >> 16);
    }
    u16::try_from(sum).unwrap_or(u16::MAX)
}

fn u16_len(length: usize) -> u16 {
    u16::try_from(length).unwrap_or(u16::MAX)
}

fn u32_len(length: usize) -> u32 {
    u32::try_from(length).unwrap_or(u32::MAX)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_ipv4_udp_without_casting_untrusted_bytes() {
        let bytes = [
            0x45, 0, 0, 28, 0, 0, 0, 0, 64, UDP, 0, 0, 10, 0, 0, 1, 8, 8, 8, 8, 0, 42, 0, 53, 0, 8,
            0, 0,
        ];
        let packet =
            parse(&bytes).unwrap_or_else(|error| panic!("unexpected parse error: {error:?}"));
        assert_eq!(packet.flow.source_port, 42);
        assert_eq!(
            packet.flow.destination,
            "8.8.8.8"
                .parse()
                .unwrap_or(IpAddr::V4(Ipv4Addr::UNSPECIFIED))
        );
    }

    #[test]
    fn respects_the_udp_datagram_length() {
        let bytes = [
            0x45, 0, 0, 32, 0, 0, 0, 0, 64, UDP, 0, 0, 10, 0, 0, 1, 8, 8, 8, 8, 0, 42, 0, 53, 0,
            10, 0, 0, 1, 2, 0xaa, 0xbb,
        ];
        let packet = parse(&bytes).expect("UDP packet");
        let Transport::Udp { payload } = packet.transport else {
            panic!("expected UDP transport");
        };
        assert_eq!(payload, &[1, 2]);

        let mut truncated = bytes;
        truncated[24..26].copy_from_slice(&13_u16.to_be_bytes());
        assert!(matches!(parse(&truncated), Err(ParseError::InvalidLength)));
    }

    #[test]
    fn rejects_trailing_or_incomplete_ipv6_fragments() {
        let mut trailing = vec![0x60, 0, 0, 0, 0, 8, UDP, 64];
        trailing.extend_from_slice(&Ipv6Addr::LOCALHOST.octets());
        trailing.extend_from_slice(&Ipv6Addr::LOCALHOST.octets());
        trailing.extend_from_slice(&[0, 42, 0, 53, 0, 8, 0, 0, 0]);
        assert!(matches!(parse(&trailing), Err(ParseError::InvalidLength)));

        let mut fragmented = vec![0x60, 0, 0, 0, 0, 16, 44, 64];
        fragmented.extend_from_slice(&Ipv6Addr::LOCALHOST.octets());
        fragmented.extend_from_slice(&Ipv6Addr::LOCALHOST.octets());
        fragmented.extend_from_slice(&[UDP, 0, 0, 1, 0, 0, 0, 0]);
        fragmented.extend_from_slice(&[0, 42, 0, 53, 0, 8, 0, 0]);
        assert!(matches!(parse(&fragmented), Err(ParseError::Fragmented)));
    }

    #[test]
    fn accepts_a_wire_valid_ipv4_checksum() {
        let mut packet = vec![0_u8; 28];
        packet[0] = 0x45;
        packet[2..4].copy_from_slice(&(28_u16).to_be_bytes());
        packet[8] = 64;
        packet[9] = UDP;
        packet[12..16].copy_from_slice(&[10, 0, 0, 1]);
        packet[16..20].copy_from_slice(&[10, 0, 0, 2]);
        let header_checksum = !checksum(&packet[..20]);
        packet[10..12].copy_from_slice(&header_checksum.to_be_bytes());
        assert!(ipv4_fixed_header_checksum_valid(&packet));
        packet[8] ^= 1;
        assert!(!ipv4_fixed_header_checksum_valid(&packet));
    }

    #[test]
    fn validates_ipv4_checksum_across_header_options() {
        let mut packet = vec![0_u8; 32];
        packet[0] = 0x46;
        packet[2..4].copy_from_slice(&(32_u16).to_be_bytes());
        packet[8] = 64;
        packet[9] = UDP;
        packet[12..16].copy_from_slice(&[10, 0, 0, 1]);
        packet[16..20].copy_from_slice(&[10, 0, 0, 2]);
        packet[20..24].copy_from_slice(&[1, 1, 1, 1]);
        let header_checksum = !checksum(&packet[..24]);
        packet[10..12].copy_from_slice(&header_checksum.to_be_bytes());
        assert!(ipv4_fixed_header_checksum_valid(&packet));
        packet[20] ^= 1;
        assert!(!ipv4_fixed_header_checksum_valid(&packet));
    }

    #[test]
    fn serialises_icmpv6_with_the_standard_source_destination_pseudo_header() {
        let flow = Flow {
            version: Version::V6,
            protocol: ICMPV6,
            source: "2001:db8::1"
                .parse()
                .unwrap_or(IpAddr::V6(Ipv6Addr::UNSPECIFIED)),
            destination: "2001:db8::2"
                .parse()
                .unwrap_or(IpAddr::V6(Ipv6Addr::UNSPECIFIED)),
            source_port: 42,
            destination_port: 42,
        };
        let reply = icmp_reply(flow, &[129, 0, 0, 0, 0, 42, 0, 1]).expect("ICMPv6 reply");
        let parsed = parse(&reply).expect("parsed reply");
        assert_eq!(transport_checksum(parsed.flow, ICMPV6, &reply[40..]), 0);
    }

    #[test]
    fn maps_icmp_identifier_to_flow_ports_like_c() {
        let bytes = [
            0x45, 0, 0, 28, 0, 0, 0, 0, 64, ICMP, 0, 0, 10, 0, 0, 1, 8, 8, 8, 8, 8, 0, 0, 0, 0x12,
            0x34, 0, 1,
        ];
        let packet = parse(&bytes).expect("ICMP packet");
        assert_eq!(
            (packet.flow.source_port, packet.flow.destination_port),
            (0x1234, 0x1234)
        );
    }

    #[test]
    fn serialised_udp_reply_is_parseable() {
        let flow = Flow {
            version: Version::V4,
            protocol: UDP,
            source: "10.0.0.1"
                .parse()
                .unwrap_or(IpAddr::V4(Ipv4Addr::UNSPECIFIED)),
            destination: "1.1.1.1"
                .parse()
                .unwrap_or(IpAddr::V4(Ipv4Addr::UNSPECIFIED)),
            source_port: 1234,
            destination_port: 53,
        };
        let reply = udp_reply(flow, &[1, 2, 3]);
        let parsed =
            parse(&reply).unwrap_or_else(|error| panic!("unexpected parse error: {error:?}"));
        assert_eq!(parsed.flow.source_port, 53);
        assert_eq!(&reply[6..8], &[0, 0]);
    }

    #[test]
    fn syn_ack_includes_mss_and_window_scale_options() {
        let flow = Flow {
            version: Version::V4,
            protocol: TCP,
            source: Ipv4Addr::new(10, 0, 0, 1).into(),
            destination: Ipv4Addr::new(1, 1, 1, 1).into(),
            source_port: 1234,
            destination_port: 443,
        };
        let reply = tcp_reply(
            flow,
            10,
            20,
            1024,
            TcpFlags {
                syn: true,
                ack: true,
                ..TcpFlags::default()
            },
            &[],
        );
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = parse(&reply).unwrap_or_else(|error| panic!("unexpected parse error: {error:?}"))
        else {
            panic!("expected TCP response");
        };
        assert_eq!(header.mss, Some(9960));
        assert_eq!(&reply[40..44], &[2, 4, 0x26, 0xe8]);
        assert_eq!(header.window_scale, Some(0));

        let scaled = tcp_reply_with_window_scale(
            flow,
            10,
            20,
            1024,
            TcpFlags {
                syn: true,
                ack: true,
                ..TcpFlags::default()
            },
            &[],
            7,
        );
        let Packet {
            transport: Transport::Tcp(header),
            ..
        } = parse(&scaled).expect("scaled SYN/ACK")
        else {
            panic!("expected TCP response");
        };
        assert_eq!(header.window_scale, Some(7));
    }
}
