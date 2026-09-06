package com.makeevrserg.compose.html.preview.core.lifecycle

/**
 * Enables the children in the given order and disables them in reverse, so a component is never
 * stopped before the components that depend on it.
 */
class CompositeLifecycle(
    private val children: List<Lifecycle>
) : Lifecycle {
    override fun onEnable() = children.forEach(Lifecycle::onEnable)

    override fun onDisable() = children.reversed().forEach(Lifecycle::onDisable)
}
