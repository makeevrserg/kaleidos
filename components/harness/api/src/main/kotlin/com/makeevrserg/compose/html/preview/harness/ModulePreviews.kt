package com.makeevrserg.compose.html.preview.harness

/**
 * Previews found in the sources of one Gradle module.
 *
 * @param moduleDirectory absolute directory of the module, the identity Gradle modules are matched by
 */
data class ModulePreviews(
    val moduleDirectory: String,
    val previews: List<HarnessPreview>
)
