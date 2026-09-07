package com.makeevrserg.kaleidos.ui.di

import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.ui.PreviewMessageTexts
import com.makeevrserg.kaleidos.ui.PreviewPanel
import com.makeevrserg.kaleidos.ui.PreviewStatusTexts
import com.makeevrserg.kaleidos.ui.browser.PreviewBrowser
import com.makeevrserg.kaleidos.ui.browser.PreviewBrowserFactory
import com.makeevrserg.kaleidos.ui.donation.DialogDonationPresenter
import com.makeevrserg.kaleidos.ui.donation.DonationCatalog
import com.makeevrserg.kaleidos.ui.donation.DonationPresenter
import com.makeevrserg.kaleidos.ui.donation.DonationTexts
import com.makeevrserg.kaleidos.ui.state.PreviewUiStateFactory

/**
 * Tool window content is created by the platform on demand, so this module hands out factories
 * instead of instances.
 */
class UiModule(
    private val previewStore: PreviewStore,
    project: Project
) {
    val previewBrowserFactory = PreviewBrowserFactory()

    private val previewMessageTexts = PreviewMessageTexts()

    private val previewUiStateFactory = PreviewUiStateFactory(
        statusTexts = PreviewStatusTexts(),
        messageTexts = previewMessageTexts
    )

    private val donationTexts = DonationTexts()

    private val donationPresenter: DonationPresenter = DialogDonationPresenter(
        project = project,
        texts = donationTexts,
        catalog = DonationCatalog()
    )

    /** Must be called on the event dispatch thread. The caller owns [browser]. */
    fun createPreviewPanel(browser: PreviewBrowser): PreviewPanel {
        return PreviewPanel(
            store = previewStore,
            browser = browser,
            messageTexts = previewMessageTexts,
            uiStateFactory = previewUiStateFactory,
            donationTexts = donationTexts,
            donationPresenter = donationPresenter
        )
    }
}
