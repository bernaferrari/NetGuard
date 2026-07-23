//! The deliberately tiny unsafe boundary for POSIX file descriptors and the
//! Android system-property API. Callers use ordinary Rust slices and results.

use std::{
    fs::File,
    io::{self, Read},
    os::fd::{FromRawFd, OwnedFd, RawFd},
};

#[cfg(target_os = "android")]
use std::ffi::{CStr, CString};

pub(crate) fn read(fd: RawFd, buffer: &mut [u8]) -> io::Result<usize> {
    // SAFETY: `buffer` is a live, writable Rust slice for the exact length
    // passed to libc; the descriptor is borrowed and never closed here.
    let result = unsafe { libc::read(fd, buffer.as_mut_ptr().cast(), buffer.len()) };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        usize::try_from(result).map_err(|_| io::Error::other("negative read length"))
    }
}

pub(crate) fn write(fd: RawFd, buffer: &[u8]) -> io::Result<usize> {
    // SAFETY: `buffer` is a live, readable Rust slice for the exact length
    // passed to libc; the descriptor is borrowed and never closed here.
    let result = unsafe { libc::write(fd, buffer.as_ptr().cast(), buffer.len()) };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        usize::try_from(result).map_err(|_| io::Error::other("negative write length"))
    }
}

pub(crate) fn send_tcp(fd: RawFd, buffer: &[u8], more: bool) -> io::Result<usize> {
    #[cfg(target_os = "android")]
    let more_flag = if more { libc::MSG_MORE } else { 0 };
    #[cfg(not(target_os = "android"))]
    let more_flag = {
        let _ = more;
        0
    };
    // SAFETY: `buffer` is a live readable slice and `fd` is a borrowed TCP
    // descriptor. MSG_NOSIGNAL/MSG_MORE match the original C send flags.
    let result = unsafe {
        libc::send(
            fd,
            buffer.as_ptr().cast(),
            buffer.len(),
            libc::MSG_NOSIGNAL | more_flag,
        )
    };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        usize::try_from(result).map_err(|_| io::Error::other("negative send length"))
    }
}

pub(crate) fn set_blocking(fd: RawFd) -> io::Result<()> {
    // SAFETY: `fcntl` only reads the status flags of the borrowed descriptor.
    let flags = unsafe { libc::fcntl(fd, libc::F_GETFL) };
    if flags < 0 {
        return Err(io::Error::last_os_error());
    }
    // SAFETY: this changes only the borrowed descriptor's nonblocking bit,
    // matching the original tunnel setup before its event loop begins.
    if unsafe { libc::fcntl(fd, libc::F_SETFL, flags & !libc::O_NONBLOCK) } < 0 {
        return Err(io::Error::last_os_error());
    }
    Ok(())
}

pub(crate) fn writable(fd: RawFd) -> io::Result<bool> {
    polled(fd, libc::POLLOUT, 0)
}

#[derive(Debug)]
pub(crate) struct WakePipe {
    read: OwnedFd,
    write: OwnedFd,
}

impl WakePipe {
    pub(crate) fn new() -> io::Result<Self> {
        let mut descriptors = [-1; 2];
        // SAFETY: `descriptors` is valid writable storage for the two file
        // descriptors filled by pipe. Ownership transfers after success.
        if unsafe { libc::pipe(descriptors.as_mut_ptr()) } < 0 {
            return Err(io::Error::last_os_error());
        }
        for descriptor in descriptors {
            // SAFETY: fcntl only changes flags of this freshly-created pipe.
            let flags = unsafe { libc::fcntl(descriptor, libc::F_GETFL) };
            if flags < 0
                // SAFETY: `flags` came from F_GETFL for this descriptor.
                || unsafe { libc::fcntl(descriptor, libc::F_SETFL, flags | libc::O_NONBLOCK) } < 0
            {
                // SAFETY: both descriptors were created by pipe and remain
                // uniquely owned on this error path.
                unsafe {
                    libc::close(descriptors[0]);
                    libc::close(descriptors[1]);
                }
                return Err(io::Error::last_os_error());
            }
        }
        // SAFETY: pipe succeeded and both raw descriptors are uniquely owned.
        let read = unsafe { OwnedFd::from_raw_fd(descriptors[0]) };
        // SAFETY: see above for the write descriptor.
        let write = unsafe { OwnedFd::from_raw_fd(descriptors[1]) };
        Ok(Self { read, write })
    }

    pub(crate) fn reader(&self) -> RawFd {
        std::os::fd::AsRawFd::as_raw_fd(&self.read)
    }

