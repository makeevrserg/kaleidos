package com.makeevrserg.kaleidos.server

sealed interface GradleRunEvent {
    /** The task has been handed to the platform; its process may not exist yet. */
    data object Started : GradleRunEvent

    /** Console output as it arrives; a chunk may hold several lines or part of one. */
    data class Output(val text: String) : GradleRunEvent

    /**
     * A continuous build only exits when it is stopped, so [isSuccess] false means the task failed
     * before or while serving.
     */
    data class Exited(val isSuccess: Boolean) : GradleRunEvent
}
