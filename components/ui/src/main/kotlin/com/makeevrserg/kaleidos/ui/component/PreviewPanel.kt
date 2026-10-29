package com.makeevrserg.kaleidos.ui.component

import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.model.PreviewState
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import com.makeevrserg.kaleidos.ui.composable.component.PreviewToolWindowContent
import com.makeevrserg.kaleidos.ui.donation.api.DonationPresenter
import com.makeevrserg.kaleidos.ui.donation.theme.DonationTexts
import com.makeevrserg.kaleidos.ui.state.model.PreviewUiState
import com.makeevrserg.kaleidos.ui.theme.PreviewMessageTexts
import org.jetbrains.jewel.bridge.JewelComposePanel
import javax.swing.JComponent

/**
 * Swing side of the tool window: one Compose panel that follows the IDE theme through the Jewel
 * bridge. Must be created on the event dispatch thread, and so must the [browser] it renders.
 */
class PreviewPanel(
    private val store: PreviewStore,
    private val browser: PreviewBrowser,
    private val messageTexts: PreviewMessageTexts,
    private val buildUiState: (PreviewState) -> PreviewUiState,
    private val donationPresenter: DonationPresenter
) {

    val component: JComponent = JewelComposePanel {
        PreviewToolWindowContent(
            store = store,
            browser = browser,
            messageTexts = messageTexts,
            buildUiState = buildUiState,
            donationText = DonationTexts.footerLink,
            onDonateClick = donationPresenter::showDonation
        )
    }

    fun reload() = browser.reload()

    fun openDevTools() = browser.openDevTools()
}
