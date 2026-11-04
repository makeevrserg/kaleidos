package com.makeevrserg.kaleidos.ui.donation.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.makeevrserg.kaleidos.ui.donation.model.DonationOption
import com.makeevrserg.kaleidos.ui.donation.theme.DonationCatalog
import com.makeevrserg.kaleidos.ui.donation.theme.DonationTexts
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

private const val DIALOG_WIDTH = 620
private const val DIALOG_HEIGHT = 480

/**
 * The options in a modal window of the platform.
 *
 * Swing and not Compose, although the tool window that opens it is Compose: a `ComposePanel` reports
 * the size of its composition in physical pixels and ignores the one it is given, so a dialog around
 * one comes out twice too big on a display that scales. The UI DSL sizes itself to its content.
 */
internal class DonationDialog(
    project: Project,
    private val texts: DonationTexts,
    private val catalog: DonationCatalog
) : DialogWrapper(project) {

    /** Copies the address and then says so: the clipboard gives no feedback of its own. */
    private fun createCopyLink(address: String): ActionLink {
        val copyLink = ActionLink(DonationTexts.copyAddress)
        copyLink.addActionListener { _ ->
            CopyPasteManager.getInstance().setContents(StringSelection(address))
            copyLink.text = DonationTexts.addressCopied
        }
        return copyLink
    }

    /** A link is followed, an address is selected or copied; the title of the option labels the row. */
    private fun donationRow(panel: Panel, option: DonationOption) {
        when (option) {
            is DonationOption.Link -> panel.row(option.title) {
                browserLink(option.linkText, option.url)
                    .comment(option.description)
            }
            is DonationOption.Address -> panel.row(option.title) {
                cell(JBLabel(option.address).apply { setCopyable(true) })
                cell(createCopyLink(option.address))
            }
        }
    }

    /** Nothing here is confirmed or cancelled, so the dialog only closes. */
    override fun createActions(): Array<Action> = arrayOf(okAction)

    /**
     * The size the content packs to, scaled with the IDE. Without it the platform asks the content
     * how big it wants to be before it has a width, which a wrapped paragraph cannot answer: the
     * dialog then opens as tall as the screen.
     */
    override fun getInitialSize(): Dimension = JBUI.size(DIALOG_WIDTH, DIALOG_HEIGHT)

    private fun createDonationPanel(): DialogPanel {
        return panel {
            row {
                icon(AllIcons.Ide.Gift)
                label(DonationTexts.heading).applyToComponent { font = JBFont.h4() }
            }
            row {
                text(texts.intro)
            }
            catalog.options.forEach { option -> donationRow(this, option) }
        }
    }

    /**
     * The options are centred instead of filling the dialog: a window manager can hand it far more
     * room than they need, and a block of text pinned to the top left corner of an empty window
     * looks like something failed to load. A [GridBagLayout] with a single child does exactly that
     * and leaves the child at its own size.
     */
    override fun createCenterPanel(): JComponent {
        val centeringPanel = JPanel(GridBagLayout())
        centeringPanel.isOpaque = false
        centeringPanel.add(createDonationPanel())
        return centeringPanel
    }

    init {
        title = DonationTexts.dialogTitle
        setOKButtonText(DonationTexts.closeButton)
        init()
    }
}
