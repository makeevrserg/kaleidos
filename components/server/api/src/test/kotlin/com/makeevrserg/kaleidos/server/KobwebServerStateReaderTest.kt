package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.server.KobwebServerFixtures.writeServerState
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A real process: whether a recorded server still runs is a question only the operating system answers. */
class KobwebServerStateReaderTest {
    private val moduleDirectory: Path = Files.createTempDirectory("kobweb-module")

    private val reader = KobwebServerStateReader(ioContext = EmptyCoroutineContext)

    private var serverProcess: Process? = null

    private fun startFakeServer(): Process {
        val process = ProcessBuilder("sleep", "60").start()
        serverProcess = process
        return process
    }

    @AfterTest
    fun cleanUp() {
        serverProcess?.destroyForcibly()
        moduleDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_record_of_a_live_server_WHEN_readRunning_THEN_its_port_and_process() = runBlocking {
        val process = startFakeServer()
        writeServerState(moduleDirectory, port = 8086, pid = process.pid())

        val running = reader.readRunning(moduleDirectory.toString())

        assertEquals(8086, running?.port)
        assertEquals(process.pid(), running?.process?.pid())
    }

    @Test
    fun GIVEN_module_that_never_ran_a_server_WHEN_readRunning_THEN_null() = runBlocking {
        assertNull(reader.readRunning(moduleDirectory.toString()))
    }

    /** What a server that crashed leaves behind, and what `kobwebStart` ignores just as well. */
    @Test
    fun GIVEN_record_of_a_server_that_is_gone_WHEN_readRunning_THEN_null() = runBlocking {
        val process = startFakeServer()
        val pid = process.pid()
        process.destroyForcibly()
        process.waitFor()
        writeServerState(moduleDirectory, port = 8086, pid = pid)

        assertNull(reader.readRunning(moduleDirectory.toString()))
    }

    @Test
    fun GIVEN_record_without_a_port_WHEN_readRunning_THEN_null() = runBlocking {
        moduleDirectory.resolve(".kobweb/server").createDirectories()
        moduleDirectory.resolve(".kobweb/server/state.yaml").writeText(
            """
            env: "DEV"
            pid: 1
            """.trimIndent()
        )

        assertNull(reader.readRunning(moduleDirectory.toString()))
    }

    @Test
    fun GIVEN_record_without_a_pid_WHEN_readRunning_THEN_null() = runBlocking {
        moduleDirectory.resolve(".kobweb/server").createDirectories()
        moduleDirectory.resolve(".kobweb/server/state.yaml").writeText(
            """
            env: "DEV"
            port: 8086
            """.trimIndent()
        )

        assertNull(reader.readRunning(moduleDirectory.toString()))
    }
}
