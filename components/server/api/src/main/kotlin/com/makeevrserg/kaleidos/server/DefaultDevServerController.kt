package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.core.CoroutineFeature
import com.makeevrserg.kaleidos.host.PreviewHost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Nothing is configured: the task follows the kind of the module and the origin comes from the output
 * of the run or from the Kobweb configuration file.
 *
 * The state is the latest launch request switched into [DevServerLauncher.launch]. A new request cancels
 * the previous launch, which stops its run before the next one starts, and cancelling the owning scope
 * stops whatever is running. No launch can outlive its request, so nothing has to be serialised.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDevServerController(
    private val launcher: DevServerLauncher,
    private val originResolver: DevServerOriginResolver,
    private val healthCheck: DevServerHealthCheck,
    coroutineFeature: CoroutineFeature
) : DevServerController {
    private val request = MutableStateFlow<DevServerLaunchRequest?>(null)

    override val state: StateFlow<DevServerState> = request
        .flatMapLatest { launchRequest ->
            if (launchRequest == null) {
                flowOf(DevServerState.Stopped)
            } else {
                launcher.launch(launchRequest.host, launchRequest.options, launchRequest.isRestart)
            }
        }
        .stateIn(coroutineFeature, SharingStarted.Eagerly, DevServerState.Stopped)

    /** Every request is distinct, so asking for a module again after its server stopped launches again. */
    private fun requestLaunch(host: PreviewHost, options: DevServerLaunchOptions, isRestart: Boolean) {
        request.update { previous ->
            DevServerLaunchRequest(
                host = host,
                options = options,
                isRestart = isRestart,
                attempt = (previous?.attempt ?: 0) + 1
            )
        }
    }

    private suspend fun isAlreadyServing(current: DevServerState, host: PreviewHost): Boolean {
        return when (current) {
            is DevServerState.Starting -> current.host == host
            is DevServerState.Running -> current.host == host && healthCheck.isAlive(current.baseUrl)
            DevServerState.Stopped, is DevServerState.Failed -> false
        }
    }

    /**
     * A previous failure blocks automatic launches: a broken preview module would otherwise be relaunched
     * on every editor event, flooding the IDE with errors. Only explicit requests retry. Adoption is never
     * blocked, so a file whose server is up is shown even after another module failed.
     */
    private suspend fun isLaunchBlocked(
        current: DevServerState,
        host: PreviewHost,
        options: DevServerLaunchOptions,
        retryAfterFailure: Boolean
    ): Boolean {
        if (current !is DevServerState.Failed || retryAfterFailure) return false
        return originResolver.findAlive(host, options) == null
    }

    override suspend fun requestRunning(
        host: PreviewHost,
        options: DevServerLaunchOptions,
        retryAfterFailure: Boolean
    ) {
        val current = state.value
        if (isAlreadyServing(current, host)) return
        if (isLaunchBlocked(current, host, options, retryAfterFailure)) return
        requestLaunch(host, options, isRestart = false)
    }

    override fun stop() {
        request.value = null
    }

    override fun restart(host: PreviewHost, options: DevServerLaunchOptions) {
        requestLaunch(host, options, isRestart = true)
    }
}
