package com.makeevrserg.kaleidos.server.service

import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.api.DetachedServerStopper
import com.makeevrserg.kaleidos.server.api.KobwebServerStateReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Does what `kobwebStop` does, without Gradle: the server the module recorded for itself is asked to
 * terminate and, if it does not exit within [stopTimeout], killed. Only that process is touched, so a
 * program of someone else answering on the same port is left alone.
 */
internal class KobwebServerStopper(
    private val stateReader: KobwebServerStateReader,
    private val ioContext: CoroutineContext,
    private val stopTimeout: Duration
) : DetachedServerStopper {

    private suspend fun awaitExit(process: ProcessHandle): Boolean {
        return withTimeoutOrNull(stopTimeout) {
            while (process.isAlive) delay(EXIT_POLL_INTERVAL)
            true
        } ?: false
    }

    override suspend fun stop(host: PreviewHost) {
        return withContext(ioContext) {
            val process = stateReader.readRunning(host.directory)?.process ?: return@withContext
            process.destroy()
            if (!awaitExit(process)) process.destroyForcibly()
        }
    }

    private companion object {
        val EXIT_POLL_INTERVAL = 100.milliseconds
    }
}
