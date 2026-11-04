package com.makeevrserg.kaleidos.ui.browser.util

import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent

private const val HTTP_SCHEME = "http"
private const val NO_STATUS = 0
private const val FIRST_ERROR_STATUS = 400

internal fun BrowserPageEvent.isObservedPageEvent(isPageActive: Boolean): Boolean {
    if (!isPageActive && this !is BrowserPageEvent.Navigate) return false
    return when (this) {
        is BrowserPageEvent.Started -> isMain
        is BrowserPageEvent.Failed -> isMain && !isAborted
        is BrowserPageEvent.Finished -> isMain && url.orEmpty().startsWith(HTTP_SCHEME) && status != NO_STATUS
        else -> true
    }
}

internal fun BrowserPageEvent.Completion.toPageFailureOrNull(): PageLoad.Failed? {
    return when (this) {
        is BrowserPageEvent.Failed -> PageLoad.Failed(errorText?.takeIf(String::isNotBlank) ?: errorCode)
        is BrowserPageEvent.Finished -> if (status >= FIRST_ERROR_STATUS) {
            PageLoad.Failed("The dev server answered HTTP $status")
        } else {
            null
        }
    }
}
