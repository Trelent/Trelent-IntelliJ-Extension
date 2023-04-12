package net.trelent.document.services

import com.intellij.AppTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
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
            //On document changed
            override fun documentChanged(event: DocumentEvent) {
                try {
                    //Call to super (Could be redundant)
                    super.documentChanged(event)

                    //Grab instance of editor
                    val editor = EditorFactory.getInstance().allEditors.find {
                        it.document == event.document
                    };

                    //Ensure editor is valid
                    if (editor != null) {

                        //Create job on dispatch thread, and cancel old one if it exists
                        ApplicationManager.getApplication().invokeLater {
                            try {

                                // *Timeout job*
                                job?.cancel();

                                //Get change detection service, and apply range changes
                                val changeDetectionService = ChangeDetectionService.getInstance();
                                changeDetectionService.updateFunctionRanges(event);

                                //Create new job with an initial delay (essentially a timeout)
                                job = GlobalScope.launch {
                                    delay(DELAY)
                                    try {
                                        val edit = EditorFactory.getInstance().allEditors.find {
                                            it.document == event.document
                                        };

                                        if(edit != null){
                                            updateDocstrings(editor)
                                        }


                                    } finally {}
                                }
                            } finally {}
                        }
                    }
                } finally {}
            }
        }, this)

        ApplicationManager.getApplication().messageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, object: FileDocumentManagerListener {

            //When a new file is opened
            override fun fileContentLoaded(file: VirtualFile, document: Document) {
                //Call to super, just for safety
                super.fileContentLoaded(file, document)
                docLoad(document);
            }

            //When a file is reloaded from disk
            override fun fileContentReloaded(file: VirtualFile, document: Document) {
                //call to super for safety
                super.fileContentReloaded(file, document)
                docLoad(document)
            }

        })
    }

    fun docLoad(document: Document){
        try{

            //Attempt to locate editor
            val editor = EditorFactory.getInstance().allEditors.find {
                it.document == document
            };

            //If we found the editor, and the project is valid, then proceed
            if(editor != null && editor.project != null){
                parseDocument(editor, editor.project!!)
            }

        }
        finally{}
    }

    fun updateDocstrings(editor: Editor) {
        val doc = editor.document;

        //Ensure project is valid
        if(editor.project == null){
            return;
        }
        try {

            //Parse the document, which will track the history

            parseDocument(editor, editor.project!!);

            //apply highlights to changed functions

            resetHighlights(editor)

            //If the document is already in the middle of being updated, lets skip it

            if (updating.contains(doc)) {
                return;
            }

            //mark this document as being updated

            updating.add(doc);

            //Get changed functions

            val changeDetectionService = ChangeDetectionService.getInstance()

            changeDetectionService.getHistory(doc).allFunctions;

            val functionsToDocument = changeDetectionService.getDocChanges(doc);

            //Get function tags, as a hashmap

            val taggedFunctions = getFunctionTags(functionsToDocument.values.toList());

            //If there are no updates, return
            if(taggedFunctions.values.stream().flatMap{
                it.stream()
                }.count() == 0L){
                updating.remove(doc);
                return;
            }

            //Get functions tagged as AUTO
            val autoFunctions = taggedFunctions[TrelentTag.AUTO]!!;

            //Write docstrings for those functions
            writeDocstringsFromFunctions(autoFunctions, editor, editor.project!!);

        } finally {
            //Apply highlights based off the editor, and clear the doc from the updating set
            resetHighlights(editor);
            updating.remove(doc);
        }


    }

    private fun getFunctionTags(functions: List<Function>): Map<TrelentTag, List<Function>> {
        val returnObj: EnumMap<TrelentTag, ArrayList<Function>> =
            EnumMap(TrelentTag::class.java);

        //initialize the enum map with an array list at each index
        TrelentTag.values().forEach{
            returnObj[it] = arrayListOf()
        }

        //Get the default mode specified in the user settings, and create the regex to find tags in the code
        val mode = TrelentSettingsState.getInstance().settings.mode;
        val regex = "${TrelentTag.AUTO}|${TrelentTag.HIGHLIGHT}|${TrelentTag.IGNORE}g".toRegex()

        //For each function, get the text, and append the docstrings if they exist
        functions.stream().forEach {
            var text = it.text;
            if (it.docstring != null) {
                text += it.docstring
            }
            //Get tag, and if can't find one, set to settings default
            val tag = try {
                when (regex.matchEntire(text)?.value) {
                    TrelentTag.AUTO.tag -> TrelentTag.AUTO
                    TrelentTag.HIGHLIGHT.tag -> TrelentTag.HIGHLIGHT
                    TrelentTag.IGNORE.tag -> TrelentTag.IGNORE
                    else -> mode
                }
            } catch (e: Exception) {
                mode
            }

            //Add the function to the list for the given tag

            returnObj[tag]?.add(it);

        }
        return returnObj
    }


    private fun resetHighlights(editor: Editor){
        ApplicationManager.getApplication().invokeLater {
            try {

                //Clear old instances of the highlights
                clearHighlights(editor);
                clearOperations(editor);

                val changes = ChangeDetectionService.getInstance().getDocChanges(editor.document);

                if (changes.size > 0) {

                    //If there are changes, get the functions that are tagged to be highlighted
                    val highlightFunctions = getFunctionTags(changes.values.toList())[TrelentTag.HIGHLIGHT];

                    if(highlightFunctions != null){
                        applyHighlights(editor, highlightFunctions)
                        createOperations(editor, highlightFunctions);
                    }

                }
            } finally {}
        }

    }

    private fun applyHighlights(editor: Editor, functions: List<Function>){
            val highlights = getHighlights(editor, functions);

            val docID = ChangeDetectionService.getDocID(editor.document);

            this.highlights[docID] = highlights;
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
        operations[docID]?.clear();
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
        operations.values.forEach{
            it.forEach{operation ->
                operation.dispose();
            }
        }
        job?.cancel();
    }

}