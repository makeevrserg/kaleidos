package com.makeevrserg.compose.html.preview.ui.state

/** What fills the centre of the tool window. */
sealed interface PreviewContent {

    /** The browser shows the page at [url]. */
    data class Page(
        val url: String
    ) : PreviewContent

    /** There is no page on screen yet; [text] says what is being waited for. */
    data class Loading(
        val text: String
    ) : PreviewContent

    /** There is nothing to preview here and nothing is wrong; [text] says what the file lacks. */
    data class Empty(
        val text: String
    ) : PreviewContent

    /** The preview cannot be rendered; [text] says why and how to recover. */
    data class Failure(
        val text: String
    ) : PreviewContent
}
