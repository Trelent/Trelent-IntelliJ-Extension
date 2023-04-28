package net.trelent.document.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.util.messages.Topic
import net.trelent.document.helpers.Function
import java.util.*

class TrelentListeners {

    //Should only be invoked in the ChangeDetectionService. Fires when a function is documented
    interface DocumentedListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_DOCUMENTED_ACTION: Topic<DocumentedListener> = Topic(
                DocumentedListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun documented(document: Document, function: Function, language: String);
    }

    //Should only be invoked in the ChangeDetectionService. Fires when a document is parsed
    interface ParseListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_PARSE_TRACK_ACTION: Topic<ParseListener> = Topic(
                ParseListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun parse(document: Document, language: String);
    }

    //Should only be invoked in the ChangeDetectionService. Fires when function ranges are updated
    interface RangeUpdateListener: EventListener {
        companion object{
            @JvmStatic val TRELENT_RANGE_UPDATE: Topic<RangeUpdateListener> = Topic(
                RangeUpdateListener::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun rangeUpdate(document: Document);
    }

    //Should only be invoked in the ChangeDetectionService. Fires when the changed functions stored change in a way
    //not covered by the above events (EG: Deleting a docChange externally)
    interface ChangeUpdate: EventListener {
        companion object{
            @JvmStatic val TRELENT_CHANGE_UPDATE: Topic<ChangeUpdate> = Topic(ChangeUpdate::class.java, Topic.BroadcastDirection.TO_PARENT);
        }

        fun changeUpdate(document: Document)
    }


}