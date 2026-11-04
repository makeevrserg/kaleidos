package com.makeevrserg.kaleidos.harness.api.util

import com.makeevrserg.kaleidos.host.api.model.DevServerKind

const val TITLE = "Kaleidos"
const val WEBPACK_FILE_NAME = "kaleidos.html"
const val WEBPACK_BUNDLE_NAME = "kaleidos.js"
const val KOBWEB_ROUTE = "/kaleidos"

fun DevServerKind.previewPagePath(): String {
    return when (this) {
        DevServerKind.KOBWEB -> KOBWEB_ROUTE
        DevServerKind.WEBPACK -> "/$WEBPACK_FILE_NAME"
    }
}
