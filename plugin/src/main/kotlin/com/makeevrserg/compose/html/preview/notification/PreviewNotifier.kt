package com.makeevrserg.compose.html.preview.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class PreviewNotifier(private val project: Project) {

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, type)
            .notify(project)
    }

    fun error(content: String) = notify(content, NotificationType.ERROR)

    companion object {
        /** Must match the `notificationGroup` id registered in plugin.xml. */
        const val GROUP_ID = "Compose HTML Preview"
    }
}
