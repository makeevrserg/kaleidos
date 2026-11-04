package com.makeevrserg.kaleidos.ui.browser.util

import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Registers both native handlers before the first URL, then owns their lifetime and page identification.
 * The hooks run synchronously inside the collection; partial registration removes the first handler.
 * One coroutine owns event ordering, with generations rejecting queued results of cancelled checks.
 * Native title callbacks carry no navigation ID, so the caller owns one collection per browser.
 */
internal fun Flow<String?>.observeBrowserPages(
    registerLoadHandler: (SendChannel<BrowserPageEvent>) -> AutoCloseable,
    registerTitleHandler: (SendChannel<BrowserPageEvent>) -> AutoCloseable,
    navigate: (String?) -> Unit,
    identificationTimeout: Duration
): Flow<PageLoad> {
    return callbackFlow {
        val events = Channel<BrowserPageEvent>(Channel.UNLIMITED)
        val loadRegistration = try {
            registerLoadHandler.invoke(events)
        } catch (exception: Exception) {
            events.close()
            throw exception
        }
        val titleRegistration = try {
            registerTitleHandler.invoke(events)
        } catch (exception: Exception) {
            events.close()
            loadRegistration.close()
            throw exception
        }
        launch {
            observePageEvents(
                events,
                navigate,
                identificationTimeout
            )
        }
        launch {
            this@observeBrowserPages.collect { url -> events.trySend(BrowserPageEvent.Navigate(url)) }
        }
        awaitClose {
            events.close()
            try {
                titleRegistration.close()
            } finally {
                loadRegistration.close()
            }
        }
    }.buffer(Channel.UNLIMITED)
}
