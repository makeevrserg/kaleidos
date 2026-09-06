package com.makeevrserg.compose.html.preview.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevServerUrlDetectorTest {
    private val detector = DevServerUrlDetector()

    @Test
    fun GIVEN_webpack_loopback_line_WHEN_find_THEN_localhost_origin_with_port() {
        val origin = detector.find("<i> [webpack-dev-server] Loopback: http://localhost:8085/")

        assertEquals("http://localhost:8085", origin)
    }

    @Test
    fun GIVEN_kobweb_line_WHEN_find_THEN_origin_without_trailing_text() {
        val origin = detector.find("A Kobweb server is now running at http://localhost:8086")

        assertEquals("http://localhost:8086", origin)
    }

    @Test
    fun GIVEN_server_bound_to_all_interfaces_WHEN_find_THEN_loopback_origin() {
        assertEquals("http://localhost:3000", detector.find("On Your Network: http://0.0.0.0:3000/"))
        assertEquals("http://localhost:3000", detector.find("Listening on https://127.0.0.1:3000"))
    }

    @Test
    fun GIVEN_several_urls_WHEN_find_THEN_first_one_wins() {
        val origin = detector.find("Loopback: http://localhost:8085/, http://localhost:9000/")

        assertEquals("http://localhost:8085", origin)
    }

    @Test
    fun GIVEN_no_local_url_WHEN_find_THEN_null() {
        assertNull(detector.find("> Task :preview:jsBrowserDevelopmentRun"))
        assertNull(detector.find("Fetched https://repo.maven.apache.org:443/maven2"))
    }
}
