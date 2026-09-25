package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DevServerLog(capacity: Int) {
    private val mutableEvents = MutableSharedFlow<DevServerLogEvent>(
        replay = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<DevServerLogEvent> = mutableEvents.asSharedFlow()

    fun record(event: DevServerLogEvent) {
        mutableEvents.tryEmit(event)
    }
}
