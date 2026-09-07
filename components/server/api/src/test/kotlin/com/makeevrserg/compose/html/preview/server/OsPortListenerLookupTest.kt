package com.makeevrserg.compose.html.preview.server

import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A real socket and the real command of this machine: nothing else can answer who holds a port. */
class OsPortListenerLookupTest {
    private val lookup = OsPortListenerLookup(
        command = LsofPortListenerCommand(),
        ioContext = EmptyCoroutineContext,
        timeout = COMMAND_TIMEOUT
    )

    private var socket: ServerSocket? = null

    private fun listen(): Int {
        val opened = ServerSocket(0)
        socket = opened
        return opened.localPort
    }

    private fun lookupOf(arguments: List<String>, timeout: Duration): OsPortListenerLookup {
        return OsPortListenerLookup(
            command = FakePortListenerCommand(arguments, parsedPid = ProcessHandle.current().pid()),
            ioContext = EmptyCoroutineContext,
            timeout = timeout
        )
    }

    @AfterTest
    fun cleanUp() {
        socket?.close()
    }

    @Test
    fun GIVEN_this_process_listens_WHEN_find_THEN_it_is_named_with_its_command_line() = runBlocking {
        val port = listen()

        val listener = assertNotNull(lookup.find(port), "lsof did not name the listener of port $port")

        assertEquals(ProcessHandle.current().pid(), listener.pid)
        assertEquals("kill ${listener.pid}", listener.stopCommand)
        assertTrue(listener.commandLine.orEmpty().contains("java"), listener.commandLine.orEmpty())
    }

    @Test
    fun GIVEN_nobody_listens_WHEN_find_THEN_null() = runBlocking {
        val port = listen()
        socket?.close()

        assertNull(lookup.find(port))
    }

    @Test
    fun GIVEN_a_command_this_machine_does_not_have_WHEN_find_THEN_null() = runBlocking {
        val missing = lookupOf(listOf("compose-html-preview-no-such-command"), COMMAND_TIMEOUT)

        assertNull(missing.find(listen()))
    }

    @Test
    fun GIVEN_a_command_that_does_not_answer_WHEN_find_THEN_null_after_the_timeout() = runBlocking {
        val hanging = lookupOf(listOf("sleep", "60"), SHORT_TIMEOUT)

        assertNull(hanging.find(listen()))
    }

    private companion object {
        val COMMAND_TIMEOUT = 5.seconds
        val SHORT_TIMEOUT = 200.milliseconds
    }
}
