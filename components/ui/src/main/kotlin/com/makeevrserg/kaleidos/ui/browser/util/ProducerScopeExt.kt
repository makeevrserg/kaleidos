package com.makeevrserg.kaleidos.ui.browser.util

import com.makeevrserg.kaleidos.harness.api.util.TITLE
import com.makeevrserg.kaleidos.preview.api.feature.model.PageLoad
import com.makeevrserg.kaleidos.ui.browser.model.BrowserPageEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

private fun ProducerScope<PageLoad>.identifyPage(
    title: MutableStateFlow<String?>,
    generation: Long,
    events: Channel<BrowserPageEvent>,
    timeout: Duration
): Job {
    return launch(start = CoroutineStart.UNDISPATCHED) {
        val identified = withTimeoutOrNull(timeout) { title.first { current -> current == TITLE } }
        val result = if (identified == null) PageLoad.Foreign else PageLoad.Succeeded
        events.send(BrowserPageEvent.Identified(generation, result))
    }
}

internal suspend fun ProducerScope<PageLoad>.observePageEvents(
    events: Channel<BrowserPageEvent>,
    navigate: (String?) -> Unit,
    identificationTimeout: Duration
) {
    val title = MutableStateFlow<String?>(null)
    var identification: Job? = null
    var generation = 0L
    var isPageActive = false
    events.consumeEach { event ->
        if (!event.isObservedPageEvent(isPageActive)) return@consumeEach
        if (event !is BrowserPageEvent.TitleChanged && event !is BrowserPageEvent.Identified) {
            identification?.cancel()
            generation++
        }
        when (event) {
            is BrowserPageEvent.TitleChanged -> title.value = event.title
            is BrowserPageEvent.Identified -> if (event.generation == generation) send(event.result)
            is BrowserPageEvent.Navigate -> {
                isPageActive = event.url != null
                title.value = null
                navigate.invoke(event.url)
            }
            is BrowserPageEvent.Started -> title.value = null
            is BrowserPageEvent.Completion -> {
                val failure = event.toPageFailureOrNull()
                if (failure != null) {
                    send(failure)
                } else {
                    identification = identifyPage(
                        title,
                        generation,
                        events,
                        identificationTimeout
                    )
                }
            }
        }
    }
}
