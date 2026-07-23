//! Minimal local DHCP responder used for Android tethering, matching the
//! original fixed `NetGuard` lease while keeping all offsets checked.

const BOOTP_LEN: usize = 236;
const MAGIC: [u8; 4] = [99, 130, 83, 99];
const RESPONSE_LEN: usize = 500;

pub(crate) fn response(request: &[u8]) -> Option<Vec<u8>> {
    if !is_recognized(request) {
        return None;
    }
    if request[0] != 1 {
        return None;
    }
    let mut reply = vec![0; RESPONSE_LEN];
    reply[..BOOTP_LEN + 4].copy_from_slice(&request[..BOOTP_LEN + 4]);
    // DHCPOFFER when `siaddr` is unset, DHCPACK otherwise.
    reply[0] = if request[20..24] == [0; 4] { 2 } else { 4 };
    reply[8..12].fill(0); // secs, flags
    reply[12..16].fill(0); // ciaddr
    reply[16..20].copy_from_slice(&[10, 1, 10, 2]); // yiaddr
    reply[20..24].copy_from_slice(&[10, 1, 10, 1]); // siaddr
    reply[24..28].fill(0); // giaddr

    let mut offset = BOOTP_LEN + 4;
    let mut option = |code: u8, value: &[u8]| {
        reply[offset] = code;
        let Ok(length) = u8::try_from(value.len()) else {
            return;
        };
        reply[offset + 1] = length;
        offset += 2;
        reply[offset..offset + value.len()].copy_from_slice(value);
        offset += value.len();
    };
    option(53, &[if request[20..24] == [0; 4] { 2 } else { 5 }]);
    option(1, &[255, 255, 255, 0]);
    option(3, &[10, 1, 10, 1]);
    option(51, &3600_u32.to_be_bytes());
    option(54, &[10, 1, 10, 1]);
    option(6, &[8, 8, 8, 8]);
    reply[offset] = 255;
    Some(reply)
}

/// C considers a well-formed DHCP message handled even when its opcode does
/// not need an Offer/Ack, so it must not be forwarded to the network.
pub(crate) fn is_recognized(request: &[u8]) -> bool {
    request.len() >= BOOTP_LEN + MAGIC.len()
        && request[0] == 1
        && request[BOOTP_LEN..BOOTP_LEN + 4] == MAGIC
        && request[1] == 1
        && request[2] == 6
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn creates_a_network_order_tethering_offer() {
        let mut request = vec![0; 300];
        request[0] = 1;
        request[1] = 1;
        request[2] = 6;
        request[BOOTP_LEN..BOOTP_LEN + 4].copy_from_slice(&MAGIC);
        let reply = response(&request).expect("valid DHCP discover");
        assert_eq!(reply.len(), RESPONSE_LEN);
        assert_eq!(reply[0], 2);
        assert_eq!(&reply[16..24], &[10, 1, 10, 2, 10, 1, 10, 1]);
        assert_eq!(&reply[240..243], &[53, 1, 2]);
        assert_eq!(&reply[255..261], &[51, 4, 0, 0, 0x0e, 0x10]);
    }

    #[test]
    fn does_not_consume_a_dhcp_reply_as_a_request() {
        let mut response = vec![0; 300];
        response[0] = 2;
        response[1] = 1;
        response[2] = 6;
        response[BOOTP_LEN..BOOTP_LEN + 4].copy_from_slice(&MAGIC);
        assert!(!is_recognized(&response));
    }
}
