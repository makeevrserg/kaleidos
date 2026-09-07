package com.makeevrserg.kaleidos.notification

/** Tells the user about a failure outside of the tool window, which may be hidden at that moment. */
interface PreviewNotifier {
    fun error(content: String)
}
