package com.github.calumbird.trelentintellijextension.services

import com.intellij.openapi.project.Project
import com.github.calumbird.trelentintellijextension.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
