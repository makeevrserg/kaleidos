@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.api.service

import com.makeevrserg.kaleidos.server.api.fake.FakePortListenerCommand
import com.makeevrserg.kaleidos.server.api.memory.LsofPortListenerCommand
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.coroutines.EmptyCoroutineContext
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

    private fun lookupOf(arguments: List<String>, timeout: Duration): OsPortListenerLookup {
        return OsPortListenerLookup(
            command = FakePortListenerCommand(arguments, parsedPid = ProcessHandle.current().pid()),
            ioContext = EmptyCoroutineContext,
            timeout = timeout
        )
    }

    @Test
    fun GIVEN_this_process_listens_WHEN_find_THEN_it_is_named_with_its_command_line(): TestResult {
        return runTest {
            ServerSocket(0).use { socket ->
                val port = socket.localPort
                val listener = assertNotNull(lookup.find(port), "lsof did not name the listener of port $port")

                assertEquals(ProcessHandle.current().pid(), listener.pid)
                assertEquals("kill ${listener.pid}", listener.stopCommand)
                assertTrue(listener.commandLine.orEmpty().contains("java"), listener.commandLine.orEmpty())
            }
        }
    }

    @Test
    fun GIVEN_nobody_listens_WHEN_find_THEN_null(): TestResult {
        return runTest {
            val port = ServerSocket(0).use(ServerSocket::getLocalPort)

            assertNull(lookup.find(port))
        }
    }

    @Test
    fun GIVEN_a_command_this_machine_does_not_have_WHEN_find_THEN_null(): TestResult {
        return runTest {
            val missing = lookupOf(listOf("kaleidos-no-such-command"), COMMAND_TIMEOUT)

            ServerSocket(0).use { socket ->
                assertNull(missing.find(socket.localPort))
            }
        }
    }

    @Test
    fun GIVEN_a_command_that_does_not_answer_WHEN_find_THEN_null_after_the_timeout(): TestResult {
        return runTest {
            val hanging = lookupOf(listOf("sleep", "60"), SHORT_TIMEOUT)

            ServerSocket(0).use { socket ->
                assertNull(hanging.find(socket.localPort))
            }
        }
    }

    private companion object {
        val COMMAND_TIMEOUT = 5.seconds
        val SHORT_TIMEOUT = 200.milliseconds
    }
}
