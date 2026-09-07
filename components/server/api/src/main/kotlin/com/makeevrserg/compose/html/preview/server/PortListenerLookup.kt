package com.makeevrserg.compose.html.preview.server

/**
 * Which process listens on a TCP port of this machine. The JVM has no API for it, so the answer comes
 * from outside; a machine that cannot answer answers nothing, and the address is then reported without
 * a name, which is less helpful but never wrong.
 */
interface PortListenerLookup {
    /** Null when nothing listens on [port] and when this machine does not say who does. */
    suspend fun find(port: Int): PortListener?
}
