package com.makeevrserg.compose.html.preview.server

/**
 * Watches one run: reports the origin the server prints, once, and the exit of the run. Output
 * arrives in chunks that may split a line, so the unfinished tail is kept for the next chunk.
 */
class DevServerRunListener(
    private val urlDetector: DevServerUrlDetector,
    private val onBaseUrlAnnounced: (baseUrl: String) -> Unit,
    private val onExited: (isSuccess: Boolean) -> Unit
) : DevServerProcessListener {
    private val pendingOutput = StringBuilder()

    private var isAnnounced = false

    override fun onOutput(text: String) = synchronized(pendingOutput) {
        if (isAnnounced) return@synchronized
        pendingOutput.append(text)
        val baseUrl = urlDetector.find(pendingOutput)
        val lastLineBreak = pendingOutput.lastIndexOf(LINE_BREAK)
        if (lastLineBreak >= 0) pendingOutput.delete(0, lastLineBreak + 1)
        if (baseUrl != null) {
            isAnnounced = true
            onBaseUrlAnnounced(baseUrl)
        }
    }

    override fun onExited(isSuccess: Boolean) = onExited.invoke(isSuccess)

    private companion object {
        const val LINE_BREAK = "\n"
    }
}
