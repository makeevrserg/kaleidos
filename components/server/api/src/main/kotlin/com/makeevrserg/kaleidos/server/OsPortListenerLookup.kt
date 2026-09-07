package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * Runs the command of the operating system and reads the process it names.
 *
 * The command is waited for before its output is read, so one that never answers costs [timeout]
 * instead of a blocked thread. The price is the buffer of the pipe: a command printing more than the
 * operating system can hold never exits, and the port is then reported without a name.
 */
class OsPortListenerLookup(
    private val command: PortListenerCommand,
    private val ioContext: CoroutineContext,
    private val timeout: Duration
) : PortListenerLookup {

    /** Null when the command is not installed, fails, or does not answer in time. */
    private fun readOutput(port: Int): String? {
        val process = runCatching {
            ProcessBuilder(command.arguments(port)).redirectErrorStream(true).start()
        }.getOrNull() ?: return null
        return try {
            val hasExited = process.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            if (hasExited) process.inputStream.bufferedReader().use(BufferedReader::readText) else null
        } finally {
            process.destroyForcibly()
        }
    }

    override suspend fun find(port: Int): PortListener? = withContext(ioContext) {
        val output = readOutput(port) ?: return@withContext null
        val pid = command.parsePid(output, port) ?: return@withContext null
        // Gone between the two calls: the port is free again, so there is nobody to report.
        val process = ProcessHandle.of(pid).orElse(null) ?: return@withContext null
        PortListener(
            pid = pid,
            commandLine = process.info().commandLine().orElse(null),
            stopCommand = command.stopCommand(pid)
        )
    }
}
