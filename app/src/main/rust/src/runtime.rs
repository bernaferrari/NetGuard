//! Ownership and lifecycle state for one native VPN engine instance.

use std::{
    io,
    sync::{
        Mutex,
        atomic::{AtomicBool, AtomicI32},
    },
};

use crate::{engine::Engine, platform};

pub(crate) struct NativeContext {
    pub(crate) sdk: i32,
    pub(crate) stopping: AtomicBool,
    pub(crate) clear_requested: AtomicBool,
    pub(crate) statistics: [AtomicI32; 3],
    pub(crate) wake: platform::WakePipe,
    pub(crate) engine: Mutex<Engine>,
}

impl NativeContext {
    pub(crate) fn new(sdk: i32) -> io::Result<Self> {
        Ok(Self {
            sdk,
            stopping: AtomicBool::new(false),
            clear_requested: AtomicBool::new(false),
            statistics: std::array::from_fn(|_| AtomicI32::new(0)),
            wake: platform::WakePipe::new()?,
            engine: Mutex::new(Engine::new()),
        })
    }
}
