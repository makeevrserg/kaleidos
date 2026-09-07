package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.flow.Flow

/**
 * Runs Gradle tasks of the user's project. Runs are told apart by their execution name; only runs
 * carrying a name given here are ever stopped, so a dev server started from a terminal is left alone.
 */
interface GradleTaskRunner {
    /**
     * Cold: collecting hands the task to the platform and forwards the run; the flow completes after
     * [GradleRunEvent.Exited]. Cancelling the collector only stops watching, the run keeps going
     * until [stop].
     */
    fun run(config: DevServerLaunchConfig, executionName: String): Flow<GradleRunEvent>

    /** Terminates every run started with [executionName]. */
    suspend fun stop(executionName: String)
}
