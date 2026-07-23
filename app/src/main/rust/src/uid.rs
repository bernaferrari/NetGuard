//! Android pre-Q connection-owner lookup through the kernel's proc tables.

use std::{
    fs,
    net::{IpAddr, Ipv4Addr, Ipv6Addr},
    sync::{Mutex, OnceLock},
    time::{Duration, Instant},
};

use crate::packet::{Flow, ICMP, ICMPV6, TCP, UDP, Version};

const CACHE_AGE: Duration = Duration::from_secs(30);

#[derive(Clone, Copy)]
struct CacheEntry {
    flow: Flow,
    uid: i32,
    observed: Instant,
}

static CACHE: OnceLock<Mutex<Vec<CacheEntry>>> = OnceLock::new();

fn cache() -> &'static Mutex<Vec<CacheEntry>> {
    CACHE.get_or_init(|| Mutex::new(Vec::new()))
}

pub(crate) fn clear_cache() {
    if let Ok(mut cache) = cache().lock() {
        cache.clear();
    }
}

pub(crate) fn lookup(flow: Flow) -> Option<i32> {
    // Android exposes many IPv4 sockets through the IPv6 table as mapped
    // addresses. C probes that table first, then falls back to IPv4.
    if flow.version == Version::V4
        && matches!(flow.protocol, TCP | UDP)
        && let Some(uid) = lookup_table(ipv4_mapped(flow))
    {
        return Some(uid);
    }
    lookup_table(flow)
}

fn ipv4_mapped(flow: Flow) -> Flow {
    let map = |address: IpAddr| match address {
        IpAddr::V4(address) => IpAddr::V6(address.to_ipv6_mapped()),
        address @ IpAddr::V6(_) => address,
    };
    Flow {
        version: Version::V6,
        source: map(flow.source),
        destination: map(flow.destination),
        ..flow
    }
}

fn lookup_table(flow: Flow) -> Option<i32> {
    let name = match (flow.version, flow.protocol) {
        (Version::V4, ICMP) => "icmp",
        (Version::V6, ICMPV6) => "icmp6",
        (Version::V4, TCP) => "tcp",
        (Version::V6, TCP) => "tcp6",
        (Version::V4, UDP) => "udp",
        (Version::V6, UDP) => "udp6",
        _ => return None,
    };
    let now = Instant::now();
    if let Ok(cache) = cache().lock()
        // C scans uid_cache from index zero upward, so an older matching
        // wildcard row wins over a later duplicate.
        && let Some(entry) = cache.iter().find(|entry| {
            now.duration_since(entry.observed) <= CACHE_AGE && matches(entry.flow, flow)
        })
    {
        return Some(entry.uid);
    }
    let contents = fs::read_to_string(format!("/proc/net/{name}")).ok()?;
    let mut found = None;
    let mut parsed = Vec::new();
    for line in contents.lines().skip(1) {
        if let Some((entry, uid)) = parse_line(line, flow.version, flow.protocol) {
            if matches(entry, flow) {
                found = Some(uid);
            }
            parsed.push(CacheEntry {
                flow: entry,
                uid,
                observed: now,
            });
        }
    }
    if let Ok(mut cache) = cache().lock() {
        cache.retain(|entry| now.duration_since(entry.observed) <= CACHE_AGE);
        cache.extend(parsed);
    }
    found
}

fn matches(entry: Flow, flow: Flow) -> bool {
    entry.protocol == flow.protocol
        && entry.source_port == flow.source_port
        && (entry.destination_port == flow.destination_port || entry.destination_port == 0)
        && (entry.source == flow.source || entry.source.is_unspecified())
        && (entry.destination == flow.destination || entry.destination.is_unspecified())
}

fn parse_line(line: &str, version: Version, protocol: u8) -> Option<(Flow, i32)> {
    let fields: Vec<_> = line.split_whitespace().collect();
    let (local, local_port) = parse_endpoint(fields.get(1)?, version)?;
    let (remote, remote_port) = parse_endpoint(fields.get(2)?, version)?;
    let uid = fields.get(7)?.parse::<i32>().ok()?;
    Some((
        Flow {
            version,
            protocol,
            source: local,
            destination: remote,
            source_port: local_port,
            destination_port: remote_port,
        },
        uid,
    ))
}

fn parse_endpoint(value: &str, version: Version) -> Option<(IpAddr, u16)> {
    let (address, port) = value.split_once(':')?;
    let port = u16::from_str_radix(port, 16).ok()?;
    let address = match version {
        Version::V4 if address.len() == 8 => {
            let value = u32::from_str_radix(address, 16).ok()?;
            IpAddr::V4(Ipv4Addr::from(value.to_le_bytes()))
        }
        Version::V6 if address.len() == 32 => {
            let mut octets = [0_u8; 16];
            for (index, output) in octets.chunks_exact_mut(4).enumerate() {
                let start = index * 8;
                let value = u32::from_str_radix(&address[start..start + 8], 16).ok()?;
                output.copy_from_slice(&value.to_le_bytes());
            }
            IpAddr::V6(Ipv6Addr::from(octets))
        }
        _ => return None,
    };
    Some((address, port))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_ipv4_proc_endpoints_in_kernel_byte_order() {
        let (address, port) = parse_endpoint("0100007F:1F90", Version::V4).expect("endpoint");
        assert_eq!(address, IpAddr::V4(Ipv4Addr::LOCALHOST));
        assert_eq!(port, 8080);
    }

    #[test]
    fn checks_ipv4_mapped_ipv6_rows_before_ipv4_rows() {
        let flow = Flow {
            version: Version::V4,
            protocol: TCP,
            source: IpAddr::V4(Ipv4Addr::new(10, 1, 10, 2)),
            destination: IpAddr::V4(Ipv4Addr::new(203, 0, 113, 7)),
            source_port: 4242,
            destination_port: 443,
        };
        let mapped = ipv4_mapped(flow);
        assert_eq!(mapped.version, Version::V6);
        assert_eq!(
            mapped.source,
            IpAddr::V6(Ipv4Addr::new(10, 1, 10, 2).to_ipv6_mapped())
        );
        assert!(matches(mapped, mapped));
        assert!(!matches(mapped, flow));
    }
}
