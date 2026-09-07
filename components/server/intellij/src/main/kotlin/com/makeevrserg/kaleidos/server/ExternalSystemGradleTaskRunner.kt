package com.makeevrserg.kaleidos.server

import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.util.Disposer
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.util.GradleConstants
import kotlin.coroutines.CoroutineContext

/**
 * Launches Gradle tasks as regular runs so their output lands in the Run tool window, exactly like
 * the Compose Desktop preview does.
 *
 * @param mainContext run descriptors and `ExternalSystemUtil.runTask` require the event dispatch thread
 * @param backgroundContext destroying an external system process cancels the Gradle task synchronously,
 * which the platform forbids on the event dispatch thread
 */
class ExternalSystemGradleTaskRunner(
    private val projectDependencies: ProjectDependencies,
    private val mainContext: CoroutineContext,
    private val backgroundContext: CoroutineContext
) : GradleTaskRunner {

    private fun createSettings(
        config: DevServerLaunchConfig,
        executionName: String
    ): ExternalSystemTaskExecutionSettings {
        val settings = ExternalSystemTaskExecutionSettings()
        settings.externalProjectPath = config.rootProjectPath
        settings.taskNames = listOf(config.qualifiedTaskName)
        settings.scriptParameters = config.scriptParameters
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
        settings.executionName = executionName
        return settings
    }

    /** The outcome ends the flow: nothing is reported after the task has finished. */
    private fun ProducerScope<GradleRunEvent>.createTaskCallback(executionName: String): TaskCallback {
        return object : TaskCallback {
            override fun onSuccess() {
                LOG.info("Run '$executionName' finished successfully")
                trySend(GradleRunEvent.Exited(isSuccess = true))
                close()
            }

            override fun onFailure() {
                LOG.info("Run '$executionName' failed or was terminated")
                trySend(GradleRunEvent.Exited(isSuccess = false))
                close()
            }
        }
    }

    private fun findRunningHandlers(executionName: String): List<ProcessHandler> {
        return projectDependencies.runContentManager.allDescriptors
            .filter { descriptor -> descriptor.displayName?.contains(executionName) == true }
            .mapNotNull { descriptor -> descriptor.processHandler }
            .filterNot { handler -> handler.isProcessTerminated }
    }

    /**
     * The process handler of a run only exists once the platform has started it, so the output is
     * captured through the execution topic. Subscription and listener are released together when the
     * flow ends, whether the run finished, the collector left or `runTask` failed.
     */
    override fun run(config: DevServerLaunchConfig, executionName: String): Flow<GradleRunEvent> = callbackFlow {
        val forwarder = RunOutputForwarder(executionName) { text -> trySend(GradleRunEvent.Output(text)) }
        val subscription = Disposer.newDisposable("Kaleidos run output")
        try {
            projectDependencies.messageBus.connect(subscription).subscribe(ExecutionManager.EXECUTION_TOPIC, forwarder)
            withContext(mainContext) {
                ExternalSystemUtil.runTask(
                    createSettings(config, executionName),
                    DefaultRunExecutor.EXECUTOR_ID,
                    projectDependencies.project,
                    GradleConstants.SYSTEM_ID,
                    createTaskCallback(executionName),
                    ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                    false
                )
            }
            trySend(GradleRunEvent.Started)
            awaitClose()
        } finally {
            Disposer.dispose(subscription)
            forwarder.detach()
        }
    }

    /** A closing project has no runs left: the platform destroyed them before disposing the project. */
    override suspend fun stop(executionName: String) {
        if (projectDependencies.project.isDisposed) {
            LOG.info("Not stopping '$executionName': the project is disposed and its runs are already gone")
            return
        }
        val handlers = withContext(mainContext) { findRunningHandlers(executionName) }
        LOG.info("Stopping '$executionName': ${handlers.size} running process(es)")
        withContext(backgroundContext) {
            handlers.forEach { handler -> handler.destroyProcess() }
        }
    }

    private companion object {
        val LOG = Logger.getInstance(ExternalSystemGradleTaskRunner::class.java)
    }
}
