package net.trelent.document.services

import com.intellij.openapi.Disposable
import net.trelent.document.helpers.URIService

class MyProjectService() : Disposable {
    private var uriService: URIService = URIService()
    val port: Int = uriService.start()

    init {
        // Init project level services
        println("Enabled Trelent URI Service on port $port.")
    }

    override fun dispose() {
        uriService.stop()
    }

}
