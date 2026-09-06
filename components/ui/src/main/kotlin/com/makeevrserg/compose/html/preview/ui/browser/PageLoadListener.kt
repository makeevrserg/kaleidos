package com.makeevrserg.compose.html.preview.ui.browser

fun interface PageLoadListener {
    /** Called for every finished main-frame load, including live reloads. May run off the EDT. */
    fun onPageLoaded(url: String)
}
