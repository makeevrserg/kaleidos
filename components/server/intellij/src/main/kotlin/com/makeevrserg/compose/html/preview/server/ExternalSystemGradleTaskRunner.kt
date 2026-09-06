package com.makeevrserg.compose.html.preview.server

import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.util.Disposer
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
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
        settings.scriptParameters = config.arguments
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id
        settings.executionName = executionName
        return settings
    }

    private fun DevServerProcessListener.asTaskCallback(subscription: Disposable): TaskCallback {
        return object : TaskCallback {
            override fun onSuccess() {
                Disposer.dispose(subscription)
                onExited(isSuccess = true)
            }

            override fun onFailure() {
                Disposer.dispose(subscription)
                onExited(isSuccess = false)
            }
        }
    }

    /**
     * The process handler of a run only exists once the platform has started it, so the output is
     * captured through the execution topic. The subscription ends when the process is found or, for
     * runs that never start, when the task reports its outcome.
     */
    private fun subscribeToOutput(executionName: String, listener: DevServerProcessListener): Disposable {
        val subscription = Disposer.newDisposable("Compose HTML Preview run output")
        val forwarder = RunOutputForwarder(executionName, listener) { Disposer.dispose(subscription) }
        projectDependencies.messageBus.connect(subscription).subscribe(ExecutionManager.EXECUTION_TOPIC, forwarder)
        return subscription
    }

    private fun findRunningHandlers(executionName: String): List<ProcessHandler> {
        return projectDependencies.runContentManager.allDescriptors
            .filter { descriptor -> descriptor.displayName?.contains(executionName) == true }
            .mapNotNull { descriptor -> descriptor.processHandler }
            .filterNot { handler -> handler.isProcessTerminated }
    }

    override suspend fun start(
        config: DevServerLaunchConfig,
        executionName: String,
        listener: DevServerProcessListener
    ) {
        withContext(mainContext) {
            val subscription = subscribeToOutput(executionName, listener)
            ExternalSystemUtil.runTask(
                createSettings(config, executionName),
                DefaultRunExecutor.EXECUTOR_ID,
                projectDependencies.project,
                GradleConstants.SYSTEM_ID,
                listener.asTaskCallback(subscription),
                ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                false
            )
        }
    }

    override suspend fun stop(executionName: String) {
        val handlers = withContext(mainContext) { findRunningHandlers(executionName) }
        withContext(backgroundContext) {
            handlers.forEach { handler -> handler.destroyProcess() }
        }
    }
}
