//! Stable, generated Kotlin surface for the native VPN engine.

use std::{
    io,
    net::{IpAddr, SocketAddr},
    path::PathBuf,
    sync::{Arc, Mutex, atomic::Ordering},
    time::{SystemTime, UNIX_EPOCH},
};

use crate::{
    MTU, dns,
    engine::{Callbacks, PacketInfo, Redirect, Socks5Config},
    packet::Flow,
    pcap::PcapWriter,
    platform,
    runtime::NativeContext,
    session::Usage,
    uid,
};

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum NativeEngineError {
    #[error("native engine initialization failed: {0}")]
    Initialization(String),
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum NativeConfigurationError {
    #[error("invalid PCAP configuration: {0}")]
    InvalidPcap(String),
    #[error("invalid SOCKS5 configuration: {0}")]
    InvalidSocks5(String),
    #[error("native configuration failed: {0}")]
    Io(String),
}

#[derive(uniffi::Record)]
pub struct NativeCapabilities {
    pub mtu: u32,
    pub supports_ipv4: bool,
    pub supports_ipv6: bool,
}

#[derive(uniffi::Record)]
pub struct NativeStatistics {
    pub icmp_sessions: i32,
    pub udp_sessions: i32,
    pub tcp_sessions: i32,
    pub open_file_descriptors: i32,
    pub file_descriptor_limit: i32,
}

#[derive(uniffi::Record)]
pub struct NativePcapConfig {
    pub path: Option<String>,
    pub record_size: i32,
    pub maximum_size: i64,
}

#[derive(uniffi::Record)]
pub struct NativeSocks5Config {
    pub address: String,
    pub port: i32,
    pub username: String,
    pub password: String,
}

#[derive(uniffi::Record)]
pub struct NativeFlow {
    pub version: i32,
    pub protocol: i32,
    pub source: String,
    pub source_port: i32,
    pub destination: String,
    pub destination_port: i32,
}

#[derive(uniffi::Record)]
pub struct NativePacket {
    pub flow: NativeFlow,
    pub flags: String,
    pub data: String,
    pub uid: i32,
}

#[derive(uniffi::Record)]
pub struct NativeRedirect {
    pub address: String,
    pub port: i32,
    pub redirected: bool,
}

#[derive(uniffi::Record)]
pub struct NativeDnsRecord {
    pub question: String,
    pub answer_name: String,
    pub resource: String,
    pub ttl: i32,
    pub uid: i32,
    pub time_millis: i64,
}

#[derive(uniffi::Record)]
pub struct NativeUsage {
    pub flow: NativeFlow,
    pub uid: i32,
    pub sent: i64,
    pub received: i64,
    pub time_millis: i64,
}

#[uniffi::export(foreign)]
pub trait NativeCallbacks: Send + Sync {
    fn uid_for(&self, flow: NativeFlow) -> i32;
    fn allow(&self, packet: NativePacket) -> Option<NativeRedirect>;
    fn domain_blocked(&self, name: String) -> bool;
    fn log_dns_blocked(&self, packet: NativePacket);
    fn dns_resolved(&self, record: NativeDnsRecord);
    fn protect_socket(&self, fd: i32) -> bool;
    fn usage(&self, usage: NativeUsage);
    fn report_exit(&self, message: String);
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct PcapConfiguration {
    path: Option<PathBuf>,
    record_size: usize,
    maximum_size: u64,
}

impl Default for PcapConfiguration {
    fn default() -> Self {
        Self {
            path: None,
            record_size: 64,
            maximum_size: 2 * 1024 * 1024,
        }
    }
}

#[derive(uniffi::Object)]
pub struct NativeEngine {
    context: NativeContext,
    pcap: Mutex<PcapConfiguration>,
    socks5: Mutex<Option<Socks5Config>>,
}

#[uniffi::export]
impl NativeEngine {
    #[uniffi::constructor]
    pub fn new(sdk: i32) -> Result<Arc<Self>, NativeEngineError> {
        platform::raise_fd_limit();
        let context = NativeContext::new(sdk)
            .map_err(|error| NativeEngineError::Initialization(error.to_string()))?;
        Ok(Arc::new(Self {
            context,
            pcap: Mutex::new(PcapConfiguration::default()),
            socks5: Mutex::new(None),
        }))
    }

    pub fn start(&self) {
        self.context.stopping.store(false, Ordering::Release);
        self.context.clear_requested.store(false, Ordering::Release);
        self.context.wake.drain();
        for stat in &self.context.statistics {
            stat.store(0, Ordering::Release);
        }
    }

    pub fn run(
        &self,
        tun: i32,
        forward_dns: bool,
        rcode: i32,
        callbacks: Arc<dyn NativeCallbacks>,
    ) {
        let mut callbacks = UniFfiCallbacks {
            callbacks,
            sdk: self.context.sdk,
        };
        if let Err(error) = platform::set_blocking(tun) {
            platform::log(&format!("VPN tunnel blocking-mode setup failed: {error}"));
        }
        let mut engine = match self.context.engine.lock() {
            Ok(engine) => engine,
            Err(poisoned) => poisoned.into_inner(),
        };
        engine.configure_dns(forward_dns, rcode);
        let mut applied_pcap = None;
        let mut applied_socks5 = None;
        let mut sync_configuration = |engine: &mut crate::engine::Engine| {
            let pcap = self
                .pcap
                .lock()
                .map(|configuration| configuration.clone())
                .unwrap_or_default();
            if applied_pcap.as_ref() != Some(&pcap) {
                if let Err(error) =
                    engine.configure_pcap(pcap.path.as_deref(), pcap.record_size, pcap.maximum_size)
                {
                    platform::log(&format!("PCAP configuration failed: {error}"));
                }
                applied_pcap = Some(pcap);
            }
            let socks5 = self
                .socks5
                .lock()
                .map(|configuration| configuration.clone())
                .unwrap_or_default();
            if applied_socks5.as_ref() != Some(&socks5) {
                engine.configure_socks5(socks5.clone());
                applied_socks5 = Some(socks5);
            }
        };
        engine.run(
            tun,
            self.context.wake.reader(),
            &self.context.stopping,
            &self.context.clear_requested,
            &self.context.statistics,
            &mut callbacks,
            &mut sync_configuration,
        );
    }

    pub fn stop(&self) {
        self.context.stopping.store(true, Ordering::Release);
        self.context.wake.wake();
    }

    pub fn clear(&self) {
        self.context.clear_requested.store(true, Ordering::Release);
        self.context.wake.wake();
        if let Ok(mut engine) = self.context.engine.try_lock() {
            engine.clear();
            self.context.clear_requested.store(false, Ordering::Release);
            for stat in &self.context.statistics {
                stat.store(0, Ordering::Release);
            }
        }
    }

    pub fn stats(&self) -> NativeStatistics {
        NativeStatistics {
            icmp_sessions: self.context.statistics[0].load(Ordering::Acquire),
            udp_sessions: self.context.statistics[1].load(Ordering::Acquire),
            tcp_sessions: self.context.statistics[2].load(Ordering::Acquire),
            open_file_descriptors: platform::fd_count(),
            file_descriptor_limit: platform::fd_limit(),
        }
    }

    pub fn configure_pcap(&self, config: NativePcapConfig) -> Result<(), NativeConfigurationError> {
        let record_size = usize::try_from(config.record_size)
            .ok()
            .filter(|size| (1..=MTU).contains(size))
            .ok_or_else(|| {
                NativeConfigurationError::InvalidPcap("record size out of range".into())
            })?;
        let maximum_size = u64::try_from(config.maximum_size)
            .ok()
            .filter(|size| *size >= 24)
            .ok_or_else(|| {
                NativeConfigurationError::InvalidPcap("maximum size is too small".into())
            })?;
        let configuration = PcapConfiguration {
            path: config.path.map(PathBuf::from),
            record_size,
            maximum_size,
        };
        let mut writer = PcapWriter::default();
        writer
            .configure(
                configuration.path.as_deref(),
                configuration.record_size,
                configuration.maximum_size,
            )
            .map_err(|error| NativeConfigurationError::Io(error.to_string()))?;
        if let Ok(mut stored) = self.pcap.lock() {
            *stored = configuration;
        }
        Ok(())
    }

    pub fn configure_socks5(
        &self,
        config: Option<NativeSocks5Config>,
    ) -> Result<(), NativeConfigurationError> {
        let configured = config
            .map(|config| {
                let address = config.address.parse::<IpAddr>().map_err(|_| {
                    NativeConfigurationError::InvalidSocks5("address must be numeric".into())
                })?;
                let port = u16::try_from(config.port)
                    .ok()
                    .filter(|port| *port != 0)
                    .ok_or_else(|| {
                        NativeConfigurationError::InvalidSocks5("port must be 1..65535".into())
                    })?;
                Ok(Socks5Config {
                    endpoint: SocketAddr::new(address, port),
                    username: config.username,
                    password: config.password,
                })
            })
            .transpose()?;
        if let Ok(mut configuration) = self.socks5.lock() {
            *configuration = configured;
        }
        Ok(())
    }
}

impl Drop for NativeEngine {
    fn drop(&mut self) {
        self.context.stopping.store(true, Ordering::Release);
        self.context.wake.wake();
        uid::clear_cache();
    }
}

struct UniFfiCallbacks {
    callbacks: Arc<dyn NativeCallbacks>,
    sdk: i32,
}

impl Callbacks for UniFfiCallbacks {
    fn uid_for(&mut self, flow: Flow) -> i32 {
        if self.sdk <= 28 {
            return uid::lookup(flow).unwrap_or(-1);
        }
        self.callbacks.uid_for(native_flow(flow))
    }

    fn allow(&mut self, packet: &PacketInfo) -> Option<Redirect> {
        let redirect = self.callbacks.allow(native_packet(packet))?;
        let address = redirect.address.parse::<IpAddr>().ok()?;
        let port = u16::try_from(redirect.port).ok()?;
        Some(Redirect {
            address,
            port,
            redirected: redirect.redirected,
        })
    }

    fn domain_blocked(&mut self, name: &str) -> bool {
        self.callbacks.domain_blocked(name.to_owned())
    }

    fn log_dns_blocked(&mut self, packet: &PacketInfo) {
        self.callbacks.log_dns_blocked(native_packet(packet));
    }

    fn dns_resolved(&mut self, record: &dns::ResolvedRecord, uid: i32) {
        self.callbacks.dns_resolved(NativeDnsRecord {
            question: record.question.clone(),
            answer_name: record.answer_name.clone(),
            resource: record.resource.to_string(),
            ttl: record.ttl,
            uid,
            time_millis: unix_millis(),
        });
    }

    fn protect_socket(&mut self, fd: i32) -> io::Result<()> {
        if self.callbacks.protect_socket(fd) {
            Ok(())
        } else {
            Err(io::Error::other("VpnService refused to protect socket"))
        }
    }

    fn usage(&mut self, flow: Flow, usage: Usage) {
        self.callbacks.usage(NativeUsage {
            flow: native_flow(flow),
            uid: usage.uid,
            sent: i64::try_from(usage.sent).unwrap_or(i64::MAX),
            received: i64::try_from(usage.received).unwrap_or(i64::MAX),
            time_millis: unix_millis(),
        });
    }

    fn report_exit(&mut self, message: &str) {
        self.callbacks.report_exit(message.to_owned());
    }
}

fn native_flow(flow: Flow) -> NativeFlow {
    NativeFlow {
        version: flow.version.number(),
        protocol: i32::from(flow.protocol),
        source: flow.source.to_string(),
        source_port: i32::from(flow.source_port),
        destination: flow.destination.to_string(),
        destination_port: i32::from(flow.destination_port),
    }
}

fn native_packet(packet: &PacketInfo) -> NativePacket {
    NativePacket {
        flow: native_flow(packet.flow),
        flags: packet.flags.clone(),
        data: packet.data.clone(),
        uid: packet.uid,
    }
}

fn unix_millis() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis()
        .try_into()
        .unwrap_or(i64::MAX)
}

#[uniffi::export]
pub fn capabilities() -> NativeCapabilities {
    NativeCapabilities {
        mtu: u32::try_from(MTU).unwrap_or(u32::MAX),
        supports_ipv4: true,
        supports_ipv6: true,
    }
}

#[uniffi::export]
// UniFFI owns this `String`; borrowing would make the generated ABI invalid.
#[allow(clippy::needless_pass_by_value)]
pub fn system_property(name: String) -> String {
    platform::system_property(&name)
}

#[uniffi::export]
// UniFFI owns this `String`; borrowing would make the generated ABI invalid.
#[allow(clippy::needless_pass_by_value)]
pub fn is_numeric_address(value: String) -> bool {
    value.parse::<IpAddr>().is_ok()
}

#[cfg(test)]
mod tests {
    use std::sync::{
        Arc,
        atomic::{AtomicBool, Ordering},
    };

    use super::*;

    #[derive(Default)]
    struct RecordingCallbacks {
        protected: AtomicBool,
    }

    impl NativeCallbacks for RecordingCallbacks {
        fn uid_for(&self, _flow: NativeFlow) -> i32 {
            -1
        }
        fn allow(&self, _packet: NativePacket) -> Option<NativeRedirect> {
            None
        }
        fn domain_blocked(&self, _name: String) -> bool {
            false
        }
        fn log_dns_blocked(&self, _packet: NativePacket) {}
        fn dns_resolved(&self, _record: NativeDnsRecord) {}
        fn protect_socket(&self, _fd: i32) -> bool {
            self.protected.store(true, Ordering::Release);
            true
        }
        fn usage(&self, _usage: NativeUsage) {}
        fn report_exit(&self, _message: String) {}
    }

    #[test]
    fn protects_egress_sockets_on_modern_android() {
        let callbacks = Arc::new(RecordingCallbacks::default());
        let mut adapter = UniFfiCallbacks {
            callbacks: callbacks.clone(),
            sdk: 35,
        };
        Callbacks::protect_socket(&mut adapter, 42).expect("socket protected");
        assert!(callbacks.protected.load(Ordering::Acquire));
    }
}
