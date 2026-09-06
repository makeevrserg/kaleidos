package com.makeevrserg.compose.html.preview.server

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key

/**
 * Attaches to the process of the run named [executionName] as soon as the platform reports it and
 * forwards its console output. [onAttached] lets the owner drop the execution subscription.
 */
class RunOutputForwarder(
    private val executionName: String,
    private val listener: DevServerProcessListener,
    private val onAttached: () -> Unit
) : ExecutionListener, ProcessListener {

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (env.runProfile.name != executionName) return
        handler.addProcessListener(this)
        onAttached()
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) = listener.onOutput(event.text)
}
