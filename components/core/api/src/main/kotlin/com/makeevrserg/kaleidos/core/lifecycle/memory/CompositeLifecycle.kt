package com.makeevrserg.kaleidos.core.lifecycle.memory

import com.makeevrserg.kaleidos.core.lifecycle.api.Lifecycle

/**
 * Enables the children in the given order and disables them in reverse, so a component is never
 * stopped before the components that depend on it.
 */
internal class CompositeLifecycle(
    private val children: List<Lifecycle>
) : Lifecycle {
    override fun onEnable() = children.forEach(Lifecycle::onEnable)

    override fun onDisable() = children.reversed().forEach(Lifecycle::onDisable)
}
