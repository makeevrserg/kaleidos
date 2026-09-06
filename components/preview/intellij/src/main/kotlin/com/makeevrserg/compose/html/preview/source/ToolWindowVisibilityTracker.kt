package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.toolwindow.PreviewToolWindowIds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Reports whether the preview tool window is visible, so nothing is rendered and no dev server is
 * started while the user cannot see the result.
 *
 * @param mainContext [com.makeevrserg.compose.html.preview.core.PreviewDispatchers.main]; the tool
 * window state is read on the event dispatch thread
 */
class ToolWindowVisibilityTracker(
    private val projectDependencies: ProjectDependencies,
    private val contract: PreviewStore,
    private val mainContext: CoroutineContext
) {
    private fun isPreviewToolWindowVisible(): Boolean {
        return projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.isVisible == true
    }

    /**
     * Current visibility first, then every change the manager reports, including changes of other
     * tool windows; the store ignores repeats. Subscribing and the first read happen together on the
     * UI thread so no change is lost in between.
     */
    private fun visibility(): Flow<Boolean> = callbackFlow {
        val listener = object : ToolWindowManagerListener {
            override fun stateChanged(toolWindowManager: ToolWindowManager) {
                trySend(isPreviewToolWindowVisible())
            }
        }
        val subscription = Disposer.newDisposable("Compose HTML Preview tool window visibility")
        withContext(mainContext) {
            projectDependencies.messageBus.connect(subscription).subscribe(ToolWindowManagerListener.TOPIC, listener)
            trySend(isPreviewToolWindowVisible())
        }
        awaitClose { Disposer.dispose(subscription) }
    }

    /** Reports visibility until cancelled. */
    suspend fun track() = visibility().collect(contract::onToolWindowVisibilityChanged)
}
