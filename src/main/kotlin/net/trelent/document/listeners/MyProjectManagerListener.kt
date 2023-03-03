package net.trelent.document.listeners

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import net.trelent.document.helpers.getLatestVersion
import net.trelent.document.services.MyProjectService
import net.trelent.document.services.ParserService

internal class MyProjectManagerListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        project.service<MyProjectService>()
        service<ParserService>()

        // Check if there is a new version available
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId("net.trelent.document"))!!.version
        val latestVersion = getLatestVersion()!!

        // If there is, prompt the user
        // Ideally there should be a button that auto-updates, will figure that out later.
        if(currentVersion != latestVersion) {
            if(currentVersion < latestVersion) {
                val versionNotification = Notification(
                    "Trelent Warning Notification Group",
                    "Trelent - New Version Available",
                    "You are currently using Trelent version $currentVersion. The latest available version is $latestVersion. Please update to the most recent version to get the latest improvements, features and more. To do so, navigate to Settings -> Plugins -> Trelent -> Update.",
                    NotificationType.WARNING
                )

                Notifications.Bus.notify(versionNotification, project)
            }
        }
    }
}