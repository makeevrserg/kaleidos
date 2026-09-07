package com.makeevrserg.kaleidos.host

/**
 * Gradle module that serves the preview page for a file.
 *
 * @param gradlePath `:instances:web-preview`; empty for the root project
 * @param directory absolute path of the module, shared by every IDE module imported from it
 * @param rootProjectPath directory of the linked Gradle build the tasks are executed in
 */
data class PreviewHost(
    val gradlePath: String,
    val directory: String,
    val rootProjectPath: String,
    val kind: DevServerKind
) {
    val displayName: String
        get() = gradlePath.ifEmpty { "root project" }
}
