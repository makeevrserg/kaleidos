package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.host.DevServerKind

/**
 * Where the generated page answers on the dev server. Kobweb routes the path through its site, while
 * webpack serves a file next to the bundle, so an `index.html` of the module keeps working.
 */
object PreviewPagePath {
    /**
     * Title the generated page gives the document, and the only way to tell it from another page the
     * dev server answered with. A Kobweb site answers every path with the shell of the real site, so a
     * server whose bundle was built without the page returns HTTP 200 and renders nothing.
     */
    const val TITLE = "Compose HTML Preview"

    const val WEBPACK_FILE_NAME = "compose-html-preview.html"
    const val WEBPACK_BUNDLE_NAME = "compose-html-preview.js"
    const val KOBWEB_ROUTE = "/compose-html-preview"

    fun of(kind: DevServerKind): String = when (kind) {
        DevServerKind.KOBWEB -> KOBWEB_ROUTE
        DevServerKind.WEBPACK -> "/$WEBPACK_FILE_NAME"
    }
}
