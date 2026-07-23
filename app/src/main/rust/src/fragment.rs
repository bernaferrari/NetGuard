//! Bounded IPv4/IPv6 fragment reassembly for tunnel packets.

use std::{
    collections::HashMap,
    net::{IpAddr, Ipv4Addr, Ipv6Addr},
    time::{Duration, Instant},
};

use crate::packet;

const MAX_DATAGRAM: usize = 65_535;
const MAX_ENTRIES: usize = 64;
const TIMEOUT: Duration = Duration::from_secs(30);

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
struct Key {
    version: u8,
    source: IpAddr,
    destination: IpAddr,
    identifier: u32,
    protocol: u8,
}

#[derive(Debug)]
struct Entry {
    prefix: Option<Vec<u8>>,
    parts: Vec<(usize, Vec<u8>)>,
    final_length: Option<usize>,
    last_seen: Instant,
}

type FragmentParts = (Key, Option<Vec<u8>>, usize, bool, Vec<u8>);

#[derive(Debug, Default)]
pub(crate) struct Reassembler {
    entries: HashMap<Key, Entry>,
}

impl Reassembler {
    pub(crate) fn push(&mut self, bytes: &[u8]) -> Option<Vec<u8>> {
        self.entries
            .retain(|_, entry| entry.last_seen.elapsed() < TIMEOUT);
        let (key, prefix, offset, more, payload) = fragment_parts(bytes)?;
        if self.entries.len() >= MAX_ENTRIES && !self.entries.contains_key(&key) {
            return None;
        }
        let entry = self.entries.entry(key).or_insert_with(|| Entry {
            prefix: None,
            parts: Vec::new(),
            final_length: None,
            last_seen: Instant::now(),
        });
        entry.last_seen = Instant::now();
        if offset == 0 {
            entry.prefix = Some(prefix?);
        }
        if !more {
            entry.final_length = offset.checked_add(payload.len());
        }
        if offset
            .checked_add(payload.len())
            .is_none_or(|end| end > MAX_DATAGRAM)
        {
            self.entries.remove(&key);
            return None;
        }
        if !entry
            .parts
            .iter()
            .any(|(start, existing)| *start == offset && existing == &payload)
        {
            entry.parts.push((offset, payload));
        }
        let prefix = entry.prefix.as_ref()?;
        let length = entry.final_length?;
        let mut assembled = vec![0_u8; length];
        let mut filled = vec![false; length];
        for (start, part) in &entry.parts {
            let end = start.checked_add(part.len())?;
            if end > length {
                self.entries.remove(&key);
                return None;
            }
            for (index, byte) in part.iter().enumerate() {
                let at = start + index;
                if filled[at] && assembled[at] != *byte {
                    self.entries.remove(&key);
                    return None;
                }
                assembled[at] = *byte;
                filled[at] = true;
            }
        }
        if filled.iter().any(|filled| !filled) {
            return None;
        }
        let result = match key.version {
            4 => build_ipv4(prefix, &assembled)?,
            6 => build_ipv6(prefix, &assembled)?,
            _ => return None,
        };
        self.entries.remove(&key);
        Some(result)
    }
}

fn fragment_parts(bytes: &[u8]) -> Option<FragmentParts> {
    match bytes.first()? >> 4 {
        4 => ipv4_parts(bytes),
        6 => ipv6_parts(bytes),
        _ => None,
    }
}

fn ipv4_parts(bytes: &[u8]) -> Option<FragmentParts> {
    let header_len = usize::from(bytes.first()? & 0x0f) * 4;
    let total_len = usize::from(u16::from_be_bytes([*bytes.get(2)?, *bytes.get(3)?]));
    if header_len < 20 || total_len != bytes.len() || total_len < header_len {
        return None;
    }
    let flags = u16::from_be_bytes([*bytes.get(6)?, *bytes.get(7)?]);
    let offset = usize::from(flags & 0x1fff) * 8;
    let key = Key {
        version: 4,
        source: IpAddr::V4(Ipv4Addr::new(
            *bytes.get(12)?,
            *bytes.get(13)?,
            *bytes.get(14)?,
            *bytes.get(15)?,
        )),
        destination: IpAddr::V4(Ipv4Addr::new(
            *bytes.get(16)?,
            *bytes.get(17)?,
            *bytes.get(18)?,
            *bytes.get(19)?,
        )),
        identifier: u32::from(u16::from_be_bytes([*bytes.get(4)?, *bytes.get(5)?])),
        protocol: *bytes.get(9)?,
    };
    Some((
        key,
        (offset == 0).then(|| bytes[..header_len].to_vec()),
        offset,
        flags & 0x2000 != 0,
        bytes[header_len..].to_vec(),
    ))
}

fn ipv6_parts(bytes: &[u8]) -> Option<FragmentParts> {
    let total_len = 40 + usize::from(u16::from_be_bytes([*bytes.get(4)?, *bytes.get(5)?]));
    if total_len != bytes.len() {
        return None;
    }
    let mut next = *bytes.get(6)?;
    let mut offset = 40;
    let mut previous_next = 6;
    loop {
        if next == 44 {
            if offset + 8 > total_len {
                return None;
            }
            let field = u16::from_be_bytes([bytes[offset + 2], bytes[offset + 3]]);
            let fragment_offset = usize::from(field >> 3) * 8;
            let key = Key {
                version: 6,
                source: IpAddr::V6(Ipv6Addr::from(
                    <[u8; 16]>::try_from(bytes.get(8..24)?).ok()?,
                )),
                destination: IpAddr::V6(Ipv6Addr::from(
                    <[u8; 16]>::try_from(bytes.get(24..40)?).ok()?,
                )),
                identifier: u32::from_be_bytes(bytes[offset + 4..offset + 8].try_into().ok()?),
                protocol: bytes[offset],
            };
            let prefix = (fragment_offset == 0).then(|| {
                let mut prefix = bytes[..offset].to_vec();
                prefix[previous_next] = bytes[offset];
                prefix
            });
            return Some((
                key,
                prefix,
                fragment_offset,
                field & 1 != 0,
                bytes[offset + 8..].to_vec(),
            ));
        }
        if !matches!(next, 0 | 43 | 60 | 51) || offset + 2 > total_len {
            return None;
        }
        let length = (usize::from(bytes[offset + 1]) + 1) * 8;
        if offset + length > total_len {
            return None;
        }
        previous_next = offset;
        next = bytes[offset];
        offset += length;
    }
}

