package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.animation.Crossfade
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
private const val STATUS_TRANSITION = "preview-status"
private const val STARTING_SAMPLE = "Starting kobwebStart in :instances:web-app, waiting for the server…"
private const val URL_SAMPLE = "http://localhost:8080/compose-html-preview?preview=app.CardPreview"

/** Footer with the state of the dev server. What is being waited for is said by the placeholder above it. */
@Composable
internal fun PreviewStatusBar(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = STATUS_PADDING_HORIZONTAL.dp, vertical = STATUS_PADDING_VERTICAL.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(targetState = text, label = STATUS_TRANSITION) { current ->
            Text(
                text = current,
                color = JewelTheme.globalColors.text.info,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
internal fun StatusBarStartingPreview() {
    PreviewStatusBar(text = STARTING_SAMPLE)
}

@Preview
@Composable
internal fun StatusBarPagePreview() {
    PreviewStatusBar(text = URL_SAMPLE)
}
