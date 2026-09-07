package com.makeevrserg.kaleidos.core.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs [block] from enable to disable. Resources the block acquires are released by its own
 * cancellation handling, so a component needs no separate disposable for them.
 */
class CoroutineLifecycle(
    private val scope: CoroutineScope,
    private val block: suspend CoroutineScope.() -> Unit
) : Lifecycle {
    private val job = AtomicReference<Job?>(null)

    override fun onEnable() {
        job.getAndSet(scope.launch(block = block))?.cancel()
    }

    override fun onDisable() {
        job.getAndSet(null)?.cancel()
    }
}
