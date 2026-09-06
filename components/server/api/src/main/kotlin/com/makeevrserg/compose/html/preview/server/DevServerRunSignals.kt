package com.makeevrserg.compose.html.preview.server

import kotlinx.coroutines.CompletableDeferred

/**
 * What a run reports asynchronously from platform threads.
 *
 * @param exit true when the Gradle task finished successfully; a continuous build only finishes when stopped
 */
class DevServerRunSignals(
    val announcedBaseUrl: CompletableDeferred<String>,
    val exit: CompletableDeferred<Boolean>
)
