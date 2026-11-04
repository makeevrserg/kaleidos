package com.makeevrserg.kaleidos.ui.browser.fake

import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent
import kotlinx.coroutines.channels.SendChannel

/** Scripts only the native boundary; production code owns observation, navigation ordering and cleanup. */
internal class FakeBrowserPageRegistration {
    val calls = mutableListOf<String>()

    val navigatedUrls = mutableListOf<String?>()

    var navigationEvents: List<BrowserPageEvent> = emptyList()

    var titleRegistrationFailure: Exception? = null

    var loadRegistrationFailure: Exception? = null

    var navigationFailure: Exception? = null

    var events: SendChannel<BrowserPageEvent>? = null
        private set

    fun registerLoadHandler(destination: SendChannel<BrowserPageEvent>): AutoCloseable {
        loadRegistrationFailure?.let { failure -> throw failure }
        calls.add("register-load")
        events = destination
        return AutoCloseable { calls.add("remove-load") }
    }

    fun registerTitleHandler(destination: SendChannel<BrowserPageEvent>): AutoCloseable {
        titleRegistrationFailure?.let { failure -> throw failure }
        calls.add("register-title")
        check(destination === events)
        return AutoCloseable { calls.add("remove-title") }
    }

    fun navigate(url: String?) {
        navigationFailure?.let { failure -> throw failure }
        calls.add("navigate")
        navigatedUrls.add(url)
        navigationEvents.forEach(::emit)
    }

    fun emit(event: BrowserPageEvent) {
        checkNotNull(events).trySend(event).getOrThrow()
    }
}
