package net.trelent.document.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import net.trelent.document.helpers.Function
import java.util.*

class TrelentListeners {

    interface DocumentedListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_DOCUMENTED_ACTION: Topic<DocumentedListener> = Topic(
                DocumentedListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun documented(document: Document, language: String);
    }

    interface ParseListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_PARSE_TRACK_ACTION: Topic<ParseListener> = Topic(
                ParseListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun parse(document: Document, language: String, functions: List<Function>);
    }

    interface RangeUpdateListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_RANGE_UPDATE: Topic<RangeUpdateListener> = Topic(
                RangeUpdateListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun rangeUpdate(document: Document);
    }


}