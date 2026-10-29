package com.makeevrserg.kaleidos.server.api

interface AnnouncedBaseUrlParser {
    fun feed(text: String): String?
}
