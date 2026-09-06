package com.makeevrserg.compose.html.preview.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnnouncedBaseUrlParserTest {
    private val parser = AnnouncedBaseUrlParser(DevServerUrlDetector())

    @Test
    fun GIVEN_url_split_across_chunks_WHEN_output_arrives_THEN_announced_once_complete() {
        assertNull(parser.feed("Loopback: http://loc"))

        assertEquals("http://localhost:8085", parser.feed("alhost:8085/\n"))
    }

    @Test
    fun GIVEN_url_already_announced_WHEN_more_urls_arrive_THEN_ignored() {
        parser.feed("Loopback: http://localhost:8085/\n")

        assertNull(parser.feed("Loopback: http://localhost:9999/\n"))
    }

    @Test
    fun GIVEN_finished_lines_without_url_WHEN_url_arrives_later_THEN_announced() {
        assertNull(parser.feed("> Task :preview:compileKotlinJs\n> Task :preview:jsBrowserDevelopmentRun\n"))

        val announced = parser.feed("<i> [webpack-dev-server] Loopback: http://localhost:8085/\n")

        assertEquals("http://localhost:8085", announced)
    }
}
