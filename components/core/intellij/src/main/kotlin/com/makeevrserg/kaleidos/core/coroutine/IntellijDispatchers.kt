package com.makeevrserg.kaleidos.core.coroutine

import com.intellij.openapi.application.EDT
import com.makeevrserg.kaleidos.core.coroutine.api.PreviewDispatchers
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

internal class IntellijDispatchers : PreviewDispatchers {
    override val main: CoroutineContext = Dispatchers.EDT

    override val io: CoroutineContext = Dispatchers.IO

    override val default: CoroutineContext = Dispatchers.Default
}
