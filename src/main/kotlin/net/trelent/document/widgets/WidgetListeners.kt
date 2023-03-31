package net.trelent.document.widgets

import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import java.util.*

class WidgetListeners {

    interface DocumentedListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_DOCUMENTED_ACTION: Topic<DocumentedListener> = Topic(
                DocumentedListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun documented(editor: Editor, language: String);
    }


}