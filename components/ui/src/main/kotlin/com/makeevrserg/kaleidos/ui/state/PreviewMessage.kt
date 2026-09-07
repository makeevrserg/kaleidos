package com.makeevrserg.kaleidos.ui.state

/**
 * What a tool window with no page tells the user: the headline and the detail under it.
 *
 * @param title one line, what is going on
 * @param description why, and what to do about it
 */
data class PreviewMessage(
    val title: String,
    val description: String
)
