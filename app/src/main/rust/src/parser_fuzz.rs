//! Property tests for packet-facing parsers.  These use arbitrary byte slices
//! as a compact in-tree fuzz corpus, so malformed tunnel input is exercised in
//! every normal `cargo test` run instead of only by a separate fuzzer job.

use proptest::prelude::*;

use crate::{dns, fragment::Reassembler, packet, tls};

proptest! {
    #![proptest_config(ProptestConfig::with_cases(256))]

    #[test]
    fn hostile_wire_bytes_never_panic(data in prop::collection::vec(any::<u8>(), 0..2048)) {
        if let Ok(packet) = packet::parse(&data) {
            let _ = packet::transport_checksum_valid(&data, &packet);
        }
        let _ = dns::parse_response(&data);
        let _ = tls::server_name(&data);
        let _ = tls::record_length_if_complete(&data);
        let mut fragments = Reassembler::default();
        let _ = fragments.push(&data);
    }
}
