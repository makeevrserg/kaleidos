package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.PreviewHost

/**
 * Execution names tell the plugin's runs apart from the user's own in the Run tool window; only runs
 * carrying them are ever stopped by the plugin.
 */
object DevServerRunNames {
    fun devServer(host: PreviewHost): String = "Kaleidos dev server (${host.displayName})"
}
