package com.makeevrserg.compose.html.preview.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevServerRunListenerTest {
    private val announced = mutableListOf<String>()

    private val exits = mutableListOf<Boolean>()

    private val listener = DevServerRunListener(
        urlDetector = DevServerUrlDetector(),
        onBaseUrlAnnounced = announced::add,
        onExited = exits::add
    )

    @Test
    fun GIVEN_url_split_across_chunks_WHEN_output_arrives_THEN_announced_once_complete() {
        listener.onOutput("Loopback: http://loc")
        assertTrue(announced.isEmpty())

        listener.onOutput("alhost:8085/\n")

        assertEquals(listOf("http://localhost:8085"), announced)
    }

    @Test
    fun GIVEN_url_already_announced_WHEN_more_urls_arrive_THEN_ignored() {
        listener.onOutput("Loopback: http://localhost:8085/\n")
        listener.onOutput("Loopback: http://localhost:9999/\n")

        assertEquals(listOf("http://localhost:8085"), announced)
    }

    @Test
    fun GIVEN_finished_lines_without_url_WHEN_url_arrives_later_THEN_announced() {
        listener.onOutput("> Task :preview:compileKotlinJs\n> Task :preview:jsBrowserDevelopmentRun\n")
        listener.onOutput("<i> [webpack-dev-server] Loopback: http://localhost:8085/\n")

        assertEquals(listOf("http://localhost:8085"), announced)
    }

    @Test
    fun GIVEN_run_exits_WHEN_onExited_THEN_outcome_forwarded() {
        listener.onExited(isSuccess = false)
        listener.onExited(isSuccess = true)

        assertEquals(listOf(false, true), exits)
    }
}
