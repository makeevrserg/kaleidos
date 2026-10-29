package com.makeevrserg.kaleidos.url.util

import java.net.URLEncoder

internal fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)
