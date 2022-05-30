package net.trelent.document.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import net.trelent.document.helpers.URIService

class MyProjectService(project: Project) : Disposable {
    var uriService: URIService = URIService()
    val port: Int = uriService.start()

    init {
        // Init project level services
        println("Enabled Trelent URI Service on port $port.")
    }

    override fun dispose() {
        uriService.stop()
    }

}
