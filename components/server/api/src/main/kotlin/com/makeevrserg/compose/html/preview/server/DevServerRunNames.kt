package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * Execution names tell the plugin's runs apart from the user's own in the Run tool window; only runs
 * carrying them are ever stopped by the plugin.
 */
object DevServerRunNames {
    const val STOP_TASK = "Compose HTML Preview stop"

    fun devServer(host: PreviewHost): String = "Compose HTML Preview dev server (${host.displayName})"
}
