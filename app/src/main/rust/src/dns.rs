//! DNS wire-format parsing and response policy.  DNS names are parsed without
//! pointer casts and compression jumps are bounded, so hostile replies cannot
//! escape their packet buffer.

use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};

const HEADER_LEN: usize = 12;
const CLASS_IN: u16 = 1;
const TYPE_A: u16 = 1;
const TYPE_AAAA: u16 = 28;
const TYPE_SVCB: u16 = 64;
const TYPE_HTTPS: u16 = 65;
const MAX_LABELS: usize = 25;
const MAX_NAME_LEN: usize = 255;

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct ResolvedRecord {
    pub(crate) question: String,
    pub(crate) answer_name: String,
    pub(crate) resource: IpAddr,
    pub(crate) ttl: i32,
}

#[derive(Debug, Default, Clone, PartialEq, Eq)]
pub(crate) struct Response {
    pub(crate) question: Option<(String, u16)>,
    pub(crate) records: Vec<ResolvedRecord>,
    pub(crate) contains_service_binding: bool,
    /// C reports answers as it walks them, but abandons the policy rewrite if
    /// a later answer is malformed. Keep that distinction explicit.
    pub(crate) complete: bool,
}

/// Parses the response subset the original native engine reports to Kotlin.
/// Malformed records end parsing rather than producing partially trusted data.
pub(crate) fn parse_response(data: &[u8]) -> Option<Response> {
    if data.len() <= HEADER_LEN {
        return None;
    }
    let flags = be_u16(data, 2)?;
    let question_count = usize::from(be_u16(data, 4)?);
    let answer_count = usize::from(be_u16(data, 6)?);
    if flags & 0x8000 == 0 || (flags >> 11) & 0x0f != 0 || question_count == 0 || answer_count == 0
    {
        return None;
    }

    let (question_name, mut offset) = read_name(data, HEADER_LEN)?;
    let question_type = be_u16(data, offset)?;
    let _question_class = be_u16(data, offset + 2)?;
    offset += 4;
    let mut response = Response {
        question: Some((question_name.clone(), question_type)),
        complete: true,
        ..Response::default()
    };

    for _ in 0..answer_count {
        let Some((answer_name, name_end)) = read_name(data, offset) else {
            response.complete = false;
            break;
        };
        offset = name_end;
        let (Some(record_type), Some(class), Some(ttl), Some(length)) = (
            be_u16(data, offset),
            be_u16(data, offset + 2),
            be_u32(data, offset + 4),
            be_u16(data, offset + 8),
        ) else {
            response.complete = false;
            break;
        };
        // C passes the network u32 through a JNI `int`; retain that exact
        // two's-complement conversion so synthetic TLS/SNI records can use
        // its `-1` sentinel as well.
        let ttl = i32::try_from(ttl).unwrap_or(i32::MAX);
        let length = usize::from(length);
        let Some(rdata_end) = offset
            .checked_add(10)
            .and_then(|start| start.checked_add(length))
        else {
            response.complete = false;
            break;
        };
        offset += 10;
        let Some(rdata) = data.get(offset..rdata_end) else {
            response.complete = false;
            break;
        };
        offset += length;

        if class == CLASS_IN && matches!(record_type, TYPE_SVCB | TYPE_HTTPS) {
            response.contains_service_binding = true;
        }
        let resource = match record_type {
            TYPE_A if class == CLASS_IN && rdata.len() == 4 => {
                IpAddr::V4(Ipv4Addr::new(rdata[0], rdata[1], rdata[2], rdata[3]))
            }
            TYPE_AAAA if class == CLASS_IN && rdata.len() == 16 => {
                IpAddr::V6(Ipv6Addr::from(<[u8; 16]>::try_from(&rdata[..16]).ok()?))
            }
            _ => continue,
        };
        response.records.push(ResolvedRecord {
            question: question_name.clone(),
            answer_name,
            resource,
            ttl,
        });
    }
    Some(response)
}

