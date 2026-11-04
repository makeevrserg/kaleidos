package com.makeevrserg.kaleidos.ui.donation.component

import com.makeevrserg.kaleidos.ui.donation.api.DonationPresenter
import com.makeevrserg.kaleidos.ui.donation.gui.DonationDialog

/** Every call builds a fresh dialog: a [DialogWrapper][com.intellij.openapi.ui.DialogWrapper] is shown once. */
internal class DialogDonationPresenter(
    private val donationDialog: () -> DonationDialog
) : DonationPresenter {

    override fun showDonation() {
        donationDialog.invoke().show()
    }
}
