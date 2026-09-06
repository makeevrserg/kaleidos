package com.makeevrserg.compose.html.preview.server

/**
 * What the generated preview application needs from the run that serves it.
 *
 * @param initScriptPath init script that adds the generated sources to the build; the project itself
 * is never modified, so every other build sees it unchanged
 * @param devServerPort port the plugin picked for a webpack dev server; null for Kobweb, which takes
 * its port from `.kobweb/conf.yaml`
 */
data class DevServerLaunchOptions(
    val initScriptPath: String,
    val devServerPort: Int?
)
