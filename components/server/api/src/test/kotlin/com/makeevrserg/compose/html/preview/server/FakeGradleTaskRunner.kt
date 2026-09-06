package com.makeevrserg.compose.html.preview.server

class FakeGradleTaskRunner : GradleTaskRunner {
    val startedRuns = mutableListOf<StartedRun>()

    val stoppedExecutionNames = mutableListOf<String>()

    /** Runs started with a dev server execution name, in start order. */
    val devServerRuns: List<StartedRun>
        get() = startedRuns.filter { run -> run.executionName != DevServerRunNames.STOP_TASK }

    override suspend fun start(
        config: DevServerLaunchConfig,
        executionName: String,
        listener: DevServerProcessListener
    ) {
        startedRuns += StartedRun(config, executionName, listener)
    }

    override suspend fun stop(executionName: String) {
        stoppedExecutionNames += executionName
    }
}
