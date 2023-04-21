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
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.getHighlights
import net.trelent.document.listeners.TrelentListeners
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter
import java.util.HashMap

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
                applyHighlights(editor, ChangeDetectionService.getInstance(project).getDocChanges(document).values.map{it}.toList())
            }

        });

        project.messageBus.connect().subscribe(TrelentListeners.ParseListener.TRELENT_PARSE_TRACK_ACTION, object: TrelentListeners.ParseListener {
            override fun parse(document: Document, language: String, functions: List<Function>) {
                val editor = EditorFactory.getInstance().allEditors.find{
                    it.document == document
                } ?: return
                applyHighlights(editor, ChangeDetectionService.getInstance(project).getDocChanges(document).values.map{it}.toList())


            }

        });
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