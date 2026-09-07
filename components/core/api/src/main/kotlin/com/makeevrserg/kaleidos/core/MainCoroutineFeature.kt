package com.makeevrserg.kaleidos.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Runs on the UI thread of the host. Use for Swing and tool window manipulation.
 *
 * @param mainContext [PreviewDispatchers.main]
 */
class MainCoroutineFeature(
    parentScope: CoroutineScope,
    mainContext: CoroutineContext
) : CoroutineFeature {
    override val coroutineContext: CoroutineContext =
        parentScope.coroutineContext + mainContext + SupervisorJob(parentScope.coroutineContext[Job])
}
