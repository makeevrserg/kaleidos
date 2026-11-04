package com.makeevrserg.kaleidos.server.api.memory

import com.makeevrserg.kaleidos.server.api.api.DevServerLog
import com.makeevrserg.kaleidos.server.api.model.DevServerLogEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class DefaultDevServerLog(capacity: Int) : DevServerLog {
    private val mutableEvents = MutableSharedFlow<DevServerLogEvent>(
        replay = capacity,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val events: SharedFlow<DevServerLogEvent> = mutableEvents.asSharedFlow()

    override fun record(event: DevServerLogEvent) {
        mutableEvents.tryEmit(event)
    }
}
