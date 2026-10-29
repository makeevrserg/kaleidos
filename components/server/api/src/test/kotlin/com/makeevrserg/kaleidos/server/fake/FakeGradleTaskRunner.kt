package com.makeevrserg.kaleidos.server.fake

import com.makeevrserg.kaleidos.server.api.GradleTaskRunner
import com.makeevrserg.kaleidos.server.model.DevServerLaunchConfig
import com.makeevrserg.kaleidos.server.model.GradleRunEvent
import com.makeevrserg.kaleidos.server.model.StartedRun
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow

class FakeGradleTaskRunner : GradleTaskRunner {
    val startedRuns = mutableListOf<StartedRun>()

    val stoppedExecutionNames = mutableListOf<String>()

    /** A run is recorded when collected, like the real runner hands the task over on collection. */
    override fun run(config: DevServerLaunchConfig, executionName: String): Flow<GradleRunEvent> {
        return flow {
            val events = Channel<GradleRunEvent>(Channel.UNLIMITED)
            startedRuns += StartedRun(
                config,
                executionName,
                events
            )
            emit(GradleRunEvent.Started)
            emitAll(events.receiveAsFlow())
        }
    }

    override suspend fun stop(executionName: String) {
        stoppedExecutionNames += executionName
    }
}
