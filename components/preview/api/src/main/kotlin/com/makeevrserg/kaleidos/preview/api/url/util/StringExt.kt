package com.makeevrserg.kaleidos.preview.api.url.util

import java.net.URLEncoder

internal fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)
