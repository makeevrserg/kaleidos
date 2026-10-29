package com.makeevrserg.kaleidos.server.memory

internal class DevServerUrlDetector {
    fun find(text: CharSequence): String? {
        val port = URL_PATTERN.find(text)?.groupValues?.get(PORT_GROUP) ?: return null
        return "http://localhost:$port"
    }

    private companion object {
        const val PORT_GROUP = 1
        val URL_PATTERN = Regex("""https?://(?:localhost|127\.0\.0\.1|0\.0\.0\.0):(\d{1,5})""")
    }
}
