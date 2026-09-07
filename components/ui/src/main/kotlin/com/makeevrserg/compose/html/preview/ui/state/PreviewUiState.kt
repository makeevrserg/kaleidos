package com.makeevrserg.compose.html.preview.ui.state

/**
 * Everything the tool window renders. Derived from the store state by [PreviewUiStateFactory], so
 * the composables hold no logic of their own.
 *
 * @param statusText footer text; empty while there is nothing to say about the dev server
 */
data class PreviewUiState(
    val banner: PreviewBanner?,
    val content: PreviewContent,
    val statusText: String
)
