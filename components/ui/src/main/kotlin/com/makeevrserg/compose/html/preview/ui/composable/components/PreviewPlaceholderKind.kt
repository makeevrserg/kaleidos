package com.makeevrserg.compose.html.preview.ui.composable.components

/** How a tool window that shows no page presents itself. */
internal enum class PreviewPlaceholderKind {

    /** A page is on its way. */
    LOADING,

    /** There is nothing to preview, and nothing is wrong. */
    INFO,

    /** The preview cannot be rendered. */
    FAILURE
}
