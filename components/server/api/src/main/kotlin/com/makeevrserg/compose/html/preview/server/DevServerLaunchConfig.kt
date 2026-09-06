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
    val arguments: String,
    val initScriptPath: String
) {
    /** Root tasks are addressed as `:task`: a bare name would run the task in every subproject. */
    val qualifiedTaskName: String
        get() = "$gradlePath:$taskName"

    /** Quoted, because a project can live under a path with spaces in it. */
    val scriptParameters: String
        get() = "--init-script \"$initScriptPath\" $arguments"

    companion object {
        fun forHost(host: PreviewHost, options: DevServerLaunchOptions): DevServerLaunchConfig {
            return DevServerLaunchConfig(
                rootProjectPath = host.rootProjectPath,
                gradlePath = host.gradlePath,
                taskName = host.kind.startTask,
                arguments = host.kind.arguments,
                initScriptPath = options.initScriptPath
            )
        }
    }
}
