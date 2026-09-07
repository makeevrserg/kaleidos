package com.makeevrserg.kaleidos.ui.donation

/**
 * Where a donation can go, in the order the dialog offers it: the ways that cost nothing to arrange
 * first, the wallets after them.
 *
 * The same options the README of the repository lists, and they have to stay in step with it.
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
            title = "USDT (Polygon)",
            address = "0x3955abc6f5396e57b11a05b96f988bc60708c9b0"
        ),
        DonationOption.Address(
            title = "USDT (TRC20)",
            address = "TLYf28vZeuuHcEJMHSZtuYEzQ2DjNvNE3W"
        ),
        DonationOption.Address(
            title = "USDT (Solana)",
            address = "6sYK6Nss8cjLeeTp6u3t63JG4f1hFf8sDQ7nVtMNoyps"
        ),
        DonationOption.Address(
            title = "TON",
            address = "UQDfywxsnHI1ko_uqBYKED3RoMzoVm3mnxuS_-JVQc4mSSJt"
        )
    )
}
