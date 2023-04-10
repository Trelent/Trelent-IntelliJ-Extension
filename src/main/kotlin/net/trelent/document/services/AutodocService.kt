package net.trelent.document.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.getHighlights
import net.trelent.document.helpers.parseDocument
import net.trelent.document.helpers.writeDocstringsFromFunctions
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.settings.TrelentSettingsState.TrelentTag;
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter
import net.trelent.document.ui.highlighters.TrelentGutterRenderer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class AutodocService: Disposable {

    private val updating: HashSet<Document> = hashSetOf()
    private val highlights: HashMap<String, ArrayList<RangeHighlighter>> = hashMapOf();
    private val operations: HashMap<String, ArrayList<TrelentAutodocHighlighter>> = hashMapOf()
    var job: Job? = null;

    val DELAY = 500L;

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = try { super.documentChanged(event)
            ApplicationManager.getApplication().invokeLater{
                val changeDetectionService = ChangeDetectionService.getInstance();
                changeDetectionService.updateFunctionRanges(event);

                if(job != null){
                    job!!.cancel();
                }
                    job = GlobalScope.launch{
                            delay(DELAY)
                            try{
                                updateDocstrings(event.document)
                            }
                            finally{

                            }

                    }


            }

        } finally {

        }
        }, this)
    }

    fun updateDocstrings(doc: Document) {

        try {
            val editor = EditorFactory.getInstance().allEditors.find {
                it.document == doc
            } ?: return;
            val changeDetectionService = ChangeDetectionService.getInstance()

            parseDocument(editor, editor.project!!);

            applyHighlights(editor)

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
            applyHighlights(editor);

        } finally {

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

    fun applyHighlights(editor: Editor){
        try{
            val changes = ChangeDetectionService.getInstance().getDocChanges(editor.document);

            if(changes.size == 0){
                return;
            }
            val highlightFunctions = getFunctionTags(changes.values.toList())[TrelentTag.HIGHLIGHT]!!;

            resetHighlights(editor, highlightFunctions)
        }
        finally{

        }

    }

    fun resetHighlights(editor: Editor, functions: List<Function>){
        ApplicationManager.getApplication().invokeLater{
            clearHighlights(editor);
            clearOperations(editor)
            val highlights = getHighlights(editor, functions);

            val docID = ChangeDetectionService.getDocID(editor.document);


            this.highlights[docID] = highlights;

            createOperations(editor, functions);
        }

    }

    private fun createOperations(editor: Editor, functions: List<Function>){
        val ops: ArrayList<TrelentAutodocHighlighter> = arrayListOf();
        val docID = ChangeDetectionService.getDocID(editor.document);
        functions.forEach{function ->
            ops.add(TrelentAutodocHighlighter.TrelentAccept(editor, function))
            ops.add(TrelentAutodocHighlighter.TrelentIgnore(editor, function))
        }

        operations[docID] = ops;

    }

    private fun clearOperations(editor: Editor){
        val docID = ChangeDetectionService.getDocID(editor.document);

        operations[docID]?.forEach{ gutterIcon ->
            gutterIcon.dispose();
        }
        operations.remove(docID);
    }

    private fun clearHighlights(editor: Editor){
        val trackID = ChangeDetectionService.getDocID(editor.document);

        highlights[trackID]?.forEach{
            it.dispose();
        }
    }

    override fun dispose() {
        highlights.values.forEach{
            it.forEach{highlight ->
                highlight.dispose()
            }
        }
        job?.cancel();
    }

}