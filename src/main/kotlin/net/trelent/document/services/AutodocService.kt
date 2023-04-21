package net.trelent.document.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.printlnError
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.getHighlights
import net.trelent.document.listeners.TrelentListeners
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@Service(Service.Level.PROJECT)
class AutodocService(val project: Project): Disposable {

    private val updating: HashSet<Document> = hashSetOf()
    private val highlights: HashMap<String, ArrayList<RangeHighlighter>> = hashMapOf();
    private val operations: HashMap<String, ArrayList<TrelentAutodocHighlighter>> = hashMapOf()

    init{
        project.messageBus.connect().subscribe(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE, object: TrelentListeners.RangeUpdateListener{
            override fun rangeUpdate(document: Document) {
                val editor = EditorFactory.getInstance().allEditors.find{
                    it.document == document
                } ?: return
                resetHighlights(editor)
            }

        });

        project.messageBus.connect().subscribe(TrelentListeners.ParseListener.TRELENT_PARSE_TRACK_ACTION, object: TrelentListeners.ParseListener {
            override fun parse(document: Document, language: String, functions: List<Function>) {
                val editor = EditorFactory.getInstance().allEditors.find{
                    it.document == document
                } ?: return
                resetHighlights(editor)


            }

        });
    }

    private fun getFunctionTags(functions: List<Function>): Map<TrelentSettingsState.TrelentTag, List<Function>> {
        val returnObj: EnumMap<TrelentSettingsState.TrelentTag, ArrayList<Function>> =
            EnumMap(TrelentSettingsState.TrelentTag::class.java);

        //initialize the enum map with an array list at each index
        TrelentSettingsState.TrelentTag.values().forEach{
            returnObj[it] = arrayListOf()
        }

        //Get the default mode specified in the user settings, and create the regex to find tags in the code
        val mode = TrelentSettingsState.getInstance().settings.mode;
        val regex = "${TrelentSettingsState.TrelentTag.AUTO}|${TrelentSettingsState.TrelentTag.HIGHLIGHT}|${TrelentSettingsState.TrelentTag.IGNORE}g".toRegex()

        //For each function, get the text, and append the docstrings if they exist
        functions.stream().forEach {
            var text = it.text;
            if (it.docstring != null) {
                text += it.docstring
            }
            //Get tag, and if can't find one, set to settings default
            val tag = try {
                when (regex.matchEntire(text)?.value) {
                    TrelentSettingsState.TrelentTag.AUTO.tag -> TrelentSettingsState.TrelentTag.AUTO
                    TrelentSettingsState.TrelentTag.HIGHLIGHT.tag -> TrelentSettingsState.TrelentTag.HIGHLIGHT
                    TrelentSettingsState.TrelentTag.IGNORE.tag -> TrelentSettingsState.TrelentTag.IGNORE
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

            val changes = ChangeDetectionService.getInstance(project).getDocChanges(editor.document);

            if (changes.size > 0) {

                //If there are changes, get the functions that are tagged to be highlighted
                val highlightFunctions = getFunctionTags(changes.values.toList())[TrelentSettingsState.TrelentTag.HIGHLIGHT];

                if(highlightFunctions != null){
                    applyHighlights(editor, highlightFunctions)
                    createOperations(editor, highlightFunctions);

                }

            }
        } finally {}

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
    }
}