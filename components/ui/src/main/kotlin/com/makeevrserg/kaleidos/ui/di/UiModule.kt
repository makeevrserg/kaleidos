package com.makeevrserg.kaleidos.ui.di

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.makeevrserg.kaleidos.core.coroutine.api.CoroutineFeature
import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import com.makeevrserg.kaleidos.server.api.DevServerLog
import com.makeevrserg.kaleidos.ui.action.gui.OpenDevToolsAction
import com.makeevrserg.kaleidos.ui.action.gui.OpenInBrowserAction
import com.makeevrserg.kaleidos.ui.action.gui.RefreshPreviewAction
import com.makeevrserg.kaleidos.ui.action.gui.RestartDevServerAction
import com.makeevrserg.kaleidos.ui.action.gui.ShowPreviewAction
import com.makeevrserg.kaleidos.ui.action.gui.StopDevServerAction
import com.makeevrserg.kaleidos.ui.action.gui.ToggleDevServerConsoleAction
import com.makeevrserg.kaleidos.ui.browser.api.PreviewBrowser
import com.makeevrserg.kaleidos.ui.browser.component.JcefPreviewBrowser
import com.makeevrserg.kaleidos.ui.browser.component.UnsupportedPreviewBrowser
import com.makeevrserg.kaleidos.ui.component.PreviewPanel
import com.makeevrserg.kaleidos.ui.console.component.DevServerConsole
import com.makeevrserg.kaleidos.ui.console.component.PreviewConsoleSplitter
import com.makeevrserg.kaleidos.ui.console.theme.DevServerConsoleTexts
import com.makeevrserg.kaleidos.ui.donation.api.DonationPresenter
import com.makeevrserg.kaleidos.ui.donation.component.DialogDonationPresenter
import com.makeevrserg.kaleidos.ui.donation.gui.DonationDialog
import com.makeevrserg.kaleidos.ui.donation.theme.DonationCatalog
import com.makeevrserg.kaleidos.ui.donation.theme.DonationTexts
import com.makeevrserg.kaleidos.ui.state.util.buildUiState
import com.makeevrserg.kaleidos.ui.theme.PreviewMessageTexts
import com.makeevrserg.kaleidos.ui.theme.PreviewStatusTexts
import javax.swing.JComponent

/**
 * Tool window content is created by the platform on demand, so this module hands out factories
 * instead of instances.
 */
class UiModule(
    private val previewStore: PreviewStore,
    private val project: Project,
    private val devServerLog: DevServerLog,
    private val coroutineFeature: CoroutineFeature
) {
    val previewBrowser: PreviewBrowser = if (JBCefApp.isSupported()) {
        JcefPreviewBrowser(JBCefBrowser.createBuilder().build())
    } else {
        UnsupportedPreviewBrowser()
    }

    val isJcefSupported: Boolean
        get() = JBCefApp.isSupported()

    val showPreviewAction: (PreviewFunction) -> AnAction = { previewFunction ->
        ShowPreviewAction(previewStore, previewFunction)
    }

    private val previewMessageTexts = PreviewMessageTexts()

    private val previewStatusTexts = PreviewStatusTexts()

    private val donationTexts = DonationTexts()

    private val devServerConsoleTexts = DevServerConsoleTexts()

    private val donationPresenter: DonationPresenter = DialogDonationPresenter {
        DonationDialog(
            project = project,
            texts = donationTexts,
            catalog = DonationCatalog()
        )
    }

    fun createDevServerConsole(): DevServerConsole {
        return DevServerConsole(
            project = project,
            log = devServerLog,
            texts = devServerConsoleTexts,
            coroutineScope = coroutineFeature
        )
    }

    fun createPreviewConsoleSplitter(page: JComponent, console: JComponent): PreviewConsoleSplitter {
        return PreviewConsoleSplitter(page = page, console = console)
    }

    fun createTitleActions(panel: PreviewPanel, splitter: PreviewConsoleSplitter): List<AnAction> {
        return listOf(
            RefreshPreviewAction(previewStore, panel),
            OpenInBrowserAction(previewStore),
            ToggleDevServerConsoleAction(splitter)
        )
    }

    fun createGearActions(panel: PreviewPanel): DefaultActionGroup {
        return DefaultActionGroup(
            RestartDevServerAction(previewStore),
            StopDevServerAction(previewStore),
            OpenDevToolsAction(panel, isJcefSupported)
        )
    }

    /** Must be called on the event dispatch thread. The caller owns [browser]. */
    fun createPreviewPanel(browser: PreviewBrowser): PreviewPanel {
        return PreviewPanel(
            store = previewStore,
            browser = browser,
            messageTexts = previewMessageTexts,
            buildUiState = { state -> state.buildUiState(previewStatusTexts, previewMessageTexts) },
            donationPresenter = donationPresenter
        )
    }
}
