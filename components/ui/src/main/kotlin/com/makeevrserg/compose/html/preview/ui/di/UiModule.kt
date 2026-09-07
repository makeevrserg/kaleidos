package com.makeevrserg.compose.html.preview.ui.di

import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.ui.PreviewMessageTexts
import com.makeevrserg.compose.html.preview.ui.PreviewPanel
import com.makeevrserg.compose.html.preview.ui.PreviewStatusTexts
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowserFactory
import com.makeevrserg.compose.html.preview.ui.state.PreviewUiStateFactory

/**
 * Tool window content is created by the platform on demand, so this module hands out factories
 * instead of instances.
 */
class UiModule(
    private val previewStore: PreviewStore
) {
    val previewBrowserFactory = PreviewBrowserFactory()

    private val previewMessageTexts = PreviewMessageTexts()

    private val previewUiStateFactory = PreviewUiStateFactory(
        statusTexts = PreviewStatusTexts(),
        messageTexts = previewMessageTexts
    )

    /** Must be called on the event dispatch thread. The caller owns [browser]. */
    fun createPreviewPanel(browser: PreviewBrowser): PreviewPanel {
        return PreviewPanel(
            store = previewStore,
            browser = browser,
            messageTexts = previewMessageTexts,
            uiStateFactory = previewUiStateFactory
        )
    }
}
