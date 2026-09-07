package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.server.KobwebServerFixtures.writeServerState
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Real time and a real child process: the stopper talks to the operating system. */
class KobwebServerStopperTest {
    private val moduleDirectory: Path = Files.createTempDirectory("kobweb-module")

    private val kobwebHost = host(":instances:web-preview-kobweb", DevServerKind.KOBWEB, moduleDirectory)

    private val stopper = KobwebServerStopper(
        stateReader = KobwebServerStateReader(ioContext = EmptyCoroutineContext),
        ioContext = EmptyCoroutineContext,
        stopTimeout = 5.seconds
    )

    private var serverProcess: Process? = null

    private fun startFakeServer(): Process {
        val process = ProcessBuilder("sleep", "60").start()
        serverProcess = process
        return process
    }

    private fun writeState(pid: Long) = writeServerState(moduleDirectory, port = 8086, pid = pid)

    @AfterTest
    fun cleanUp() {
        serverProcess?.destroyForcibly()
        moduleDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_state_file_points_at_live_process_WHEN_stop_THEN_process_is_terminated() = runBlocking {
        val process = startFakeServer()
        writeState(process.pid())

        stopper.stop(kobwebHost)

        assertFalse(process.isAlive)
    }

    @Test
    fun GIVEN_no_state_file_WHEN_stop_THEN_other_processes_are_left_alone() = runBlocking {
        val bystander = startFakeServer()

        stopper.stop(kobwebHost)

        assertTrue(bystander.isAlive)
    }

    @Test
    fun GIVEN_stale_state_file_WHEN_stop_THEN_no_failure() = runBlocking {
        val process = startFakeServer()
        val pid = process.pid()
        process.destroyForcibly()
        process.waitFor()
        writeState(pid)

        stopper.stop(kobwebHost)
    }
}