fn build_ipv4(prefix: &[u8], payload: &[u8]) -> Option<Vec<u8>> {
    let total = prefix.len().checked_add(payload.len())?;
    let total_u16 = u16::try_from(total).ok()?;
    let mut result = prefix.to_vec();
    result[2..4].copy_from_slice(&total_u16.to_be_bytes());
    let flags = u16::from_be_bytes([result[6], result[7]]) & 0x4000;
    result[6..8].copy_from_slice(&flags.to_be_bytes());
    result[10..12].fill(0);
    let checksum = !packet::checksum(&result);
    result[10..12].copy_from_slice(&checksum.to_be_bytes());
    result.extend_from_slice(payload);
    Some(result)
}

fn build_ipv6(prefix: &[u8], payload: &[u8]) -> Option<Vec<u8>> {
    let payload_length = prefix.len().checked_sub(40)?.checked_add(payload.len())?;
    let mut result = prefix.to_vec();
    result[4..6].copy_from_slice(&u16::try_from(payload_length).ok()?.to_be_bytes());
    result.extend_from_slice(payload);
    Some(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn ipv4_fragment(payload: &[u8], offset: u16, more: bool) -> Vec<u8> {
        let mut packet = vec![0x45, 0, 0, 0, 0x12, 0x34, 0, 0, 64, packet::UDP, 0, 0];
        packet.extend_from_slice(&[10, 0, 0, 1, 8, 8, 8, 8]);
        let total = u16::try_from(packet.len() + payload.len()).expect("IPv4 length");
        packet[2..4].copy_from_slice(&total.to_be_bytes());
        let flags = offset | u16::from(more) << 13;
        packet[6..8].copy_from_slice(&flags.to_be_bytes());
        let checksum = !packet::checksum(&packet);
        packet[10..12].copy_from_slice(&checksum.to_be_bytes());
        packet.extend_from_slice(payload);
        packet
    }

    fn ipv6_fragment(payload: &[u8], offset: u16, more: bool) -> Vec<u8> {
        let mut packet = vec![0_u8; 48];
        packet[0] = 0x60;
        packet[4..6].copy_from_slice(
            &u16::try_from(8 + payload.len())
                .expect("IPv6 payload length")
                .to_be_bytes(),
        );
        packet[6] = 44;
        packet[7] = 64;
        packet[8..24].copy_from_slice(&[0x20, 1, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]);
        packet[24..40].copy_from_slice(&[0x20, 1, 0x0d, 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2]);
        packet[40] = packet::UDP;
        let field = (offset << 3) | u16::from(more);
        packet[42..44].copy_from_slice(&field.to_be_bytes());
        packet[44..48].copy_from_slice(&0x1234_5678_u32.to_be_bytes());
        packet.extend_from_slice(payload);
        packet
    }

    #[test]
    fn reassembles_out_of_order_ipv4_fragments() {
        let payload = [0, 42, 0, 53, 0, 16, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8];
        let first = ipv4_fragment(&payload[..8], 0, true);
        let second = ipv4_fragment(&payload[8..], 1, false);
        let mut reassembler = Reassembler::default();
        assert!(reassembler.push(&second).is_none());
        let reassembled = reassembler.push(&first).expect("reassembled packet");
        assert_eq!(&reassembled[20..], &payload);
        assert!(packet::ipv4_fixed_header_checksum_valid(&reassembled));
    }

    #[test]
    fn reassembles_out_of_order_ipv6_fragments() {
        let payload = [0, 42, 0, 53, 0, 16, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8];
        let first = ipv6_fragment(&payload[..8], 0, true);
        let second = ipv6_fragment(&payload[8..], 1, false);
        let mut reassembler = Reassembler::default();
        assert!(reassembler.push(&second).is_none());
        let reassembled = reassembler.push(&first).expect("reassembled packet");
        assert_eq!(reassembled[6], packet::UDP);
        assert_eq!(
            usize::from(u16::from_be_bytes([reassembled[4], reassembled[5]])),
            payload.len()
        );
        assert_eq!(&reassembled[40..], &payload);
    }

    #[test]
    fn rejects_conflicting_overlapping_fragments_without_poisoning_a_retry() {
        let payload = [0, 42, 0, 53, 0, 16, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8];
        let first = ipv4_fragment(&payload[..8], 0, true);
        let mut conflicting = payload[4..].to_vec();
        conflicting[0] ^= 0xff;
        let overlap = ipv4_fragment(&conflicting, 0, false);
        let final_fragment = ipv4_fragment(&payload[8..], 1, false);
        let mut reassembler = Reassembler::default();
        assert!(reassembler.push(&first).is_none());
        assert!(reassembler.push(&overlap).is_none());
        assert!(reassembler.push(&first).is_none());
        let reassembled = reassembler
            .push(&final_fragment)
            .expect("fresh retry completes");
        assert_eq!(&reassembled[20..], &payload);
    }
}
