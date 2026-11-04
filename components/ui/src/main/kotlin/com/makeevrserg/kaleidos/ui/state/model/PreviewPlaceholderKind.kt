package com.makeevrserg.kaleidos.ui.state.model

/** How a tool window that shows no page presents itself. */
internal enum class PreviewPlaceholderKind {

    /** A page is on its way. */
    LOADING,

    /** There is nothing to preview, and nothing is wrong. */
    INFO,

    /** The preview cannot be rendered. */
    FAILURE
}
