//! Safe PCAP writer used by the optional packet capture setting.

use std::{
    fs::{File, OpenOptions},
    io::{self, Seek, SeekFrom, Write},
    path::Path,
};

const MIN_RECORD_SIZE: usize = 1;
const MAX_RECORD_SIZE: usize = crate::MTU;
const MIN_FILE_SIZE: u64 = 24;

#[derive(Debug)]
pub(crate) struct PcapWriter {
    file: Option<File>,
    record_size: usize,
    maximum_size: u64,
}

impl Default for PcapWriter {
    fn default() -> Self {
        Self {
            file: None,
            record_size: 64,
            maximum_size: 2 * 1024 * 1024,
        }
    }
}

impl PcapWriter {
    pub(crate) fn configure(
        &mut self,
        path: Option<&Path>,
        record_size: usize,
        maximum_size: u64,
    ) -> io::Result<()> {
        let record_size = record_size.clamp(MIN_RECORD_SIZE, MAX_RECORD_SIZE);
        let minimum_usable_file_size =
            MIN_FILE_SIZE + 16 + u64::try_from(record_size).unwrap_or(u64::MAX);
        let maximum_size = maximum_size.max(minimum_usable_file_size);
        if path.is_none() {
            let sync = self.file.take().map_or(Ok(()), |file| file.sync_all());
            self.record_size = record_size;
            self.maximum_size = maximum_size;
            return sync;
        }
        self.file = match path {
            None => None,
            Some(path) => {
                let mut file = OpenOptions::new()
                    .create(true)
                    .append(true)
                    .read(true)
                    .open(path)?;
                if file.metadata()?.len() == 0 {
                    file.write_all(&pcap_header(record_size))?;
                }
                Some(file)
            }
        };
        self.record_size = record_size;
        self.maximum_size = maximum_size;
        Ok(())
    }

    pub(crate) fn write(&mut self, packet: &[u8]) -> io::Result<()> {
        let Some(file) = self.file.as_mut() else {
            return Ok(());
        };
        let captured = packet.len().min(self.record_size);
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default();
        file.write_all(
            &u32::try_from(now.as_secs())
                .unwrap_or(u32::MAX)
                .to_le_bytes(),
        )?;
        file.write_all(&(now.subsec_micros()).to_le_bytes())?;
        file.write_all(&u32::try_from(captured).unwrap_or(u32::MAX).to_le_bytes())?;
        file.write_all(
            &u32::try_from(packet.len())
                .unwrap_or(u32::MAX)
                .to_le_bytes(),
        )?;
        file.write_all(&packet[..captured])?;
        if file.metadata()?.len() > self.maximum_size {
            file.set_len(24)?;
            file.seek(SeekFrom::Start(24))?;
        }
        Ok(())
    }
}

fn pcap_header(record_size: usize) -> [u8; 24] {
    let mut header = [
        0xd4, 0xc3, 0xb2, 0xa1, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 101, 0, 0, 0,
    ];
    header[16..20].copy_from_slice(&u32::try_from(record_size).unwrap_or(u32::MAX).to_le_bytes());
    header
}

#[cfg(test)]
mod tests {
    use std::{
        fs,
        time::{SystemTime, UNIX_EPOCH},
    };

    use super::*;

    #[test]
    fn clamps_capture_limits_to_safe_values() {
        let suffix = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("clock")
            .as_nanos();
        let path = std::env::temp_dir().join(format!("netguard-pcap-{suffix}.pcap"));
        let mut writer = PcapWriter::default();
        writer
            .configure(Some(&path), usize::MAX, 10_000)
            .expect("configure capture");
        writer.write(&[1, 2, 3]).expect("write record");
        let bytes = fs::read(&path).expect("pcap bytes");
        assert_eq!(
            u32::from_le_bytes(bytes[16..20].try_into().expect("snaplen")),
            crate::MTU as u32
        );
        drop(writer);
        fs::remove_file(path).expect("remove test capture");
    }

    #[test]
    fn minimum_file_limit_retains_a_valid_header() {
        let suffix = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("clock")
            .as_nanos();
        let path = std::env::temp_dir().join(format!("netguard-pcap-minimum-{suffix}.pcap"));
        let mut writer = PcapWriter::default();
        writer
            .configure(Some(&path), 64, 0)
            .expect("configure capture");
        writer.write(&[1, 2, 3]).expect("write record");
        assert_eq!(
            fs::metadata(&path).expect("pcap metadata").len(),
            24 + 16 + 3
        );
        drop(writer);
        fs::remove_file(path).expect("remove test capture");
    }
}
