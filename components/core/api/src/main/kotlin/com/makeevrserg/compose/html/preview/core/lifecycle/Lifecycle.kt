package com.makeevrserg.compose.html.preview.core.lifecycle

/**
 * Start and stop hooks of a component. Modules expose one so the composition root can bring the
 * whole graph up in dependency order and down in reverse, without knowing what each component does.
 */
interface Lifecycle {
    fun onEnable()

    fun onDisable()
}
