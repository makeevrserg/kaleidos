package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private const val ICON_GAP = 4
private const val LINK_SAMPLE = "Support the author"

/**
 * The nudge in the footer of the tool window: the gift icon of the platform and the words that open
 * the dialog. It shares the strip with the state of the dev server and must not compete with it, so
 * it is a plain link and never a button or a banner.
 */
@Composable
internal fun DonationLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ICON_GAP.dp)
    ) {
        Icon(key = AllIconsKeys.Ide.Gift, contentDescription = null)
        Link(text = text, onClick = onClick)
    }
}

@Preview
@Composable
internal fun DonationLinkPreview() {
    DonationLink(text = LINK_SAMPLE, onClick = {})
}
