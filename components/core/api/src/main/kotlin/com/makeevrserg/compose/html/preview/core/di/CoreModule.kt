package com.makeevrserg.compose.html.preview.core.di

import com.makeevrserg.compose.html.preview.core.BackgroundCoroutineFeature
import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.core.MainCoroutineFeature
import com.makeevrserg.compose.html.preview.core.PreviewDispatchers
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

/**
 * Shared infrastructure of every component.
 *
 * @param coroutineScope scope of the project; the host cancels it when the project closes
 */
class CoreModule(
    private val coroutineScope: CoroutineScope,
    val dispatchers: PreviewDispatchers,
    val clock: Clock
) {
    val mainCoroutineFeature: CoroutineFeature = MainCoroutineFeature(coroutineScope, dispatchers.main)

    val backgroundCoroutineFeature: CoroutineFeature = BackgroundCoroutineFeature(coroutineScope)

    /** Child scope for UI that lives shorter than the project, for example the tool window content. */
    fun createMainCoroutineFeature(): CoroutineFeature = MainCoroutineFeature(coroutineScope, dispatchers.main)
}
