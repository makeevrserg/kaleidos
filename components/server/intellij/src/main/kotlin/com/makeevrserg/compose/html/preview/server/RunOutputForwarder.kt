package com.makeevrserg.compose.html.preview.server

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Attaches to the process of the run named [executionName] as soon as the platform reports it and
 * forwards its console output until [detach].
 *
 * The run is marked to be destroyed silently when the project closes: the plugin owns it, so the
 * platform must not ask the user whether to terminate it.
 */
class RunOutputForwarder(
    private val executionName: String,
    private val onOutput: (text: String) -> Unit
) : ExecutionListener, ProcessListener {
    private val attachedHandler = AtomicReference<ProcessHandler?>(null)

    private val isDetached = AtomicBoolean(false)

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (env.runProfile.name != executionName) return
        handler.putUserData(ProcessHandler.SILENTLY_DESTROY_ON_CLOSE, true)
        handler.addProcessListener(this)
        attachedHandler.set(handler)
        // The process may start on another thread while the owner detaches; never leave the listener behind
        if (isDetached.get()) detach()
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) = onOutput(event.text)

    /** Safe when the process never started or when called more than once. */
    fun detach() {
        isDetached.set(true)
        attachedHandler.getAndSet(null)?.removeProcessListener(this)
    }
}
