package com.makeevrserg.kaleidos.core.coroutine.memory

import com.makeevrserg.kaleidos.core.coroutine.api.CoroutineFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Inherits the dispatcher of the service scope, which IntelliJ backs by `Dispatchers.Default`.
 * Use for PSI scanning and other work that must stay off the event dispatch thread.
 */
internal class BackgroundCoroutineFeature(
    parentScope: CoroutineScope
) : CoroutineFeature {
    override val coroutineContext: CoroutineContext =
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
}
