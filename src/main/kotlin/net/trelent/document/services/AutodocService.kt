package net.trelent.document.services

import com.intellij.AppTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.printlnError
import kotlinx.coroutines.*
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.getHighlights
import net.trelent.document.helpers.parseDocument
import net.trelent.document.helpers.writeDocstringsFromFunctions
import net.trelent.document.listeners.TrelentListeners
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.settings.TrelentSettingsState.TrelentTag;
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class AutodocService: Disposable {

    private val updating: HashSet<Document> = hashSetOf()
    private val highlights: HashMap<String, ArrayList<RangeHighlighter>> = hashMapOf();
    private val operations: HashMap<String, ArrayList<TrelentAutodocHighlighter>> = hashMapOf()


    private var timeout: Job? = null;
    private var highlightJob: Job? = null;


    val DELAY = 500L;

    init {

        //On document changed
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
                                timeout?.cancel();

                                //Get change detection service, and apply range changes
                                val changeDetectionService = ChangeDetectionService.getInstance();
                                changeDetectionService.updateFunctionRanges(event);

                                //Create new job with an initial delay (essentially a timeout)
                                timeout = GlobalScope.launch {
                                    delay(DELAY)
                                    try {
                                        val edit = EditorFactory.getInstance().allEditors.find {
                                            it.document == event.document
                                        };
                                        if(edit != null){
                                            updateDocstrings(edit)
                                        }
                                    } finally {}
                                }
                            } finally {}
                        }
                    }
                } finally {}
            }
        }, this)

        //On document opened/reloaded from disk
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

        });

        //On range update
        ApplicationManager.getApplication().messageBus.connect().subscribe(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE, object: TrelentListeners.RangeUpdateListener {
            override fun rangeUpdate(editor: Editor) {
                resetHighlights(editor);
            }
        });

        //On parse
        ApplicationManager.getApplication().messageBus.connect().subscribe(TrelentListeners.ParseListener.TRELENT_PARSE_TRACK_ACTION, object: TrelentListeners.ParseListener{
            override fun parse(editor: Editor, language: String, functions: List<Function>) {
                resetHighlights(editor);
            }
        })

        //On documented
        ApplicationManager.getApplication().messageBus.connect().subscribe(TrelentListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION, object: TrelentListeners.DocumentedListener {
            override fun documented(editor: Editor, language: String) {
                resetHighlights(editor);
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

            //If the document is already in the middle of being updated, lets skip it

            if (updating.contains(doc)) {
                return;
            }

            //mark this document as being updated

            updating.add(doc);
            //Parse the document, which will track the history

            parseDocument(editor, editor.project!!);


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
            try {

                //Clear old instances of the highlights
                clearHighlights(editor);
                clearOperations(editor);

                val changes = ChangeDetectionService.getInstance().getDocChanges(editor.document);

                if (changes.size > 0) {

                    //If there are changes, get the functions that are tagged to be highlighted
                    val highlightFunctions = getFunctionTags(changes.values.toList())[TrelentTag.HIGHLIGHT];

                    if(highlightFunctions != null){
                        //Cancel the job pre-emptively, so that any jobs that are currently running, will be cancelled
                        if(highlightJob != null){
                            highlightJob?.cancel();
                        }
                        ApplicationManager.getApplication().invokeLater{
                            //Then, cancel the job when this runnable is called. This makes it so any invokations called after this, will be cancelled
                            if(highlightJob != null){
                                highlightJob?.cancel();
                            }
                            highlightJob = runBlocking{
                                launch{
                                    applyHighlights(editor, highlightFunctions)
                                    createOperations(editor, highlightFunctions);
                                }
                            }

                        }

                    }

                }
            } finally {}

    }

    private fun applyHighlights(editor: Editor, functions: List<Function>){
        ApplicationManager.getApplication().invokeLater{
            val highlights = getHighlights(editor, functions);

            val docID = ChangeDetectionService.getDocID(editor.document);

            this.highlights[docID] = highlights;
        }

    }

    private fun createOperations(editor: Editor, functions: List<Function>){
        val ops: ArrayList<TrelentAutodocHighlighter> = arrayListOf();
        val docID = ChangeDetectionService.getDocID(editor.document);
        functions.forEach{function ->
            try{
                ops.add(TrelentAutodocHighlighter.TrelentAccept(editor, function))
                ops.add(TrelentAutodocHighlighter.TrelentIgnore(editor, function))
            }
            catch(ill: IllegalStateException){
                printlnError("Issue creating operations for function with offsets: ${function.offsets}, on document with length ${editor.document.text.length}")
            }
            finally{}

        };
        operations[docID] = ops;
    }

    private fun clearOperations(editor: Editor){
        val docID = ChangeDetectionService.getDocID(editor.document);

        if(operations[docID] != null){
            val ops = operations[docID]?.map{
                it
            }
            operations[docID]?.clear();
            ops?.forEach{ gutterIcon ->
                ApplicationManager.getApplication().invokeLater{
                    gutterIcon.dispose();
                }
            }

        }

    }

    private fun clearHighlights(editor: Editor){
        val trackID = ChangeDetectionService.getDocID(editor.document);
        highlights[trackID]?.forEach{
            ApplicationManager.getApplication().invokeLater{
                it.dispose();
            }

        }
    }

    override fun dispose() {
        highlights.values.forEach{
            it.forEach{highlight ->
                ApplicationManager.getApplication().invokeLater{
                    highlight.dispose();
                }
            }
        }
        operations.values.forEach{
            it.forEach{operation ->
                ApplicationManager.getApplication().invokeLater{
                    operation.dispose();
                }
            }
        }
        timeout?.cancel();
    }

}