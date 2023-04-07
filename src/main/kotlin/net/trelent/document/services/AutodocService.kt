package net.trelent.document.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.parseDocument
import net.trelent.document.helpers.writeDocstringsFromFunctions
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.settings.TrelentSettingsState.TrelentTag;
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


interface AutodocService{

}
class AutodocServiceImpl: Disposable {

    val updating: HashSet<Document> = hashSetOf()

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                try {
                    val changeDetectionService = ChangeDetectionService.getInstance()!!;
                    changeDetectionService.updateFunctionRanges(event);
                    super.documentChanged(event)
                } finally {
                }

            }
        }, this)
    }

    fun updateDocstrings(doc: Document) {
        try {
            val changeDetectionService = ChangeDetectionService.getInstance()!!
            val editor = EditorFactory.getInstance().allEditors.find {
                it.document == doc
            }!!;

            parseDocument(editor, editor.project!!);

            //TODO: Apply highlights

            changeDetectionService.getHistory(doc).allFunctions;

            if (updating.contains(doc)) {
                return;
            }
            updating.add(doc);

            val functionsToDocument = changeDetectionService.getDocChanges(doc);

            //Get function tags

            val taggedFunctions = getFunctionTags(functionsToDocument.values.toList());

            if(taggedFunctions.isEmpty()){
                updating.remove(doc);
                return;
            }

            val autoFunctions = taggedFunctions[TrelentTag.AUTO]!!;
            writeDocstringsFromFunctions(autoFunctions, editor, editor.project!!);

        } finally {
            //TODO: Highlight Functions
            updating.remove(doc);
        }


    }

    private fun getFunctionTags(functions: List<Function>): Map<TrelentTag, List<Function>> {
        val returnObj: EnumMap<TrelentTag, ArrayList<Function>> =
            EnumMap(TrelentTag::class.java);

        TrelentTag.values().forEach{
            returnObj[it] = arrayListOf()
        }

        val mode = TrelentSettingsState.getInstance().settings.mode;
        val regex = "${TrelentTag.AUTO}|${TrelentTag.HIGHLIGHT}|${TrelentTag.IGNORE}g".toRegex()

        functions.stream().forEach {
            var text = it.text;
            if (it.docstring != null) {
                text += it.docstring!!
            }
            //Get tag, and if can't find one, default to settings
            val tag = try {
                when (regex.matchEntire(text)!!.value) {
                    TrelentTag.AUTO.tag -> TrelentTag.AUTO
                    TrelentTag.HIGHLIGHT.tag -> TrelentTag.HIGHLIGHT
                    TrelentTag.IGNORE.tag -> TrelentTag.IGNORE
                    else -> mode
                }
            } catch (e: Exception) {
                mode
            }

            returnObj[tag]?.add(it);

        }
        return returnObj
    }

    override fun dispose() {
    }

}