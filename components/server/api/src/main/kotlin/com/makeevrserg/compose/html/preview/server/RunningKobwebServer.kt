package com.makeevrserg.compose.html.preview.server

/**
 * The Kobweb server of a module as the module recorded it, while that process is still alive.
 *
 * @param process the server itself, which outlives the Gradle run that started it
 */
data class RunningKobwebServer(
    val port: Int,
    val process: ProcessHandle
)
