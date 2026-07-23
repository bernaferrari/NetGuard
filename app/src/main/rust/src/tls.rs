//! TLS `ClientHello` SNI extraction with complete record-length validation.

const HANDSHAKE_RECORD: u8 = 22;
const CLIENT_HELLO: u8 = 1;
const SERVER_NAME_EXTENSION: u16 = 0;
// The C buffer is TLS_SNI_LENGTH bytes and rejects lengths >= 255.
const MAX_SERVER_NAME: usize = 254;

pub(crate) fn server_name(data: &[u8]) -> Option<String> {
    let (handshake, _) = client_hello_if_complete(data)?;
    if handshake.first() != Some(&CLIENT_HELLO) {
        return None;
    }
    // Handshake type/length, client version, random.
    let mut index = 4 + 2 + 32;
    let session_length = usize::from(*handshake.get(index)?);
    index = index.checked_add(1 + session_length)?;
    let suites_length = usize::from(be_u16(&handshake, index)?);
    index = index.checked_add(2 + suites_length)?;
    let compression_length = usize::from(*handshake.get(index)?);
    index = index.checked_add(1 + compression_length)?;
    let extension_list_length = usize::from(be_u16(&handshake, index)?);
    index = index.checked_add(2)?;
    let extension_list_end = index.checked_add(extension_list_length)?;
    if extension_list_length == 0 || extension_list_end != handshake.len() {
        return None;
    }

    while index < extension_list_end {
        let extension_type = be_u16(&handshake, index)?;
        let extension_length = usize::from(be_u16(&handshake, index + 2)?);
        index = index.checked_add(4)?;
        let extension_end = index.checked_add(extension_length)?;
        if extension_end > extension_list_end {
            return None;
        }
        if extension_type == SERVER_NAME_EXTENSION {
            let list_length = usize::from(be_u16(&handshake, index)?);
            let mut name_index = index.checked_add(2)?;
            let list_end = name_index.checked_add(list_length)?;
            if list_end != extension_end {
                return None;
            }
            while name_index < list_end {
                let name_type = *handshake.get(name_index)?;
                name_index += 1;
                let name_length = usize::from(be_u16(&handshake, name_index)?);
                name_index += 2;
                let name_end = name_index.checked_add(name_length)?;
                if name_end > list_end {
                    return None;
                }
                if name_type == 0 {
                    let name = handshake.get(name_index..name_end)?;
                    if name_length == 0 || name_length > MAX_SERVER_NAME {
                        return None;
                    }
                    return std::str::from_utf8(name).ok().map(str::to_owned);
                }
                name_index = name_end;
            }
            return None;
        }
        index = extension_end;
    }
    None
}

pub(crate) fn record_length_if_complete(data: &[u8]) -> Option<usize> {
    client_hello_if_complete(data).map(|(_, length)| length)
}

fn client_hello_if_complete(data: &[u8]) -> Option<(Vec<u8>, usize)> {
    let mut consumed_records = 0_usize;
    let mut handshake = Vec::new();
    loop {
        let header = data.get(consumed_records..consumed_records.checked_add(5)?)?;
        if header[0] != HANDSHAKE_RECORD || header[1] < 3 {
            return None;
        }
        let next_record = consumed_records.checked_add(record_length(header)?)?;
        let record = data.get(consumed_records + 5..next_record)?;
        handshake.extend_from_slice(record);
        consumed_records = next_record;
        if handshake.len() < 4 {
            continue;
        }
        let length = (usize::from(handshake[1]) << 16)
            | (usize::from(handshake[2]) << 8)
            | usize::from(handshake[3]);
        let handshake_end = 4usize.checked_add(length)?;
        if handshake.len() >= handshake_end {
            handshake.truncate(handshake_end);
            return Some((handshake, consumed_records));
        }
    }
}

fn record_length(data: &[u8]) -> Option<usize> {
    Some(5 + usize::from(be_u16(data, 3)?))
}

fn be_u16(data: &[u8], offset: usize) -> Option<u16> {
    Some(u16::from_be_bytes([
        *data.get(offset)?,
        *data.get(offset + 1)?,
    ]))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reads_a_complete_client_hello_server_name() {
        let host = b"example.com";
        let extension_length = 2 + 1 + 2 + host.len();
        let extensions_length = 4 + extension_length;
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
        assert_eq!(server_name(&hello).as_deref(), Some("example.com"));
        hello[4] = 0;
        assert_eq!(server_name(&hello), None);
    }

    #[test]
    fn reads_a_client_hello_split_across_tls_records() {
        let host = b"split.example";
        let extension_length = 2 + 1 + 2 + host.len();
        let extensions_length = 4 + extension_length;
        let handshake_length = 2 + 32 + 1 + 2 + 2 + 1 + 1 + 2 + extensions_length;
        let mut handshake = vec![1];
        handshake.extend_from_slice(&(handshake_length as u32).to_be_bytes()[1..]);
        handshake.extend_from_slice(&[3, 3]);
        handshake.extend_from_slice(&[0; 32]);
        handshake.push(0);
        handshake.extend_from_slice(&2_u16.to_be_bytes());
        handshake.extend_from_slice(&[0x13, 1]);
        handshake.extend_from_slice(&[1, 0]);
        handshake.extend_from_slice(&(extensions_length as u16).to_be_bytes());
        handshake.extend_from_slice(&0_u16.to_be_bytes());
        handshake.extend_from_slice(&(extension_length as u16).to_be_bytes());
        handshake.extend_from_slice(&(1 + 2 + host.len() as u16).to_be_bytes());
        handshake.push(0);
        handshake.extend_from_slice(&(host.len() as u16).to_be_bytes());
        handshake.extend_from_slice(host);

        let split = 11;
        let mut records = Vec::new();
        for part in [&handshake[..split], &handshake[split..]] {
            records.extend_from_slice(&[HANDSHAKE_RECORD, 3, 3]);
            records.extend_from_slice(&(part.len() as u16).to_be_bytes());
            records.extend_from_slice(part);
        }
        assert_eq!(server_name(&records).as_deref(), Some("split.example"));
        assert_eq!(record_length_if_complete(&records), Some(records.len()));
    }
}
