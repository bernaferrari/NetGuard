fn main() {
    if std::env::var_os("CARGO_CFG_TARGET_OS").as_deref() == Some(std::ffi::OsStr::new("android")) {
        println!("cargo:rustc-link-lib=log");
    }
}
