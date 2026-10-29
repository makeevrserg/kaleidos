package com.makeevrserg.kaleidos.server.service

import com.makeevrserg.kaleidos.core.coroutine.api.CoroutineFeature
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.api.DevServerController
import com.makeevrserg.kaleidos.server.api.DevServerHealthCheck
import com.makeevrserg.kaleidos.server.api.DevServerOriginResolver
import com.makeevrserg.kaleidos.server.model.DevServerLaunchOptions
import com.makeevrserg.kaleidos.server.model.DevServerLaunchRequest
import com.makeevrserg.kaleidos.server.model.DevServerState
import com.makeevrserg.kaleidos.server.usecase.LaunchDevServerUseCase
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
 * The state is the latest launch request switched into [LaunchDevServerUseCase.invoke]. A new request cancels
 * the previous launch, which stops its run before the next one starts, and cancelling the owning scope
 * stops whatever is running. No launch can outlive its request, so nothing has to be serialised.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultDevServerController(
    private val launcher: LaunchDevServerUseCase,
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
                launcher.invoke(
                    launchRequest.host,
                    launchRequest.options,
                    launchRequest.isRestart
                )
            }
        }
        .stateIn(
            coroutineFeature,
            SharingStarted.Eagerly,
            DevServerState.Stopped
        )

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
        if (isLaunchBlocked(
                current,
                host,
                options,
                retryAfterFailure
            )
        ) {
            return
        }
        requestLaunch(
            host,
            options,
            isRestart = false
        )
    }

    override fun stop() {
        request.value = null
    }

    override fun restart(host: PreviewHost, options: DevServerLaunchOptions) {
        requestLaunch(
            host,
            options,
            isRestart = true
        )
    }
}
