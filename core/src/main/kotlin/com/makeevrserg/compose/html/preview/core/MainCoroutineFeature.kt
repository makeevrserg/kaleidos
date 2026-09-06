package com.makeevrserg.compose.html.preview.core

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Runs on the IntelliJ event dispatch thread. Use for Swing and tool window manipulation.
 */
class MainCoroutineFeature(
    parentScope: CoroutineScope
) : CoroutineFeature {
    override val coroutineContext: CoroutineContext =
        parentScope.coroutineContext + Dispatchers.EDT + SupervisorJob(parentScope.coroutineContext[Job])
}
