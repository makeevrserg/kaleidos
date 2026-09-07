package com.makeevrserg.compose.html.preview.ui.state

/** What fills the centre of the tool window. */
sealed interface PreviewContent {

    /** The dev server answers and the browser renders [url]. */
    data class Page(val url: String) : PreviewContent

    /** There is no page yet; [text] says what is being waited for. */
    data class Loading(val text: String) : PreviewContent

    /** No page can appear at all; [text] says why. */
    data class Message(val text: String) : PreviewContent
}
