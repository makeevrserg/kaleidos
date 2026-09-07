package com.makeevrserg.compose.html.preview.server.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.server.DefaultDevServerController
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerLauncher
import com.makeevrserg.compose.html.preview.server.DevServerOriginResolver
import com.makeevrserg.compose.html.preview.server.DevServerUrlDetector
import com.makeevrserg.compose.html.preview.server.GradleTaskRunner
import com.makeevrserg.compose.html.preview.server.HttpDevServerHealthCheck
import com.makeevrserg.compose.html.preview.server.KobwebConfReader
import com.makeevrserg.compose.html.preview.server.KobwebServerStateReader
import com.makeevrserg.compose.html.preview.server.KobwebServerStopper
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ServerModule(
    coreModule: CoreModule,
    gradleTaskRunner: GradleTaskRunner
) {
    private val healthCheck = HttpDevServerHealthCheck(
        ioContext = coreModule.dispatchers.io,
        connectTimeout = HEALTH_CHECK_TIMEOUT
    )

    private val kobwebServerStateReader = KobwebServerStateReader(ioContext = coreModule.dispatchers.io)

    private val originResolver = DevServerOriginResolver(
        kobwebConfReader = KobwebConfReader(ioContext = coreModule.dispatchers.io),
        kobwebServerStateReader = kobwebServerStateReader,
        healthCheck = healthCheck
    )

    val devServerController: DevServerController = DefaultDevServerController(
        launcher = DevServerLauncher(
            gradleTaskRunner = gradleTaskRunner,
            detachedServerStopper = KobwebServerStopper(
                stateReader = kobwebServerStateReader,
                ioContext = coreModule.dispatchers.io,
                stopTimeout = DETACHED_SERVER_STOP_TIMEOUT
            ),
            healthCheck = healthCheck,
            originResolver = originResolver,
            urlDetector = DevServerUrlDetector(),
            startupTimeout = DEV_SERVER_STARTUP_TIMEOUT,
            pollInterval = DEV_SERVER_POLL_INTERVAL
        ),
        originResolver = originResolver,
        healthCheck = healthCheck,
        coroutineFeature = coreModule.backgroundCoroutineFeature
    )

    private companion object {
        val HEALTH_CHECK_TIMEOUT = 2.seconds
        val DEV_SERVER_STARTUP_TIMEOUT = 5.minutes
        val DEV_SERVER_POLL_INTERVAL = 1.seconds
        val DETACHED_SERVER_STOP_TIMEOUT = 10.seconds
    }
}
