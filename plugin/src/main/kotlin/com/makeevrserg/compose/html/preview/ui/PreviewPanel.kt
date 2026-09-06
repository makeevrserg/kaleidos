package com.makeevrserg.compose.html.preview.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowser
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Content of the tool window. Shows the previews of the selected editor file: a spinner while the
 * preview module is looked up, its dev server starts or the page loads, the page once it answers, a
 * banner while sources are newer than the page, and the server status in a footer. Switching to a
 * file served by another module drops the old page at once instead of leaving it on screen until the
 * new server is up. Nothing is loaded while the tool window is hidden. Event dispatch thread only.
 */
class PreviewPanel(
    private val contract: PreviewStore,
    private val browser: PreviewBrowser,
    private val texts: PreviewStateTexts,
    coroutineFeature: CoroutineFeature
) : CoroutineFeature by coroutineFeature {

    private val sourceBanner = JBLabel().apply {
        border = JBUI.Borders.empty(BANNER_PADDING_VERTICAL, BANNER_PADDING_HORIZONTAL)
        isOpaque = true
        isVisible = false
    }

    private val statusLabel = JBLabel().apply {
        border = JBUI.Borders.empty(STATUS_PADDING_VERTICAL, STATUS_PADDING_HORIZONTAL)
    }

    private val messageLabel = JBLabel("", SwingConstants.CENTER).apply {
        border = JBUI.Borders.empty(MESSAGE_PADDING)
    }

    private val loadingLabel = JBLabel("", AnimatedIcon.Big.INSTANCE, SwingConstants.CENTER).apply {
        verticalTextPosition = SwingConstants.BOTTOM
        horizontalTextPosition = SwingConstants.CENTER
        iconTextGap = LOADING_ICON_TEXT_GAP
        border = JBUI.Borders.empty(MESSAGE_PADDING)
    }

    private val cardLayout = CardLayout()

    private val cards = JPanel(cardLayout).apply {
        add(browser.component, BROWSER_CARD)
        add(messageLabel, MESSAGE_CARD)
        add(loadingLabel, LOADING_CARD)
    }

    private var loadedUrl: String? = null

    /** Number of finished page loads of [loadedUrl]; anything after the first one is a reload. */
    private var pageLoads = 0

    /** True between a `load` call and the end of that load; the footer shows a spinner meanwhile. */
    private var isPageLoading = false

    /** Footer text of the current server state, restored once a page load finishes. */
    private var serverStatusText = ""

    val component: JComponent = BorderLayoutPanel()
        .addToTop(sourceBanner)
        .addToCenter(cards)
        .addToBottom(statusLabel)

    init {
        browser.addPageLoadListener(::onPageLoaded)
        contract.state
            .onEach(::render)
            .launchIn(this)
    }

    private fun renderStatus() {
        statusLabel.text = if (isPageLoading) "Loading preview…" else serverStatusText
        statusLabel.icon = if (isPageLoading) AnimatedIcon.Default.INSTANCE else null
    }

    /** JCEF reports loads off the EDT; the counters and the footer are only touched on it. */
    private fun onPageLoaded(url: String) {
        if (!url.startsWith("http")) return
        launch {
            pageLoads++
            isPageLoading = false
            contract.onPageLoaded(isReload = pageLoads > 1)
            renderStatus()
        }
    }

    private fun renderBanner(sourceState: SourceState) {
        when (sourceState) {
            SourceState.UpToDate -> sourceBanner.isVisible = false
            is SourceState.Changed -> {
                sourceBanner.text = texts.changedBanner(sourceState)
                sourceBanner.icon = AnimatedIcon.Default.INSTANCE
                sourceBanner.background = CHANGED_BACKGROUND
                sourceBanner.isVisible = true
            }
            is SourceState.Reloaded -> {
                sourceBanner.text = texts.reloadedBanner(sourceState)
                sourceBanner.icon = AllIcons.General.InspectionsOK
                sourceBanner.background = RELOADED_BACKGROUND
                sourceBanner.isVisible = true
            }
        }
    }

    /** A stale page must not survive a switch to a file that gets its own page later. */
    private fun dropPage() {
        if (loadedUrl == null) return
        loadedUrl = null
        isPageLoading = false
        browser.load(BLANK_PAGE)
    }

    private fun renderMessage(message: String) {
        messageLabel.text = message
        serverStatusText = ""
        cardLayout.show(cards, MESSAGE_CARD)
        dropPage()
        renderStatus()
    }

    private fun renderLoading(state: PreviewState) {
        loadingLabel.text = texts.loading(state)
        serverStatusText = texts.status(state.serverState, previewUrl = null)
        cardLayout.show(cards, LOADING_CARD)
        dropPage()
        renderStatus()
    }

    /**
     * The page is (re)loaded on every URL change, and a restarted server gets a fresh load because the
     * previous URL is dropped while the server is away.
     */
    private fun renderPage(state: PreviewState, previewUrl: String) {
        serverStatusText = texts.status(state.serverState, previewUrl)
        cardLayout.show(cards, BROWSER_CARD)
        if (previewUrl != loadedUrl) {
            loadedUrl = previewUrl
            pageLoads = 0
            isPageLoading = true
            browser.load(previewUrl)
        }
        renderStatus()
    }

    private fun render(state: PreviewState) {
        renderBanner(state.sourceState)
        val message = texts.message(state)
        val previewUrl = state.previewUrl
        when {
            message != null -> renderMessage(message)
            !state.isToolWindowVisible -> Unit
            previewUrl == null -> renderLoading(state)
            else -> renderPage(state, previewUrl)
        }
    }

    fun reload() = browser.reload()

    fun openDevTools() = browser.openDevTools()

    private companion object {
        const val BROWSER_CARD = "browser"
        const val MESSAGE_CARD = "message"
        const val LOADING_CARD = "loading"
        const val BLANK_PAGE = "about:blank"
        const val STATUS_PADDING_VERTICAL = 4
        const val STATUS_PADDING_HORIZONTAL = 8
        const val BANNER_PADDING_VERTICAL = 6
        const val BANNER_PADDING_HORIZONTAL = 8
        const val MESSAGE_PADDING = 16
        const val LOADING_ICON_TEXT_GAP = 12
        val CHANGED_BACKGROUND = JBColor(0xFFF4CE, 0x5A4D2B)
        val RELOADED_BACKGROUND = JBColor(0xE6F4EA, 0x2E4B31)
    }
}
