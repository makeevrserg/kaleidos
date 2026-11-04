package com.makeevrserg.kaleidos.server.api.api

import com.makeevrserg.kaleidos.server.api.model.DevServerLogEvent
import kotlinx.coroutines.flow.SharedFlow

interface DevServerLog {
    val events: SharedFlow<DevServerLogEvent>

    fun record(event: DevServerLogEvent)
}
