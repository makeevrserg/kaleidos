package com.makeevrserg.kaleidos.dependencies

import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorEventMulticaster
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiManager
import com.intellij.util.messages.MessageBus

/**
 * Project-scoped platform services gathered in one place so features receive a single dependency.
 */
class ProjectDependencies(val project: Project) {
    val toolWindowManager: ToolWindowManager = ToolWindowManager.getInstance(project)
    val runContentManager: RunContentManager = RunContentManager.getInstance(project)
    val fileIndex: ProjectFileIndex = ProjectFileIndex.getInstance(project)
    val fileDocumentManager: FileDocumentManager = FileDocumentManager.getInstance()
    val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project)
    val psiManager: PsiManager = PsiManager.getInstance(project)
    val messageBus: MessageBus = project.messageBus
    val editorEventMulticaster: EditorEventMulticaster = EditorFactory.getInstance().eventMulticaster
    val moduleManager: ModuleManager = ModuleManager.getInstance(project)
    val virtualFileManager: VirtualFileManager = VirtualFileManager.getInstance()
    val projectDataManager: ProjectDataManager = ProjectDataManager.getInstance()
}
