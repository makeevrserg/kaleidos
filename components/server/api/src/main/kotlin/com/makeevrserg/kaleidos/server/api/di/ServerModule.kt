package com.makeevrserg.kaleidos.server.api.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.server.api.api.DevServerController
import com.makeevrserg.kaleidos.server.api.api.DevServerLog
import com.makeevrserg.kaleidos.server.api.api.GradleTaskRunner
import com.makeevrserg.kaleidos.server.api.file.DefaultKobwebConfReader
import com.makeevrserg.kaleidos.server.api.file.DefaultKobwebServerStateReader
import com.makeevrserg.kaleidos.server.api.memory.DefaultAnnouncedBaseUrlParser
import com.makeevrserg.kaleidos.server.api.memory.DefaultDevServerLog
import com.makeevrserg.kaleidos.server.api.memory.DevServerUrlDetector
import com.makeevrserg.kaleidos.server.api.memory.LsofPortListenerCommand
import com.makeevrserg.kaleidos.server.api.memory.NetstatPortListenerCommand
import com.makeevrserg.kaleidos.server.api.service.DefaultDevServerController
import com.makeevrserg.kaleidos.server.api.service.DefaultDevServerOriginResolver
import com.makeevrserg.kaleidos.server.api.service.HttpDevServerHealthCheck
import com.makeevrserg.kaleidos.server.api.service.KobwebServerStopper
import com.makeevrserg.kaleidos.server.api.service.OsPortListenerLookup
import com.makeevrserg.kaleidos.server.api.usecase.LaunchDevServerUseCase
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

    private val kobwebServerStateReader = DefaultKobwebServerStateReader(ioContext = coreModule.dispatchers.io)

    private val isWindows = System.getProperty(OS_NAME_PROPERTY).orEmpty().startsWith(WINDOWS_OS_NAME)

    private val portListenerLookup = OsPortListenerLookup(
        command = if (isWindows) NetstatPortListenerCommand() else LsofPortListenerCommand(),
        ioContext = coreModule.dispatchers.io,
        timeout = PORT_LISTENER_TIMEOUT
    )

    private val originResolver = DefaultDevServerOriginResolver(
        kobwebConfReader = DefaultKobwebConfReader(ioContext = coreModule.dispatchers.io),
        kobwebServerStateReader = kobwebServerStateReader,
        healthCheck = healthCheck
    )

    private val urlDetector = DevServerUrlDetector()

    val devServerLog: DevServerLog = DefaultDevServerLog(capacity = DEV_SERVER_LOG_CAPACITY)

    val devServerController: DevServerController = DefaultDevServerController(
        launcher = LaunchDevServerUseCase(
            gradleTaskRunner = gradleTaskRunner,
            detachedServerStopper = KobwebServerStopper(
                stateReader = kobwebServerStateReader,
                ioContext = coreModule.dispatchers.io,
                stopTimeout = DETACHED_SERVER_STOP_TIMEOUT
            ),
            healthCheck = healthCheck,
            originResolver = originResolver,
            portListenerLookup = portListenerLookup,
            announcedBaseUrlParser = { DefaultAnnouncedBaseUrlParser(urlDetector) },
            log = devServerLog,
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
        const val DEV_SERVER_LOG_CAPACITY = 10_000
        val HEALTH_CHECK_TIMEOUT = 2.seconds
        val PORT_LISTENER_TIMEOUT = 3.seconds
        val DEV_SERVER_STARTUP_TIMEOUT = 5.minutes
        val DEV_SERVER_POLL_INTERVAL = 1.seconds
        val DETACHED_SERVER_STOP_TIMEOUT = 10.seconds
    }
}
