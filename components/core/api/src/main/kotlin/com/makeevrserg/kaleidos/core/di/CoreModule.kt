package com.makeevrserg.kaleidos.core.di

import com.makeevrserg.kaleidos.core.coroutine.api.CoroutineFeature
import com.makeevrserg.kaleidos.core.coroutine.api.PreviewDispatchers
import com.makeevrserg.kaleidos.core.coroutine.memory.BackgroundCoroutineFeature
import com.makeevrserg.kaleidos.core.coroutine.memory.MainCoroutineFeature
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
}
