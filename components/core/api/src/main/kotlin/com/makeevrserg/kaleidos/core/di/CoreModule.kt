package com.makeevrserg.kaleidos.core.di

import com.makeevrserg.kaleidos.core.coroutine.api.CoroutineFeature
import com.makeevrserg.kaleidos.core.coroutine.memory.BackgroundCoroutineFeature
import com.makeevrserg.kaleidos.core.coroutine.memory.MainCoroutineFeature
import com.makeevrserg.kaleidos.core.coroutine.model.PreviewDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * Shared infrastructure of every component.
 *
 * @param coroutineScope scope of the project; the host cancels it when the project closes
 * @param mainContext UI thread of the host, the event dispatch thread in the IDE
 */
class CoreModule(
    private val coroutineScope: CoroutineScope,
    mainContext: CoroutineContext,
    val clock: Clock
) {
    val dispatchers = PreviewDispatchers(
        main = mainContext,
        io = Dispatchers.IO,
        default = Dispatchers.Default
    )

    val mainCoroutineFeature: CoroutineFeature = MainCoroutineFeature(coroutineScope, dispatchers.main)

    val backgroundCoroutineFeature: CoroutineFeature = BackgroundCoroutineFeature(coroutineScope)
}
