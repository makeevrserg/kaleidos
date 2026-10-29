package com.makeevrserg.kaleidos.server.fake

import com.makeevrserg.kaleidos.server.api.PortListenerLookup
import com.makeevrserg.kaleidos.server.model.PortListener

class FakePortListenerLookup : PortListenerLookup {
    val listenersByPort = mutableMapOf<Int, PortListener>()

    override suspend fun find(port: Int): PortListener? = listenersByPort[port]
}
