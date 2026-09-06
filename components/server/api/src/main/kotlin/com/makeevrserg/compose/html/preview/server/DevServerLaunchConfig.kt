package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * @param rootProjectPath directory of the linked Gradle build the task is executed in
 * @param gradlePath `:a:b` of the module; empty for the root project
 * @param arguments raw command line passed to Gradle after the task name
 */
data class DevServerLaunchConfig(
    val rootProjectPath: String,
    val gradlePath: String,
    val taskName: String,
    val arguments: String
) {
    /** Root tasks are addressed as `:task`: a bare name would run the task in every subproject. */
    val qualifiedTaskName: String
        get() = "$gradlePath:$taskName"

    companion object {
        fun forHost(host: PreviewHost, taskName: String, arguments: String): DevServerLaunchConfig {
            return DevServerLaunchConfig(
                rootProjectPath = host.rootProjectPath,
                gradlePath = host.gradlePath,
                taskName = taskName,
                arguments = arguments
            )
        }
    }
}
