package com.makeevrserg.compose.html.preview.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.makeevrserg.compose.html.preview.core.BackgroundCoroutineFeature
import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.core.MainCoroutineFeature
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewFeature
import com.makeevrserg.compose.html.preview.feature.PreviewStateReducer
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.host.GradleModuleCatalog
import com.makeevrserg.compose.html.preview.host.ModuleDependencyGraphReader
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostSelector
import com.makeevrserg.compose.html.preview.notification.PreviewNotifier
import com.makeevrserg.compose.html.preview.psi.AnnotationFqnResolver
import com.makeevrserg.compose.html.preview.psi.PreviewFileScanner
import com.makeevrserg.compose.html.preview.psi.PreviewFunctionDetector
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerHealthCheck
import com.makeevrserg.compose.html.preview.server.DevServerOriginResolver
import com.makeevrserg.compose.html.preview.server.DevServerUrlDetector
import com.makeevrserg.compose.html.preview.server.GradleTaskRunner
import com.makeevrserg.compose.html.preview.server.KobwebConfReader
import com.makeevrserg.compose.html.preview.source.EditorTracker
import com.makeevrserg.compose.html.preview.source.SourceChangeTracker
import com.makeevrserg.compose.html.preview.source.ToolWindowVisibilityTracker
import com.makeevrserg.compose.html.preview.ui.PreviewStateTexts
import com.makeevrserg.compose.html.preview.ui.browser.PreviewBrowserFactory
import com.makeevrserg.compose.html.preview.url.PreviewUrlFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Composition root of the plugin for one project. IntelliJ instantiates it lazily and cancels
 * [coroutineScope] when the project closes; everything else is wired manually through constructors.
 * It is also the [Disposable] parent of listeners that must not outlive the project.
 */
@Service(Service.Level.PROJECT)
class PreviewProjectService(project: Project, private val coroutineScope: CoroutineScope) : Disposable {
    private val projectDependencies = ProjectDependencies(project)

    val previewFunctionDetector = PreviewFunctionDetector(AnnotationFqnResolver())

    val previewBrowserFactory = PreviewBrowserFactory()

    val previewStateTexts = PreviewStateTexts()

    private val healthCheck = DevServerHealthCheck(
        ioDispatcher = Dispatchers.IO,
        connectTimeout = HEALTH_CHECK_TIMEOUT
    )

    private val devServerController = DevServerController(
        healthCheck = healthCheck,
        gradleTaskRunner = GradleTaskRunner(
            projectDependencies = projectDependencies,
            backgroundDispatcher = Dispatchers.Default
        ),
        originResolver = DevServerOriginResolver(
            kobwebConfReader = KobwebConfReader(ioDispatcher = Dispatchers.IO),
            healthCheck = healthCheck
        ),
        urlDetector = DevServerUrlDetector(),
        startupTimeout = DEV_SERVER_STARTUP_TIMEOUT,
        pollInterval = DEV_SERVER_POLL_INTERVAL,
        coroutineFeature = BackgroundCoroutineFeature(coroutineScope)
    )

    val feature: PreviewStore = PreviewFeature(
        devServerController = devServerController,
        hostLocator = PreviewHostLocator(
            gradleModuleCatalog = GradleModuleCatalog(projectDependencies),
            graphReader = ModuleDependencyGraphReader(projectDependencies),
            selector = PreviewHostSelector(),
            backgroundDispatcher = Dispatchers.Default
        ),
        projectDependencies = projectDependencies,
        previewNotifier = PreviewNotifier(project),
        reducer = PreviewStateReducer(
            clock = Clock.systemDefaultZone(),
            previewUrlFactory = PreviewUrlFactory()
        ),
        coroutineFeature = MainCoroutineFeature(coroutineScope)
    )

    private val editorTracker = EditorTracker(
        projectDependencies = projectDependencies,
        fileScanner = PreviewFileScanner(previewFunctionDetector),
        contract = feature,
        coroutineFeature = BackgroundCoroutineFeature(coroutineScope)
    )

    private val sourceChangeTracker = SourceChangeTracker(projectDependencies, feature)

    private val toolWindowVisibilityTracker = ToolWindowVisibilityTracker(projectDependencies, feature)

    init {
        editorTracker.start(parentDisposable = this)
        sourceChangeTracker.start(parentDisposable = this)
        toolWindowVisibilityTracker.start(parentDisposable = this)
    }

    /** Child scope for UI that lives shorter than the project, for example the tool window content. */
    fun createMainCoroutineFeature(): CoroutineFeature = MainCoroutineFeature(coroutineScope)

    override fun dispose() = Unit

    private companion object {
        val HEALTH_CHECK_TIMEOUT = 2.seconds
        val DEV_SERVER_STARTUP_TIMEOUT = 5.minutes
        val DEV_SERVER_POLL_INTERVAL = 1.seconds
    }
}
