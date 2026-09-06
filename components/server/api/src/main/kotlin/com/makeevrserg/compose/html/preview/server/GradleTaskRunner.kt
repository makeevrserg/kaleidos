package com.makeevrserg.compose.html.preview.server

/**
 * Runs Gradle tasks of the user's project. Runs are told apart by their execution name; only runs
 * carrying a name given here are ever stopped, so a dev server started from a terminal is left alone.
 */
interface GradleTaskRunner {
    suspend fun start(config: DevServerLaunchConfig, executionName: String, listener: DevServerProcessListener)

    /** Terminates every run started with [executionName]. */
    suspend fun stop(executionName: String)
}
