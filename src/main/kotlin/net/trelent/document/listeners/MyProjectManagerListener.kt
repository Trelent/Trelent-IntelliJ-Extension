package net.trelent.document.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import net.trelent.document.services.MyProjectService

internal class MyProjectManagerListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        project.service<MyProjectService>()
    }
}