package com.makeevrserg.compose.html.preview.server

class FakePortListenerLookup : PortListenerLookup {
    val listenersByPort = mutableMapOf<Int, PortListener>()

    override suspend fun find(port: Int): PortListener? = listenersByPort[port]
}
