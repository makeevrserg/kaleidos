package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.notification.PreviewNotifier
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connecting to the dev server is driven by the latest [PreviewConnectRequest]: a newer request, from
 * a faster file switch for example, cancels the host lookup of the previous one instead of racing it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreviewFeature(
    private val devServerController: DevServerController,
    private val serverLauncher: PreviewServerLauncher,
    private val hostLocator: PreviewHostLocator,
    private val toolWindowPresenter: PreviewToolWindowPresenter,
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
            serverState = DevServerState.Stopped,
            pageState = PageState.Loading
        )
    )

    private val connectRequests = MutableStateFlow<PreviewConnectRequest?>(null)

    override val state: StateFlow<PreviewState> = mutableState.asStateFlow()

    init {
        connectRequests
            .filterNotNull()
            .mapLatest(::connect)
            .launchIn(this)
        devServerController.state
            .onEach { serverState -> mutableState.update { current -> reducer.setServerState(current, serverState) } }
            .launchIn(this)
        devServerController.state
            .filterIsInstance<DevServerState.Failed>()
            .onEach { failed -> previewNotifier.error(failed.reason) }
            .launchIn(this)
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

    private suspend fun connect(request: PreviewConnectRequest) {
        val resolution = resolveHost(request.target, force = request.retryAfterFailure)
        if (resolution !is PreviewHostResolution.Found) return
        serverLauncher.requestRunning(resolution.host, request.retryAfterFailure)
    }

    /**
     * The dev server is contacted only for a page that is about to be shown. Automatic checks never
     * relaunch a server whose last launch failed; explicit user requests do.
     */
    private fun connectIfNeeded(retryAfterFailure: Boolean) {
        val current = mutableState.value
        val target = current.target ?: return
        if (target.renderablePreviews.isEmpty() || !current.isToolWindowVisible) return
        connectRequests.update { previous ->
            PreviewConnectRequest(target, retryAfterFailure, attempt = (previous?.attempt ?: 0) + 1)
        }
    }

    override fun onFileSelected(target: PreviewTarget?) {
        mutableState.update { current -> reducer.select(current, target) }
        connectIfNeeded(retryAfterFailure = false)
    }

    override fun onFocusPreview(previewFunction: PreviewFunction) {
        mutableState.update { current -> reducer.focus(current, previewFunction) }
        launch { toolWindowPresenter.show() }
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
            if (resolution is PreviewHostResolution.Found) serverLauncher.restart(resolution.host)
        }
    }

    override fun onStopServer() = devServerController.stop()

    override fun onSourceChanged(changedFileName: String) {
        mutableState.update { current -> reducer.markChanged(current, changedFileName) }
    }

    override fun onPageLoaded(load: PageLoad) {
        mutableState.update { current ->
            when (load) {
                PageLoad.Succeeded -> reducer.markPageShown(current)
                is PageLoad.Failed -> reducer.markPageFailed(current, load.reason)
            }
        }
    }
}
