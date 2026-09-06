package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.host.DevServerKind

/**
 * Where the generated page answers on the dev server. Kobweb routes the path through its site, while
 * webpack serves a file next to the bundle, so an `index.html` of the module keeps working.
 */
object PreviewPagePath {
    const val WEBPACK_FILE_NAME = "compose-html-preview.html"
    const val WEBPACK_BUNDLE_NAME = "compose-html-preview.js"
    const val KOBWEB_ROUTE = "/compose-html-preview"

    fun of(kind: DevServerKind): String = when (kind) {
        DevServerKind.KOBWEB -> KOBWEB_ROUTE
        DevServerKind.WEBPACK -> "/$WEBPACK_FILE_NAME"
    }
}
