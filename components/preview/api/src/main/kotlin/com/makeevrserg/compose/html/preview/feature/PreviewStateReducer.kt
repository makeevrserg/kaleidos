package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.url.PreviewUrlFactory
import java.time.Clock

/**
 * Pure transitions of [PreviewState]. Kept apart from [PreviewFeature] so the feature only
 * orchestrates side effects: tool window, dev server, notifications.
 */
class PreviewStateReducer(
    private val clock: Clock,
    private val previewUrlFactory: PreviewUrlFactory
) {

    /** The page exists only once the server of the target's own module answers; another module's server is not it. */
    private fun PreviewState.derivedUrl(): String? {
        val target = target ?: return null
        val host = (target.host as? PreviewHostResolution.Found)?.host ?: return null
        val running = serverState as? DevServerState.Running ?: return null
        if (running.host != host) return null
        val previews = target.renderablePreviews
        if (previews.isEmpty()) return null
        return previewUrlFactory.create(running.baseUrl, host.kind, previews, target.focusedFqn)
    }

    /**
     * Every URL is loaded from scratch, so a page that was on screen for the previous address must not
     * count as the page of the new one: nothing is shown until the browser reports the new load.
     */
    private fun PreviewState.withDerivedUrl(): PreviewState {
        val derivedUrl = derivedUrl()
        if (derivedUrl == previewUrl) return this
        return copy(previewUrl = derivedUrl, pageState = PageState.Loading)
    }

    /**
     * Switching to another file starts with a fresh freshness state and an unknown host; rescans of
     * the same file keep both.
     */
    fun select(state: PreviewState, target: PreviewTarget?): PreviewState {
        val isSameFile = target != null && target.filePath == state.target?.filePath
        return state.copy(
            target = if (isSameFile) target.copy(host = state.target?.host) else target,
            sourceState = if (isSameFile) state.sourceState else SourceState.UpToDate
        ).withDerivedUrl()
    }

    /**
     * The gutter icon of a file that is not the current target yet, for example right after a click
     * before the editor selection event arrived, gets a temporary target with that single preview.
     */
    fun focus(state: PreviewState, previewFunction: PreviewFunction): PreviewState {
        val current = state.target
        val target = if (current != null && current.filePath == previewFunction.filePath) {
            current.copy(focusedFqn = previewFunction.fqn)
        } else {
            PreviewTarget(
                filePath = previewFunction.filePath,
                fileName = previewFunction.fileName,
                scan = PreviewScan.Renderable(listOf(previewFunction)),
                focusedFqn = previewFunction.fqn,
                host = null
            )
        }
        return state.copy(target = target).withDerivedUrl()
    }

    /** Ignored when the target moved to another file while the lookup was running. */
    fun attachHost(state: PreviewState, filePath: String, resolution: PreviewHostResolution): PreviewState {
        val target = state.target?.takeIf { current -> current.filePath == filePath } ?: return state
        return state.copy(target = target.copy(host = resolution)).withDerivedUrl()
    }

    fun setToolWindowVisible(state: PreviewState, isVisible: Boolean): PreviewState {
        return state.copy(isToolWindowVisible = isVisible)
    }

    fun setServerState(state: PreviewState, serverState: DevServerState): PreviewState {
        return state.copy(serverState = serverState).withDerivedUrl()
    }

    fun markChanged(state: PreviewState, changedFileName: String): PreviewState {
        if (state.sourceState is SourceState.Changed) return state
        return state.copy(sourceState = SourceState.Changed(changedFileName, clock.instant()))
    }

    /**
     * The first load of a page says nothing about freshness; a later one does, whether it follows an
     * edit in the IDE or a change made on disk that the IDE never reported.
     */
    fun markPageShown(state: PreviewState): PreviewState {
        if (state.previewUrl == null) return state
        val isReload = state.pageState is PageState.Shown
        val isFresh = state.sourceState is SourceState.UpToDate && !isReload
        return state.copy(
            pageState = PageState.Shown,
            sourceState = if (isFresh) state.sourceState else SourceState.Reloaded(clock.instant())
        )
    }

    fun markPageFailed(state: PreviewState, reason: String): PreviewState {
        if (state.previewUrl == null) return state
        return state.copy(pageState = PageState.Failed(reason))
    }
}
