package com.makeevrserg.kaleidos.ui

import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.ui.browser.PreviewBrowser
import com.makeevrserg.kaleidos.ui.composable.PreviewToolWindowContent
import com.makeevrserg.kaleidos.ui.donation.DonationPresenter
import com.makeevrserg.kaleidos.ui.donation.DonationTexts
import com.makeevrserg.kaleidos.ui.state.PreviewUiStateFactory
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
    private val uiStateFactory: PreviewUiStateFactory,
    private val donationTexts: DonationTexts,
    private val donationPresenter: DonationPresenter
) {

    val component: JComponent = JewelComposePanel {
        PreviewToolWindowContent(
            store = store,
            browser = browser,
            messageTexts = messageTexts,
            uiStateFactory = uiStateFactory,
            donationText = donationTexts.footerLink,
            onDonateClick = donationPresenter::showDonation
        )
    }

    fun reload() = browser.reload()

    fun openDevTools() = browser.openDevTools()
}
