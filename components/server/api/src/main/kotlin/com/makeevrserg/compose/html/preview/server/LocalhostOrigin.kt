package com.makeevrserg.compose.html.preview.server

/** Origin of a dev server of this machine, without trailing slash: `http://localhost:8086`. */
internal object LocalhostOrigin {
    fun ofPort(port: Int): String = "http://localhost:$port"
}
