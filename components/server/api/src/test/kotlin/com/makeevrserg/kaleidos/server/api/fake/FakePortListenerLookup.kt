package com.makeevrserg.kaleidos.server.api.fake

import com.makeevrserg.kaleidos.server.api.api.PortListenerLookup
import com.makeevrserg.kaleidos.server.api.model.PortListener

class FakePortListenerLookup : PortListenerLookup {
    val listenersByPort = mutableMapOf<Int, PortListener>()

    override suspend fun find(port: Int): PortListener? = listenersByPort[port]
}
