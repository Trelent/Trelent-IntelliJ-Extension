package net.trelent.document.widgets

import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import java.util.*

public class WidgetListeners {

    public interface TrelentDocumentationListener: EventListener {

        companion object{
            @JvmStatic val TRELENT_DOCUMENTATION_ACTION: Topic<TrelentDocumentationListener> = Topic(
                TrelentDocumentationListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun updateDocState(state: Boolean);

    }

    public interface DocumentedListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_DOCUMENTED_ACTION: Topic<DocumentedListener> = Topic(
                DocumentedListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun documented(editor: Editor, language: String);
    }


}