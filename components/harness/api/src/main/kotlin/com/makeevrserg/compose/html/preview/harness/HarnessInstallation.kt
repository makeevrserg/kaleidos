package com.makeevrserg.compose.html.preview.harness

/**
 * The generated sources on disk, described the way the Gradle run needs them.
 *
 * @param initScriptPath init script that adds the generated sources to the build of the user, whose
 * own files stay untouched
 * @param devServerPort port the webpack dev server was told to use; null when the module decides
 */
data class HarnessInstallation(
    val initScriptPath: String,
    val devServerPort: Int?
)
