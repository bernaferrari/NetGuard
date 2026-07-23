package com.bernaferrari.quietguard.service

import com.bernaferrari.quietguard.*

import com.bernaferrari.quietguard.netguard.NativeEngine
import com.bernaferrari.quietguard.netguard.NativePcapConfig
import com.bernaferrari.quietguard.netguard.NativeSocks5Config

/** Owns one generated native engine and its single tunnel worker. */
internal class NativeVpnEngineController(
    sdk: Int,
    private val callbacks: NativeVpnCallbacks,
) : AutoCloseable {
    private val engine = NativeEngine(sdk)
    private val lock = Any()
    private var worker: Thread? = null

    fun configurePcap(config: NativePcapConfig) {
        engine.configurePcap(config)
    }

    fun configureSocks5(config: NativeSocks5Config?) {
        engine.configureSocks5(config)
    }

    fun stats() = engine.stats()

    fun start(tun: Int, forwardDns: Boolean, rcode: Int) {
        synchronized(lock) {
            check(worker == null) { "native VPN engine is already running" }
            engine.start()
            worker = Thread {
                try {
                    engine.run(tun, forwardDns, rcode, callbacks)
                } finally {
                    synchronized(lock) {
                        worker = null
                    }
                }
            }.also(Thread::start)
        }
    }

    fun stop() {
        val running = synchronized(lock) { worker } ?: return
        engine.stop()
        var interrupted = false
        while (running.isAlive) {
            try {
                running.join()
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
        engine.clear()
    }

    override fun close() {
        stop()
        engine.close()
    }
}
