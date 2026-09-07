package com.makeevrserg.kaleidos.ui.donation

/**
 * Where a donation can go, in the order the dialog offers it: the ways that cost nothing to arrange
 * first, the wallets after them.
 */
class DonationCatalog {

    val options: List<DonationOption> = listOf(
        DonationOption.Link(
            title = "Telegram",
            description = "Write to the author and arrange it any way you like.",
            linkText = "@makeevrserg",
            url = "https://t.me/makeevrserg"
        ),
        DonationOption.Link(
            title = "Boosty",
            description = "One-off or monthly, by card.",
            linkText = "boosty.to/empireprojekt",
            url = "https://boosty.to/empireprojekt/donate"
        ),
        DonationOption.Link(
            title = "GitHub",
            description = "A star costs nothing and helps others find the plugin.",
            linkText = "Star the repository",
            url = "https://github.com/makeevrserg/kaleidos"
        ),
        DonationOption.Address(
            title = "Bitcoin",
            address = "bc1q9a8dr55jgfae0mhevw3vvczegjv0khfp0ngrnv"
        ),
        DonationOption.Address(
            title = "Ethereum",
            address = "0x0BaAeEA44Ce08c8DC139224ff57563695B30d423"
        )
    )
}
