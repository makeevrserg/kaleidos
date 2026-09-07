package com.makeevrserg.compose.html.preview.di

import com.intellij.openapi.project.Project
import com.makeevrserg.compose.html.preview.core.IntellijDispatchers
import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.core.lifecycle.Lifecycle
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.di.IntellijPreviewModule
import com.makeevrserg.compose.html.preview.harness.di.HarnessModule
import com.makeevrserg.compose.html.preview.host.di.HostModule
import com.makeevrserg.compose.html.preview.psi.di.PsiModule
import com.makeevrserg.compose.html.preview.server.di.IntellijServerModule
import com.makeevrserg.compose.html.preview.server.di.ServerModule
import com.makeevrserg.compose.html.preview.ui.di.UiModule
import kotlinx.coroutines.CoroutineScope
import java.time.Clock

/**
 * Composition root for one project. Modules are created in dependency order; [lifecycle] brings up
 * the components with side effects and takes them down again.
 */
class RootModule(
    project: Project,
    coroutineScope: CoroutineScope
) {
    private val coreModule = CoreModule(
        coroutineScope = coroutineScope,
        dispatchers = IntellijDispatchers(),
        clock = Clock.systemDefaultZone()
    )

    private val intellijCoreModule = IntellijCoreModule(project = project)

    val psiModule = PsiModule(
        coreModule = coreModule,
        intellijCoreModule = intellijCoreModule
    )

    private val hostModule = HostModule(
        coreModule = coreModule,
        intellijCoreModule = intellijCoreModule
    )

    private val harnessModule = HarnessModule(
        coreModule = coreModule,
        projectPreviewSource = psiModule.projectPreviewSource,
        structureReader = hostModule.projectStructureReader
    )

    private val intellijServerModule = IntellijServerModule(
        coreModule = coreModule,
        intellijCoreModule = intellijCoreModule
    )

    private val serverModule = ServerModule(
        coreModule = coreModule,
        gradleTaskRunner = intellijServerModule.gradleTaskRunner
    )

    private val intellijPreviewModule = IntellijPreviewModule(
        coreModule = coreModule,
        intellijCoreModule = intellijCoreModule,
        psiModule = psiModule,
        serverModule = serverModule,
        hostModule = hostModule,
        harnessModule = harnessModule
    )

    val previewStore: PreviewStore = intellijPreviewModule.previewModule.previewStore

    val uiModule = UiModule(
        previewStore = previewStore,
        project = project
    )

    val lifecycle: Lifecycle = intellijPreviewModule.lifecycle
}
