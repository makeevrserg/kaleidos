package com.makeevrserg.kaleidos.server.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.server.DefaultDevServerController
import com.makeevrserg.kaleidos.server.DevServerController
import com.makeevrserg.kaleidos.server.DevServerLauncher
import com.makeevrserg.kaleidos.server.DevServerOriginResolver
import com.makeevrserg.kaleidos.server.DevServerUrlDetector
import com.makeevrserg.kaleidos.server.GradleTaskRunner
import com.makeevrserg.kaleidos.server.HttpDevServerHealthCheck
import com.makeevrserg.kaleidos.server.KobwebConfReader
import com.makeevrserg.kaleidos.server.KobwebServerStateReader
import com.makeevrserg.kaleidos.server.KobwebServerStopper
import com.makeevrserg.kaleidos.server.LsofPortListenerCommand
import com.makeevrserg.kaleidos.server.NetstatPortListenerCommand
import com.makeevrserg.kaleidos.server.OsPortListenerLookup
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

    private val isWindows = System.getProperty(OS_NAME_PROPERTY).orEmpty().startsWith(WINDOWS_OS_NAME)

    private val portListenerLookup = OsPortListenerLookup(
        command = if (isWindows) NetstatPortListenerCommand() else LsofPortListenerCommand(),
        ioContext = coreModule.dispatchers.io,
        timeout = PORT_LISTENER_TIMEOUT
    )

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
            portListenerLookup = portListenerLookup,
            urlDetector = DevServerUrlDetector(),
            startupTimeout = DEV_SERVER_STARTUP_TIMEOUT,
            pollInterval = DEV_SERVER_POLL_INTERVAL
        ),
        originResolver = originResolver,
        healthCheck = healthCheck,
        coroutineFeature = coreModule.backgroundCoroutineFeature
    )

    private companion object {
        const val OS_NAME_PROPERTY = "os.name"
        const val WINDOWS_OS_NAME = "Windows"
        val HEALTH_CHECK_TIMEOUT = 2.seconds
        val PORT_LISTENER_TIMEOUT = 3.seconds
        val DEV_SERVER_STARTUP_TIMEOUT = 5.minutes
        val DEV_SERVER_POLL_INTERVAL = 1.seconds
        val DETACHED_SERVER_STOP_TIMEOUT = 10.seconds
    }
}
