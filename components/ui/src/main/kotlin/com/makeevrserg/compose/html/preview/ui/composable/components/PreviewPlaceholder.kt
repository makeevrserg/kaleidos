package com.makeevrserg.compose.html.preview.ui.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Text

private const val PLACEHOLDER_PADDING = 16
private const val SPINNER_GAP = 12
private const val LOADING_SAMPLE = "Starting kobwebStart in :instances:web-app…"
private const val MESSAGE_SAMPLE = "Open a Kotlin file with @Preview functions"

/** Centred text of a tool window with no page to show, with a spinner while one is on its way. */
@Composable
internal fun PreviewPlaceholder(
    text: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(PLACEHOLDER_PADDING.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicatorBig()
            Spacer(modifier = Modifier.height(SPINNER_GAP.dp))
        }
        Text(text = text, textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
internal fun PlaceholderLoadingPreview() {
    PreviewPlaceholder(text = LOADING_SAMPLE, isLoading = true)
}

@Preview
@Composable
internal fun PlaceholderMessagePreview() {
    PreviewPlaceholder(text = MESSAGE_SAMPLE, isLoading = false)
}
