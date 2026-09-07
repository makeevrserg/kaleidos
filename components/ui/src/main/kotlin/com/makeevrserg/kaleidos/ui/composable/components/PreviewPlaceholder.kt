package com.makeevrserg.kaleidos.ui.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makeevrserg.kaleidos.ui.state.PreviewMessage
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.typography

private const val PLACEHOLDER_PADDING = 24
private const val TEXT_MAX_WIDTH = 420
private const val ICON_GAP = 16
private const val TITLE_GAP = 6

private val LOADING_SAMPLE = PreviewMessage(
    title = "Starting the dev server",
    description = "Running kobwebStart in :instances:web-app. Its output is in the Run tool window."
)
private val INFO_SAMPLE = PreviewMessage(
    title = "No previews in Card.kt",
    description = "A preview is a top-level @Composable function without parameters, annotated with @Preview."
)
private val FAILURE_SAMPLE = PreviewMessage(
    title = "The dev server does not serve the preview page",
    description = "It was built without the page, which is what a server that was already running does.\n\n" +
        "Press Restart Dev Server to build and start it again."
)

/** The spinner while a page is on its way, otherwise the standard dialog icon of what the text says. */
@Composable
private fun PlaceholderIcon(kind: PreviewPlaceholderKind) {
    when (kind) {
        PreviewPlaceholderKind.LOADING -> CircularProgressIndicatorBig()
        PreviewPlaceholderKind.INFO -> Icon(key = AllIconsKeys.General.InformationDialog, contentDescription = null)
        PreviewPlaceholderKind.FAILURE -> Icon(key = AllIconsKeys.General.ErrorDialog, contentDescription = null)
    }
}

/**
 * A tool window with no page to show, in the shape the platform uses for an empty or failed state:
 * an icon, one line saying what is going on and the detail under it.
 */
@Composable
internal fun PreviewPlaceholder(
    message: PreviewMessage,
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
            text = message.title,
            style = JewelTheme.typography.h4TextStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH.dp)
        )
        Spacer(modifier = Modifier.height(TITLE_GAP.dp))
        Text(
            text = message.description,
            color = JewelTheme.globalColors.text.info,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH.dp)
        )
    }
}

@Preview
@Composable
internal fun PlaceholderLoadingPreview() {
    PreviewPlaceholder(message = LOADING_SAMPLE, kind = PreviewPlaceholderKind.LOADING)
}

@Preview
@Composable
internal fun PlaceholderInfoPreview() {
    PreviewPlaceholder(message = INFO_SAMPLE, kind = PreviewPlaceholderKind.INFO)
}

@Preview
@Composable
internal fun PlaceholderFailurePreview() {
    PreviewPlaceholder(message = FAILURE_SAMPLE, kind = PreviewPlaceholderKind.FAILURE)
}
