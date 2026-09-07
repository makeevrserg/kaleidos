package com.makeevrserg.kaleidos.core.di

import com.makeevrserg.kaleidos.core.BackgroundCoroutineFeature
import com.makeevrserg.kaleidos.core.CoroutineFeature
import com.makeevrserg.kaleidos.core.MainCoroutineFeature
import com.makeevrserg.kaleidos.core.PreviewDispatchers
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
