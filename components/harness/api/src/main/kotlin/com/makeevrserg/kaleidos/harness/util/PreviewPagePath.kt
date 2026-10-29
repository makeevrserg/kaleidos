package com.makeevrserg.kaleidos.harness.util

import com.makeevrserg.kaleidos.host.model.DevServerKind

const val TITLE = "Kaleidos"
const val WEBPACK_FILE_NAME = "kaleidos.html"
const val WEBPACK_BUNDLE_NAME = "kaleidos.js"
const val KOBWEB_ROUTE = "/kaleidos"

fun previewPagePath(kind: DevServerKind): String {
    return when (kind) {
        DevServerKind.KOBWEB -> KOBWEB_ROUTE
        DevServerKind.WEBPACK -> "/$WEBPACK_FILE_NAME"
    }
}
