package com.makeevrserg.compose.html.preview.ui.state

/**
 * Everything the tool window renders. Derived from the store state by [PreviewUiStateFactory], so
 * the composables hold no logic of their own.
 *
 * @param pageUrl what the browser is told to load, null when it should be left alone. Set while
 * [content] is still [PreviewContent.Loading] or [PreviewContent.Failure]: the page is loaded out of
 * sight and only shown once the browser reports it, so a page of the previous file is never visible.
 * @param statusText footer text; empty while the content already explains everything
 */
data class PreviewUiState(
    val banner: PreviewBanner?,
    val content: PreviewContent,
    val pageUrl: String?,
    val statusText: String
)
