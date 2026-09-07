package com.makeevrserg.kaleidos.di

import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.core.IntellijDispatchers
import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.core.lifecycle.Lifecycle
import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.feature.di.IntellijPreviewModule
import com.makeevrserg.kaleidos.harness.di.HarnessModule
import com.makeevrserg.kaleidos.host.di.HostModule
import com.makeevrserg.kaleidos.psi.di.PsiModule
import com.makeevrserg.kaleidos.server.di.IntellijServerModule
import com.makeevrserg.kaleidos.server.di.ServerModule
import com.makeevrserg.kaleidos.ui.di.UiModule
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
