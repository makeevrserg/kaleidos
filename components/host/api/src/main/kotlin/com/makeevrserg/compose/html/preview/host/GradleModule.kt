package com.makeevrserg.compose.html.preview.host

/**
 * One Gradle project of the build as imported by the IDE.
 *
 * @param gradlePath `:a:b`; empty for the root project
 * @param taskNames plain task names of the module, without the Gradle path
 */
data class GradleModule(
    val gradlePath: String,
    val directory: String,
    val rootProjectPath: String,
    val taskNames: Set<String>
) {
    val displayName: String
        get() = gradlePath.ifEmpty { "The root project" }
}
