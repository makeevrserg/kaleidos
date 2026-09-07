package com.makeevrserg.compose.html.preview.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortListenerCommandTest {
    private val lsof = LsofPortListenerCommand()

    private val netstat = NetstatPortListenerCommand()

    @Test
    fun GIVEN_lsof_WHEN_arguments_THEN_names_are_not_resolved_and_only_listeners_are_selected() {
        assertEquals(listOf("lsof", "-nP", "-iTCP:8080", "-sTCP:LISTEN", "-t"), lsof.arguments(8080))
    }

    @Test
    fun GIVEN_forked_listeners_WHEN_parsePid_THEN_the_first_one() {
        assertEquals(55468L, lsof.parsePid("55468\n55470\n", port = 8080))
    }

    @Test
    fun GIVEN_nothing_listens_WHEN_parsePid_THEN_null() {
        assertNull(lsof.parsePid("", port = 8080))
        assertNull(netstat.parsePid("", port = 8080))
    }

    @Test
    fun GIVEN_lsof_that_printed_an_error_WHEN_parsePid_THEN_null() {
        assertNull(lsof.parsePid("lsof: command not found\n", port = 8080))
    }

    @Test
    fun GIVEN_netstat_output_WHEN_parsePid_THEN_the_pid_of_the_listener_of_that_port() {
        val output = """
            Active Connections

              Proto  Local Address          Foreign Address        State           PID
              TCP    0.0.0.0:135            0.0.0.0:0              LISTENING       960
              TCP    127.0.0.1:8080         127.0.0.1:53124        ESTABLISHED     4242
              TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       4242
              TCP    [::]:8080              [::]:0                 LISTENING       4242
        """.trimIndent()

        assertEquals(4242L, netstat.parsePid(output, port = 8080))
    }

    /** `:18080` ends with `8080` but is another port; only the whole port counts. */
    @Test
    fun GIVEN_netstat_listener_of_a_port_ending_the_same_WHEN_parsePid_THEN_null() {
        val output = "  TCP    0.0.0.0:18080          0.0.0.0:0              LISTENING       4242"

        assertNull(netstat.parsePid(output, port = 8080))
    }

    @Test
    fun GIVEN_a_pid_WHEN_stopCommand_THEN_the_command_of_that_shell() {
        assertEquals("kill 4242", lsof.stopCommand(4242))
        assertEquals("taskkill /PID 4242 /F", netstat.stopCommand(4242))
    }
}
