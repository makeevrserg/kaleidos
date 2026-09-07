package com.makeevrserg.kaleidos.core

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class IntellijDispatchers : PreviewDispatchers {
    override val main: CoroutineContext = Dispatchers.EDT

    override val io: CoroutineContext = Dispatchers.IO

    override val default: CoroutineContext = Dispatchers.Default
}
