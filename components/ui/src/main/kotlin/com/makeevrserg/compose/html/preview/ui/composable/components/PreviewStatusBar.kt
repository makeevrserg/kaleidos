package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

private const val STATUS_PADDING_VERTICAL = 4
private const val STATUS_PADDING_HORIZONTAL = 8
private const val STATUS_GAP = 8
private const val STATUS_WEIGHT = 1f
private const val STATUS_TRANSITION = "preview-status"
private const val DONATION_SAMPLE = "Support the author"
private const val STARTING_SAMPLE = "Starting kobwebStart in :instances:web-app, waiting for the server…"
private const val URL_SAMPLE = "http://localhost:8080/compose-html-preview?preview=app.CardPreview"

/**
 * Footer with the state of the dev server, and at its trailing edge the link to the donation dialog.
 * What is being waited for is said by the placeholder above it.
 *
 * The state gives up its width first: it is a sentence that reads well truncated, while the link
 * either says what it does or says nothing at all.
 */
@Composable
internal fun PreviewStatusBar(
    text: String,
    donationText: String,
    onDonateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = STATUS_PADDING_HORIZONTAL.dp, vertical = STATUS_PADDING_VERTICAL.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(STATUS_GAP.dp)
    ) {
        Crossfade(
            targetState = text,
            modifier = Modifier.weight(STATUS_WEIGHT),
            label = STATUS_TRANSITION
        ) { current ->
            Text(
                text = current,
                color = JewelTheme.globalColors.text.info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DonationLink(
            text = donationText,
            onClick = onDonateClick
        )
    }
}

@Preview
@Composable
internal fun StatusBarStartingPreview() {
    PreviewStatusBar(
        text = STARTING_SAMPLE,
        donationText = DONATION_SAMPLE,
        onDonateClick = {}
    )
}

@Preview
@Composable
internal fun StatusBarPagePreview() {
    PreviewStatusBar(
        text = URL_SAMPLE,
        donationText = DONATION_SAMPLE,
        onDonateClick = {}
    )
}