/// Produces the intentionally minimal DNS response used for a blocked domain.
/// It preserves the header ID and first question, clearing all answer sections.
pub(crate) fn blocked_response(data: &[u8], rcode: u8) -> Option<Vec<u8>> {
    let response = parse_response(data)?;
    if !response.complete {
        return None;
    }
    let (question, _) = response.question?;
    let (_, question_end) = read_name(data, HEADER_LEN)?;
    let end = question_end.checked_add(4)?;
    let mut result = data.get(..end)?.to_vec();
    let flags = be_u16(data, 2)?;
    // QR with the configured RCODE; AA/TC/RD/RA/AD/CD are clear, as in C.
    let reply_flags = 0x8000 | (flags & 0x7800) | u16::from(rcode & 0x0f);
    result[2..4].copy_from_slice(&reply_flags.to_be_bytes());
    result[6..12].fill(0);
    if question.is_empty() {
        None
    } else {
        Some(result)
    }
}

fn read_name(data: &[u8], start: usize) -> Option<(String, usize)> {
    let mut cursor = start;
    let mut next = None;
    let mut labels = Vec::new();
    for _ in 0..=MAX_LABELS {
        let length = *data.get(cursor)?;
        if length == 0 {
            let end = next.unwrap_or(cursor + 1);
            let name = labels.join(".");
            return (!name.is_empty() && name.len() <= MAX_NAME_LEN).then_some((name, end));
        }
        if length & 0xc0 != 0 {
            let low = *data.get(cursor + 1)?;
            let target = usize::from((u16::from(length & 0x3f) << 8) | u16::from(low));
            if target >= data.len() {
                return None;
            }
            next.get_or_insert(cursor + 2);
            cursor = target;
            continue;
        }
        let label_len = usize::from(length);
        if label_len > 63 {
            return None;
        }
        let bytes = data.get(cursor + 1..cursor + 1 + label_len)?;
        let label = std::str::from_utf8(bytes).ok()?;
        labels.push(label.to_owned());
        if labels.iter().map(String::len).sum::<usize>() + labels.len().saturating_sub(1)
            > MAX_NAME_LEN
        {
            return None;
        }
        cursor += label_len + 1;
    }
    None
}

fn be_u16(data: &[u8], offset: usize) -> Option<u16> {
    Some(u16::from_be_bytes([
        *data.get(offset)?,
        *data.get(offset + 1)?,
    ]))
}

fn be_u32(data: &[u8], offset: usize) -> Option<u32> {
    Some(u32::from_be_bytes([
        *data.get(offset)?,
        *data.get(offset + 1)?,
        *data.get(offset + 2)?,
        *data.get(offset + 3)?,
    ]))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_compressed_a_answer() {
        let message = [
            0x12, 0x34, 0x81, 0x80, 0, 1, 0, 1, 0, 0, 0, 0, 3, b'w', b'w', b'w', 7, b'e', b'x',
            b'a', b'm', b'p', b'l', b'e', 3, b'c', b'o', b'm', 0, 0, 1, 0, 1, 0xc0, 0x0c, 0, 1, 0,
            1, 0, 0, 0, 60, 0, 4, 93, 184, 216, 34,
        ];
        let response = parse_response(&message).expect("valid DNS response");
        assert_eq!(response.records.len(), 1);
        assert_eq!(response.records[0].question, "www.example.com");
        assert_eq!(
            response.records[0].resource,
            "93.184.216.34".parse::<IpAddr>().unwrap()
        );
        let blocked = blocked_response(&message, 3).expect("blocked response");
        assert_eq!(blocked.len(), 33);
        assert_eq!(&blocked[6..12], &[0; 6]);
    }

    #[test]
    fn rejects_records_with_invalid_rdata_lengths() {
        let mut message = vec![
            0x12, 0x34, 0x81, 0x80, 0, 1, 0, 1, 0, 0, 0, 0, 3, b'w', b'w', b'w', 7, b'e', b'x',
            b'a', b'm', b'p', b'l', b'e', 3, b'c', b'o', b'm', 0, 0, 1, 0, 1, 0xc0, 0x0c, 0, 1, 0,
            1, 0, 0, 0, 60, 0, 5, 93, 184, 216, 34, 99,
        ];
        let response = parse_response(&message).expect("response with trailing A RDATA");
        assert!(response.complete);
        assert!(response.records.is_empty());

        message[6..8].copy_from_slice(&2_u16.to_be_bytes());
        message.push(0xc0);
        let response = parse_response(&message).expect("partially parsed response");
        assert!(response.records.is_empty());
        assert!(!response.complete);
        assert!(blocked_response(&message, 3).is_none());
    }
}
