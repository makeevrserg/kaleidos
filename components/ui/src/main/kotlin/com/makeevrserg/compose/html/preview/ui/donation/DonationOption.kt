package com.makeevrserg.compose.html.preview.ui.donation

/** One way of supporting the author. */
sealed interface DonationOption {
    val title: String

    /**
     * Followed in the system browser.
     *
     * @param linkText what the link reads, usually the address without its scheme
     */
    data class Link(
        override val title: String,
        val description: String,
        val linkText: String,
        val url: String
    ) : DonationOption

    /** A wallet: there is nothing to open, so the address is shown and copied to the clipboard. */
    data class Address(
        override val title: String,
        val address: String
    ) : DonationOption
}
