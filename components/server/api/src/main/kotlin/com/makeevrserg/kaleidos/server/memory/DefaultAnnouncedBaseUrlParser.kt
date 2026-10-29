package com.makeevrserg.kaleidos.server.memory

import com.makeevrserg.kaleidos.server.api.AnnouncedBaseUrlParser

/**
 * Finds the origin a run announces, once. Output arrives in chunks that may split a line, so the
 * unfinished tail is kept for the next chunk. One instance per run, fed from a single coroutine.
 */
internal class DefaultAnnouncedBaseUrlParser(
    private val urlDetector: DevServerUrlDetector
) : AnnouncedBaseUrlParser {
    private val pendingOutput = StringBuilder()

    private var isAnnounced = false

    /** The origin on the chunk that completes it; null before that and forever after. */
    override fun feed(text: String): String? {
        if (isAnnounced) return null
        pendingOutput.append(text)
        val baseUrl = urlDetector.find(pendingOutput)
        val lastLineBreak = pendingOutput.lastIndexOf(LINE_BREAK)
        if (lastLineBreak >= 0) pendingOutput.delete(0, lastLineBreak + 1)
        if (baseUrl != null) isAnnounced = true
        return baseUrl
    }

    private companion object {
        const val LINE_BREAK = "\n"
    }
}
