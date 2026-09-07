package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text

private const val STATUS_PADDING_VERTICAL = 4
private const val STATUS_PADDING_HORIZONTAL = 8
private const val STATUS_ICON_GAP = 6
private const val STATUS_TRANSITION = "preview-status"

/** Footer with the state of the dev server, and a spinner while the page itself is loading. */
@Composable
internal fun PreviewStatusBar(
    text: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = STATUS_PADDING_HORIZONTAL.dp, vertical = STATUS_PADDING_VERTICAL.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(STATUS_ICON_GAP.dp))
            }
        }
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
