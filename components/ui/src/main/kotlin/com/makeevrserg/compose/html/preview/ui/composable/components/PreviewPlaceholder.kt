package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private const val PLACEHOLDER_PADDING = 16
private const val ICON_GAP = 12
private const val LOADING_SAMPLE = "Starting kobwebStart in :instances:web-app…"
private const val INFO_SAMPLE = "No @Preview functions in Card.kt"
private const val FAILURE_SAMPLE = "The preview page could not be rendered.\nERR_CONNECTION_REFUSED"

/** A spinner while a page is on its way, otherwise the icon of what the text below it says. */
@Composable
private fun PlaceholderIcon(kind: PreviewPlaceholderKind) {
    when (kind) {
        PreviewPlaceholderKind.LOADING -> CircularProgressIndicatorBig()
        PreviewPlaceholderKind.INFO -> Icon(key = AllIconsKeys.General.Information, contentDescription = null)
        PreviewPlaceholderKind.FAILURE -> Icon(key = AllIconsKeys.General.Error, contentDescription = null)
    }
}

@Composable
private fun placeholderTextColor(kind: PreviewPlaceholderKind): Color {
    return when (kind) {
        PreviewPlaceholderKind.LOADING -> Color.Unspecified
        PreviewPlaceholderKind.INFO -> Color.Unspecified
        PreviewPlaceholderKind.FAILURE -> JewelTheme.globalColors.text.error
    }
}

/** Centred text of a tool window with no page to show, under the icon that says why there is none. */
@Composable
internal fun PreviewPlaceholder(
    text: String,
    kind: PreviewPlaceholderKind,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PLACEHOLDER_PADDING.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaceholderIcon(kind = kind)
        Spacer(modifier = Modifier.height(ICON_GAP.dp))
        Text(
            text = text,
            color = placeholderTextColor(kind),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
internal fun PlaceholderLoadingPreview() {
    PreviewPlaceholder(text = LOADING_SAMPLE, kind = PreviewPlaceholderKind.LOADING)
}

@Preview
@Composable
internal fun PlaceholderInfoPreview() {
    PreviewPlaceholder(text = INFO_SAMPLE, kind = PreviewPlaceholderKind.INFO)
}

@Preview
@Composable
internal fun PlaceholderFailurePreview() {
    PreviewPlaceholder(text = FAILURE_SAMPLE, kind = PreviewPlaceholderKind.FAILURE)
}
