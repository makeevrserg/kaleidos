package com.makeevrserg.kaleidos.ui.browser.model

import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad

/** Immutable observations copied from native callbacks, and commands owned by one page collection. */
internal sealed interface BrowserPageEvent {
    data class Navigate(val url: String?) : BrowserPageEvent

    data class Started(val isMain: Boolean) : BrowserPageEvent

    data class TitleChanged(val title: String?) : BrowserPageEvent

    sealed interface Completion : BrowserPageEvent

    data class Finished(val isMain: Boolean, val url: String?, val status: Int) : Completion

    data class Failed(
        val isMain: Boolean,
        val isAborted: Boolean,
        val errorText: String?,
        val errorCode: String
    ) : Completion

    data class Identified(val generation: Long, val result: PageLoad) : BrowserPageEvent
}
