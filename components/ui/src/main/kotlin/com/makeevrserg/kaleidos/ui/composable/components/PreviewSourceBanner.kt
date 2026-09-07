package com.makeevrserg.kaleidos.ui.composable.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.makeevrserg.kaleidos.ui.state.PreviewBanner
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultSuccessBanner
import org.jetbrains.jewel.ui.component.DefaultWarningBanner
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private const val BANNER_TRANSITION = "preview-source-banner"
private const val REBUILDING_SAMPLE = "Sources changed (CardPreview.kt, 10:15:30). Rebuilding…"
private const val RELOADED_SAMPLE = "Reloaded with fresh sources at 10:15:34"

/**
 * Freshness of the page below it. A null [banner] is an empty strip of zero height, so the banner
 * grows and shrinks with the animated size of the switch instead of making the page jump.
 */
@Composable
internal fun PreviewSourceBanner(banner: PreviewBanner?) {
    AnimatedContent(
        targetState = banner,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = BANNER_TRANSITION
    ) { current ->
        when (current) {
            null -> Box(modifier = Modifier.fillMaxWidth())
            is PreviewBanner.Rebuilding -> DefaultWarningBanner(
                text = current.text,
                modifier = Modifier.fillMaxWidth(),
                icon = { CircularProgressIndicator() }
            )
            is PreviewBanner.Reloaded -> DefaultSuccessBanner(
                text = current.text,
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(key = AllIconsKeys.General.InspectionsOK, contentDescription = null) }
            )
        }
    }
}

@Preview
@Composable
internal fun SourceBannerRebuildingPreview() {
    PreviewSourceBanner(banner = PreviewBanner.Rebuilding(REBUILDING_SAMPLE))
}

@Preview
@Composable
internal fun SourceBannerReloadedPreview() {
    PreviewSourceBanner(banner = PreviewBanner.Reloaded(RELOADED_SAMPLE))
}
