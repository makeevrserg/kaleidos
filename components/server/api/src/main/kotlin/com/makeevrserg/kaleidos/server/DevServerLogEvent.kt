package com.makeevrserg.kaleidos.server

sealed interface DevServerLogEvent {
    data class RunStarted(val config: DevServerLaunchConfig) : DevServerLogEvent

    data class Output(val text: String) : DevServerLogEvent

    data class Adopted(val baseUrl: String) : DevServerLogEvent
}
