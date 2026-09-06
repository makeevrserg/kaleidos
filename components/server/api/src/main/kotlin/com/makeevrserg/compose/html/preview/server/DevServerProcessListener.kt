package com.makeevrserg.compose.html.preview.server

interface DevServerProcessListener {
    /** Console output of the run as it arrives; a chunk may hold several lines or part of one. */
    fun onOutput(text: String)

    /**
     * Called once the Gradle task has finished. A continuous build only finishes when it is
     * stopped, so [isSuccess] false means the task failed before or while serving.
     */
    fun onExited(isSuccess: Boolean)
}
