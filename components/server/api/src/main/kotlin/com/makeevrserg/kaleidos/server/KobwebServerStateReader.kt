package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * What a Kobweb server records about itself in `.kobweb/server/state.yaml` while it runs.
 *
 * This is the only proof of who answers on a port: a Kobweb server outlives its Gradle run and every
 * Kobweb module of a build declares the same default port, so an answer at the address of a module
 * says nothing about whose server gave it. A record left behind by a server that crashed points at no
 * live process and is ignored, exactly as `kobwebStart` ignores it.
 */
class KobwebServerStateReader(private val ioContext: CoroutineContext) {

    private fun value(state: String, pattern: Regex): String? = pattern.find(state)?.groupValues?.get(VALUE_GROUP)

    private fun runningServer(state: String): RunningKobwebServer? {
        val port = value(state, PORT_PATTERN)?.toIntOrNull() ?: return null
        val pid = value(state, PID_PATTERN)?.toLongOrNull() ?: return null
        val process = ProcessHandle.of(pid).orElse(null) ?: return null
        return RunningKobwebServer(port = port, process = process)
    }

    /** Null when the module never ran a server, when its record is incomplete, or when that process is gone. */
    suspend fun readRunning(moduleDirectory: String): RunningKobwebServer? = withContext(ioContext) {
        val stateFile = Path.of(moduleDirectory, KOBWEB_DIRECTORY, SERVER_DIRECTORY, STATE_FILE_NAME)
        val state = runCatching { Files.readString(stateFile) }.getOrNull() ?: return@withContext null
        runningServer(state)
    }

    private companion object {
        const val KOBWEB_DIRECTORY = ".kobweb"
        const val SERVER_DIRECTORY = "server"
        const val STATE_FILE_NAME = "state.yaml"
        const val VALUE_GROUP = 1
        val PORT_PATTERN = Regex("""(?m)^\s*port:\s*(\d+)\s*$""")
        val PID_PATTERN = Regex("""(?m)^\s*pid:\s*(\d+)\s*$""")
    }
}
