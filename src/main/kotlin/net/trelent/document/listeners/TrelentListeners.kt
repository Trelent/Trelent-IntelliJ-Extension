package net.trelent.document.listeners

import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import java.util.*

class TrelentListeners {

    interface DocumentedListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_DOCUMENTED_ACTION: Topic<DocumentedListener> = Topic(
                DocumentedListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun documented(editor: Editor, language: String);
    }


}