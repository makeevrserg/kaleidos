package com.makeevrserg.kaleidos.server.util

import com.makeevrserg.kaleidos.server.model.GradleRunEvent
import com.makeevrserg.kaleidos.server.model.StartedRun

/** Simulates console output of the run. */
fun StartedRun.printOutput(text: String) {
    events.trySend(GradleRunEvent.Output(text))
}

/** Simulates the Gradle task finishing; the run reports nothing afterwards. */
fun StartedRun.exit(isSuccess: Boolean) {
    events.trySend(GradleRunEvent.Exited(isSuccess))
    events.close()
}
