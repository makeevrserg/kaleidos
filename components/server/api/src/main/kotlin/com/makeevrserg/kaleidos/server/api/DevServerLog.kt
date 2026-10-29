package com.makeevrserg.kaleidos.server.api

import com.makeevrserg.kaleidos.server.model.DevServerLogEvent
import kotlinx.coroutines.flow.SharedFlow

interface DevServerLog {
    val events: SharedFlow<DevServerLogEvent>

    fun record(event: DevServerLogEvent)
}
