package com.makeevrserg.compose.html.preview.ui.di

import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.ui.PreviewPanel
import com.makeevrserg.compose.html.preview.ui.PreviewStateTexts
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowserFactory

/**
 * Tool window content is created by the platform on demand, so this module hands out factories
 * instead of instances.
 */
class UiModule(
    private val coreModule: CoreModule,
    private val previewStore: PreviewStore
) {
    val previewBrowserFactory = PreviewBrowserFactory()

    private val previewStateTexts = PreviewStateTexts()

    /** Must be called on the event dispatch thread. The caller owns [browser] and [coroutineFeature]. */
    fun createPreviewPanel(browser: PreviewBrowser, coroutineFeature: CoroutineFeature): PreviewPanel {
        return PreviewPanel(
            contract = previewStore,
            browser = browser,
            texts = previewStateTexts,
            coroutineFeature = coroutineFeature
        )
    }

    fun createPanelCoroutineFeature(): CoroutineFeature = coreModule.createMainCoroutineFeature()
}
