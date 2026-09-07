package com.makeevrserg.kaleidos.harness

import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

/**
 * Ports nobody listens on right now, taken from a range of its own so preview servers stay recognisable
 * among the servers of the project. Falls back to a port chosen by the operating system when the whole
 * range is busy.
 */
class SocketFreePortAllocator(private val ports: IntRange) : FreePortAllocator {

    private val allocated = ConcurrentHashMap<String, Int>()

    private fun isFree(port: Int): Boolean = runCatching { ServerSocket(port).close() }.isSuccess

    private fun anyPort(): Int = ServerSocket(0).use { socket -> socket.localPort }

    override fun allocate(key: String): Int {
        return allocated.computeIfAbsent(key) { ports.firstOrNull(::isFree) ?: anyPort() }
    }
}
