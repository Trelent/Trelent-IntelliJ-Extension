package net.trelent.document.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent


interface AutodocService{

}
@Service(Service.Level.PROJECT)
class AutodocServiceImpl(private val project: Project) {

    init{
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object: BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                super.after(events)
            }
        })
    }

}