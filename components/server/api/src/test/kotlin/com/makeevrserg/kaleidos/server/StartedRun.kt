package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.channels.SendChannel

/** @param events fed by the test through [printOutput] and [exit] */
data class StartedRun(
    val config: DevServerLaunchConfig,
    val executionName: String,
    val events: SendChannel<GradleRunEvent>
)

/** Simulates console output of the run. */
fun StartedRun.printOutput(text: String) {
    events.trySend(GradleRunEvent.Output(text))
}

/** Simulates the Gradle task finishing; the run reports nothing afterwards. */
fun StartedRun.exit(isSuccess: Boolean) {
    events.trySend(GradleRunEvent.Exited(isSuccess))
    events.close()
}
