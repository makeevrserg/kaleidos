package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Does what `kobwebStop` does, without Gradle: the Kobweb server records its process id in
 * `.kobweb/server/state.yaml`, so it is asked to terminate and, if it does not exit within
 * [stopTimeout], killed. A stale state file left by a crashed server points at no live process and is
 * ignored, exactly as `kobwebStart` ignores it.
 */
class KobwebServerStopper(
    private val ioContext: CoroutineContext,
    private val stopTimeout: Duration
) : DetachedServerStopper {

    private fun parsePid(state: String): Long? = PID_PATTERN.find(state)?.groupValues?.get(PID_GROUP)?.toLongOrNull()

    private fun findServerProcess(host: PreviewHost): ProcessHandle? {
        val stateFile = Path.of(host.directory, KOBWEB_DIRECTORY, SERVER_DIRECTORY, STATE_FILE_NAME)
        val pid = runCatching { Files.readString(stateFile) }.map(::parsePid).getOrNull() ?: return null
        return ProcessHandle.of(pid).orElse(null)
    }

    private suspend fun awaitExit(process: ProcessHandle): Boolean {
        return withTimeoutOrNull(stopTimeout) {
            while (process.isAlive) delay(EXIT_POLL_INTERVAL)
            true
        } ?: false
    }

    override suspend fun stop(host: PreviewHost) = withContext(ioContext) {
        val process = findServerProcess(host) ?: return@withContext
        process.destroy()
        if (!awaitExit(process)) process.destroyForcibly()
    }

    private companion object {
        const val KOBWEB_DIRECTORY = ".kobweb"
        const val SERVER_DIRECTORY = "server"
        const val STATE_FILE_NAME = "state.yaml"
        const val PID_GROUP = 1
        val PID_PATTERN = Regex("""(?m)^\s*pid:\s*(\d+)\s*$""")
        val EXIT_POLL_INTERVAL = 100.milliseconds
    }
}
