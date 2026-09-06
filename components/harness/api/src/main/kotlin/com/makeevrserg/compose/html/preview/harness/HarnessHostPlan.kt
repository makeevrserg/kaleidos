package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.host.DevServerKind

/**
 * The module the preview page is generated into.
 *
 * @param devServerPort port the webpack dev server is told to listen on; null for Kobweb, whose port
 * comes from `.kobweb/conf.yaml`
 * @param excludedSourcePaths entry points of the module itself, left out of the compilation because the
 * generated page brings its own `main`
 */
data class HarnessHostPlan(
    val gradlePath: String,
    val directory: String,
    val rootProjectPath: String,
    val kind: DevServerKind,
    val devServerPort: Int?,
    val excludedSourcePaths: List<String>
)
