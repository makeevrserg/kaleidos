package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.notification.PreviewNotifier
import com.makeevrserg.compose.html.preview.psi.PreviewFunction
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.ui.PreviewToolWindowIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreviewFeature(
    private val devServerController: DevServerController,
    private val hostLocator: PreviewHostLocator,
    private val projectDependencies: ProjectDependencies,
    private val previewNotifier: PreviewNotifier,
    private val reducer: PreviewStateReducer,
    coroutineFeature: CoroutineFeature
) : PreviewStore, CoroutineFeature by coroutineFeature {

    private val mutableState = MutableStateFlow(
        PreviewState(
            target = null,
            previewUrl = null,
            isToolWindowVisible = false,
            sourceState = SourceState.UpToDate,
            serverState = DevServerState.Stopped
        )
    )

    override val state: StateFlow<PreviewState> = mutableState.asStateFlow()

    init {
        devServerController.state
            .onEach { serverState -> mutableState.update { current -> reducer.setServerState(current, serverState) } }
            .launchIn(this)
        devServerController.state
            .filterIsInstance<DevServerState.Failed>()
            .onEach { failed -> previewNotifier.error(failed.reason) }
            .launchIn(this)
    }

    private fun showToolWindow() {
        projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.show()
    }

    /**
     * A found host is kept on the target; a missing one is looked up again on every request because
     * a Gradle sync that finished meanwhile may have changed the answer. Explicit requests always
     * look up again, so a renamed or newly added preview module is picked up without reopening the file.
     */
    private suspend fun resolveHost(target: PreviewTarget, force: Boolean): PreviewHostResolution {
        val cached = target.host
        if (cached is PreviewHostResolution.Found && !force) return cached
        val resolution = hostLocator.locate(target.filePath)
        mutableState.update { current -> reducer.attachHost(current, target.filePath, resolution) }
        if (resolution is PreviewHostResolution.NotFound && resolution != cached) {
            previewNotifier.error(resolution.reason)
        }
        return resolution
    }

    /**
     * The dev server is contacted only for a page that is about to be shown. Automatic checks never
     * relaunch a server whose last launch failed; explicit user requests do.
     */
    private fun connectIfNeeded(retryAfterFailure: Boolean) {
        val current = mutableState.value
        val target = current.target ?: return
        if (target.previews.isEmpty() || !current.isToolWindowVisible) return
        launch {
            val resolution = resolveHost(target, force = retryAfterFailure)
            if (resolution is PreviewHostResolution.Found) {
                devServerController.ensureRunning(resolution.host, retryAfterFailure)
            }
        }
    }

    override fun onFileSelected(target: PreviewTarget?) {
        mutableState.update { current -> reducer.select(current, target) }
        connectIfNeeded(retryAfterFailure = false)
    }

    override fun onFocusPreview(previewFunction: PreviewFunction) {
        mutableState.update { current -> reducer.focus(current, previewFunction) }
        launch { showToolWindow() }
        connectIfNeeded(retryAfterFailure = true)
    }

    /**
     * Only the transition into visibility is a user request worth a retry; the manager also reports
     * state changes of other tool windows while ours stays visible.
     */
    override fun onToolWindowVisibilityChanged(isVisible: Boolean) {
        val wasVisible = mutableState.value.isToolWindowVisible
        mutableState.update { current -> reducer.setToolWindowVisible(current, isVisible) }
        if (isVisible && !wasVisible) connectIfNeeded(retryAfterFailure = true)
    }

    override fun onReconnect() {
        connectIfNeeded(retryAfterFailure = true)
    }

    override fun onRestartServer() {
        val target = mutableState.value.target ?: return
        launch {
            val resolution = resolveHost(target, force = true)
            if (resolution is PreviewHostResolution.Found) devServerController.restart(resolution.host)
        }
    }

    override fun onStopServer() {
        launch { devServerController.stop() }
    }

    override fun onSourceChanged(changedFileName: String) {
        mutableState.update { current -> reducer.markChanged(current, changedFileName) }
    }

    override fun onPageLoaded(isReload: Boolean) {
        mutableState.update { current -> reducer.markLoaded(current, isReload) }
    }
}
