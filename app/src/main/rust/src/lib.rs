//! `QuietGuard`'s native VPN engine.
//!
//! Packet parsing, session bookkeeping and packet construction are entirely
//! safe Rust. The small `platform` module is the only place that crosses the
//! Linux/JNI ABI boundary; keeping it isolated makes the safety contract
//! explicit and reviewable.

#![forbid(unsafe_op_in_unsafe_fn)]

mod dhcp;
mod dns;
mod engine;
mod fragment;
mod packet;
mod pcap;
mod platform;
mod runtime;
mod session;
mod tls;
mod uid;
mod uniffi_api;

#[cfg(test)]
mod parser_fuzz;

pub(crate) const MTU: usize = 10_000;

uniffi::setup_scaffolding!();
