package com.makeevrserg.kaleidos.server

/**
 * Finds the origin a dev server announces in its output, so no port has to be configured:
 * webpack-dev-server prints `Loopback: http://localhost:8085/`, Kobweb prints
 * `A Kobweb server is now running at http://localhost:8086`.
 */
class DevServerUrlDetector {

    /** Whatever interface the server bound to, the IDE reaches it through loopback. */
    fun find(text: CharSequence): String? {
        val port = URL_PATTERN.find(text)?.groupValues?.get(PORT_GROUP) ?: return null
        return "http://localhost:$port"
    }

    private companion object {
        const val PORT_GROUP = 1
        val URL_PATTERN = Regex("""https?://(?:localhost|127\.0\.0\.1|0\.0\.0\.0):(\d{1,5})""")
    }
}