    pub(crate) fn wake(&self) {
        let _ = write(std::os::fd::AsRawFd::as_raw_fd(&self.write), b"w");
    }

    pub(crate) fn drain(&self) {
        drain_fd(self.reader());
    }
}

pub(crate) fn drain_fd(fd: RawFd) {
    let mut bytes = [0_u8; 64];
    loop {
        match read(fd, &mut bytes) {
            Ok(0) => break,
            Err(error) if error.kind() == io::ErrorKind::WouldBlock => break,
            Ok(_) | Err(_) => {}
        }
    }
}

pub(crate) fn readable_or_woken(tun: RawFd, wake: RawFd, timeout_ms: i32) -> io::Result<bool> {
    let mut descriptors = [
        libc::pollfd {
            fd: tun,
            events: libc::POLLIN,
            revents: 0,
        },
        libc::pollfd {
            fd: wake,
            events: libc::POLLIN,
            revents: 0,
        },
    ];
    // SAFETY: `descriptors` is initialized pollfd storage for two borrowed
    // descriptors and lives for the complete poll call.
    let descriptor_count = libc::nfds_t::try_from(descriptors.len())
        .map_err(|_| io::Error::other("too many poll descriptors"))?;
    let result = unsafe { libc::poll(descriptors.as_mut_ptr(), descriptor_count, timeout_ms) };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        Ok(descriptors[0].revents & libc::POLLIN != 0)
    }
}

/// Mirrors the original `is_readable` helper for bounded tunnel draining.
///
/// The tunnel is deliberately left in blocking mode, so callers must check
/// readiness immediately before each additional read.
pub(crate) fn readable(fd: RawFd) -> io::Result<bool> {
    polled(fd, libc::POLLIN, 0)
}

fn polled(fd: RawFd, events: libc::c_short, timeout_ms: i32) -> io::Result<bool> {
    let mut descriptor = libc::pollfd {
        fd,
        events,
        revents: 0,
    };
    // SAFETY: `descriptor` is a valid, initialized `pollfd` for one element.
    let result = unsafe { libc::poll(&raw mut descriptor, 1, timeout_ms) };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        Ok(result > 0 && descriptor.revents & events != 0)
    }
}

pub(crate) fn socket_error(fd: RawFd) -> io::Result<Option<i32>> {
    let mut value = 0_i32;
    let mut length = libc::socklen_t::try_from(std::mem::size_of::<i32>())
        .map_err(|_| io::Error::other("invalid socket option length"))?;
    // SAFETY: `value` and `length` are initialized writable buffers accepted
    // by `getsockopt`; the descriptor remains borrowed for this call.
    let result = unsafe {
        libc::getsockopt(
            fd,
            libc::SOL_SOCKET,
            libc::SO_ERROR,
            (&raw mut value).cast(),
            &raw mut length,
        )
    };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else if value == 0 {
        Ok(None)
    } else {
        Ok(Some(value))
    }
}

pub(crate) fn enable_tcp_keepalive(fd: RawFd) -> io::Result<()> {
    let enabled: libc::c_int = 1;
    // SAFETY: `fd` is a borrowed TCP socket and `enabled` is a valid option
    // value for the duration of setsockopt.
    let result = unsafe {
        libc::setsockopt(
            fd,
            libc::SOL_SOCKET,
            libc::SO_KEEPALIVE,
            (&raw const enabled).cast(),
            libc::socklen_t::try_from(std::mem::size_of_val(&enabled))
                .map_err(|_| io::Error::other("invalid keepalive option length"))?,
        )
    };
    if result < 0 {
        Err(io::Error::last_os_error())
    } else {
        Ok(())
    }
}

/// Return the amount of a TCP socket's kernel send queue that can still
/// accept payload.  This is the source for the advertised receive window in
/// the original proxy, not an application-level fixed window.
pub(crate) fn tcp_send_buffer_available(fd: RawFd) -> u32 {
    let mut send_buffer = 0_i32;
    let mut length = libc::socklen_t::try_from(std::mem::size_of::<i32>()).unwrap_or_default();
    // SAFETY: both output pointers are valid initialized writable storage and
    // `fd` remains borrowed for the duration of this socket query.
    let socket_result = unsafe {
        libc::getsockopt(
            fd,
            libc::SOL_SOCKET,
            libc::SO_SNDBUF,
            (&raw mut send_buffer).cast(),
            &raw mut length,
        )
    };
    if socket_result < 0 || send_buffer <= 0 {
        send_buffer = 163_840; // SEND_BUF_DEFAULT in the C implementation.
    }
    let mut unsent = 0_i32;
    // SAFETY: Linux/Android's TIOCOUTQ writes one `int` to the supplied valid
    // pointer and does not retain it. Failure intentionally falls back to a
    // zero queue just as the C code does after logging the ioctl error.
    let _ = unsafe { libc::ioctl(fd, libc::TIOCOUTQ, &raw mut unsent) };
    u32::try_from(send_buffer.saturating_sub(unsent.max(0))).unwrap_or(0)
}

