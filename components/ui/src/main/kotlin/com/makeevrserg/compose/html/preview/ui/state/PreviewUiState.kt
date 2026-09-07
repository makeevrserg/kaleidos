package com.makeevrserg.compose.html.preview.ui.state

/**
 * Everything the tool window renders. Derived from the store state by [PreviewUiStateFactory], so
 * the composables hold no logic of their own.
 *
 * @param pageUrl what the browser is told to load, and with it whether the browser takes the place of
 * the content at all; null blanks it and gives it no room, so a message is never covered by it
 * @param statusText footer text; empty while the content already explains everything
 */
data class PreviewUiState(
    val banner: PreviewBanner?,
    val content: PreviewContent,
    val pageUrl: String?,
    val statusText: String
)
