@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.memory

import com.makeevrserg.kaleidos.server.model.DevServerLogEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDevServerLogTest {

    @Test
    fun GIVEN_log_at_capacity_WHEN_an_event_is_recorded_THEN_the_oldest_one_gives_way() {
        val log = DefaultDevServerLog(capacity = 2)

        log.record(DevServerLogEvent.Output("first"))
        log.record(DevServerLogEvent.Output("second"))
        log.record(DevServerLogEvent.Output("third"))

        assertEquals(
            listOf(DevServerLogEvent.Output("second"), DevServerLogEvent.Output("third")),
            log.events.replayCache
        )
    }
}