pub(crate) fn fd_count() -> i32 {
    std::fs::read_dir("/proc/self/fd").map_or(0, |entries| {
        i32::try_from(entries.count()).unwrap_or(i32::MAX)
    })
}

pub(crate) fn fd_limit() -> i32 {
    let mut limit = std::mem::MaybeUninit::<libc::rlimit>::uninit();
    // SAFETY: the kernel initializes the `rlimit` structure on success.
    let result = unsafe { libc::getrlimit(libc::RLIMIT_NOFILE, limit.as_mut_ptr()) };
    if result < 0 {
        0
    } else {
        // SAFETY: guarded by the successful `getrlimit` result above.
        let current = unsafe { limit.assume_init().rlim_cur };
        i32::try_from(current).unwrap_or(i32::MAX)
    }
}

pub(crate) fn raise_fd_limit() {
    let mut limit = std::mem::MaybeUninit::<libc::rlimit>::uninit();
    // SAFETY: the kernel initializes `limit` on a successful call.
    if unsafe { libc::getrlimit(libc::RLIMIT_NOFILE, limit.as_mut_ptr()) } < 0 {
        return;
    }
    // SAFETY: initialized by the successful `getrlimit` above.
    let mut limit = unsafe { limit.assume_init() };
    limit.rlim_cur = limit.rlim_max;
    // SAFETY: `limit` is fully initialized and this only raises this process'
    // soft descriptor cap, the same scope as the original C initialization.
    let _ = unsafe { libc::setrlimit(libc::RLIMIT_NOFILE, &raw const limit) };
}

pub(crate) fn secure_random_u32() -> io::Result<u32> {
    let mut bytes = [0_u8; 4];
    File::open("/dev/urandom")?.read_exact(&mut bytes)?;
    Ok(u32::from_ne_bytes(bytes))
}

#[cfg(target_os = "android")]
pub(crate) fn system_property(name: &str) -> String {
    unsafe extern "C" {
        fn __system_property_get(
            name: *const libc::c_char,
            value: *mut libc::c_char,
        ) -> libc::c_int;
    }
    let Ok(name) = CString::new(name) else {
        return String::new();
    };
    // `c_char` is signed on x86/x86_64 and unsigned on some ARM targets.
    // Keeping the buffer in the FFI type avoids architecture-dependent casts.
    let mut value = [0 as libc::c_char; 92];
    // SAFETY: Android guarantees a 92-byte property buffer is sufficient;
    // both pointers are valid for the duration of this FFI call.
    unsafe {
        __system_property_get(name.as_ptr(), value.as_mut_ptr());
    }
    // SAFETY: `__system_property_get` writes a NUL-terminated C string.
    unsafe { CStr::from_ptr(value.as_ptr()) }
        .to_string_lossy()
        .into_owned()
}

#[cfg(not(target_os = "android"))]
pub(crate) fn system_property(name: &str) -> String {
    std::env::var(name).unwrap_or_default()
}

#[cfg(target_os = "android")]
pub(crate) fn log(message: &str) {
    unsafe extern "C" {
        fn __android_log_write(
            priority: libc::c_int,
            tag: *const libc::c_char,
            text: *const libc::c_char,
        ) -> libc::c_int;
    }
    let Ok(tag) = CString::new("QuietGuard.Rust") else {
        return;
    };
    let Ok(message) = CString::new(message) else {
        return;
    };
    // SAFETY: both C strings stay alive for the entire call.
    unsafe {
        __android_log_write(5, tag.as_ptr(), message.as_ptr());
    }
}

#[cfg(not(target_os = "android"))]
pub(crate) fn log(message: &str) {
    eprintln!("QuietGuard.Rust: {message}");
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn self_pipe_wakes_and_drains_without_waiting_for_tunnel_io() {
        let wake = WakePipe::new().expect("wake pipe");
        wake.wake();
        assert!(!readable_or_woken(-1, wake.reader(), 0).expect("poll wake pipe"));
        wake.drain();
        assert!(!readable_or_woken(-1, wake.reader(), 0).expect("drained wake pipe"));
    }
}
