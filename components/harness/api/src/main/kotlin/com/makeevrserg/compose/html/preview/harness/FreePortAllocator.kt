package com.makeevrserg.compose.html.preview.harness

/**
 * Picks the port of a webpack dev server. The plugin decides it instead of the module, so a preview
 * never collides with the real application of the project on the default port.
 */
interface FreePortAllocator {
    /** The same [key] keeps its port while the project is open, so a running server is found again. */
    fun allocate(key: String): Int
}
