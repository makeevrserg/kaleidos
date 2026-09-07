package com.makeevrserg.compose.html.preview.ui.donation

import com.intellij.openapi.project.Project

/** Every call builds a fresh dialog: a [DialogWrapper][com.intellij.openapi.ui.DialogWrapper] is shown once. */
class DialogDonationPresenter(
    private val project: Project,
    private val texts: DonationTexts,
    private val catalog: DonationCatalog
) : DonationPresenter {

    override fun showDonation() {
        DonationDialog(
            project = project,
            texts = texts,
            catalog = catalog
        ).show()
    }
}
