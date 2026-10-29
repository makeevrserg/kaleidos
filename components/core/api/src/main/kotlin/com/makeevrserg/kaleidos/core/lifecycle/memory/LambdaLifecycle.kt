package com.makeevrserg.kaleidos.core.lifecycle.memory

import com.makeevrserg.kaleidos.core.lifecycle.api.Lifecycle

internal class LambdaLifecycle(
    private val onEnable: () -> Unit = {},
    private val onDisable: () -> Unit = {}
) : Lifecycle {
    override fun onEnable() = onEnable.invoke()

    override fun onDisable() = onDisable.invoke()
}
