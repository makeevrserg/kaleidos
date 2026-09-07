package com.makeevrserg.compose.html.preview.ui.state

/** What fills the centre of the tool window. */
sealed interface PreviewContent {

    /** The browser shows the page at [url] and takes the whole area of the tool window. */
    data class Page(
        val url: String
    ) : PreviewContent

    /** There is no page on screen yet; [message] says what is being waited for. */
    data class Loading(
        val message: PreviewMessage
    ) : PreviewContent

    /** There is nothing to preview here and nothing is wrong; [message] says what the file lacks. */
    data class Empty(
        val message: PreviewMessage
    ) : PreviewContent

    /** The preview cannot be rendered; [message] says why and what to do. */
    data class Failure(
        val message: PreviewMessage
    ) : PreviewContent
}
