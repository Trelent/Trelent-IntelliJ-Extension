package net.trelent.document.services

import com.intellij.AppTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.parseDocument
import org.apache.xmlbeans.impl.common.Levenshtein
import java.math.BigInteger
import java.security.MessageDigest

interface ChangeDetectionService: Disposable{
    fun trackState(doc: Document, functions: List<Function> = listOf()): HashMap<String, ArrayList<Function>>;

    fun getChangedFunctions(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>>

    fun getDocChanges(doc: Document): HashMap<String, Function>

    fun getHistory(doc: Document): ChangeDetectionServiceImpl.DocumentState

    fun updateFunctionRanges(event: DocumentEvent);

    companion object{
        fun getInstance(): ChangeDetectionService? {
            return ApplicationManager.getApplication().getService(ChangeDetectionService::class.java);
        }
    }

    override fun dispose(){
    }

}
class ChangeDetectionServiceImpl: ChangeDetectionService {

    private val fileInfo: HashMap<String, DocumentState> = hashMapOf();

    private val changedFunctions: HashMap<String, HashMap<String, Function>> = hashMapOf();

    private val LEVENSHTEIN_UPDATE_THRESHOLD = 50;

    data class DocumentState(var allFunctions: List<Function>, var updates: HashMap<String, ArrayList<Function>>);

    init{
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(AppTopics.FILE_DOCUMENT_SYNC, object: FileDocumentManagerListener{
            override fun fileContentLoaded(file: VirtualFile, document: Document) {
                try{
                    ProjectManager.getInstance().openProjects.filter{
                        FileEditorManager.getInstance(it).openFiles.contains(file);
                    }
                        .forEach{
                            EditorFactory.getInstance().allEditors.filter{editor ->
                                editor.document == document
                            }.forEach{editor ->
                                parseDocument(editor, it, true);
                            }
                        }

                }
                finally{}
            }
        });
    }
    override fun trackState(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
        val trackID = validateDoc(doc);

        val updateThese = getChangedFunctions(doc, functions);

        updateThese["deleted"]?.forEach{
            deleteDocChange(doc, it);
        };

        updateThese.keys.stream().filter{
            it != "deleted";
        }.flatMap {
            updateThese[it]?.stream();
        }.forEach{
            addDocChange(doc, it);
        }

        fileInfo[trackID] = DocumentState(functions, updateThese)
        return updateThese;

    }

    override fun getChangedFunctions(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
        val allFunctions = getHistory(doc).allFunctions;

        val returnObj: HashMap<String, ArrayList<Function>> = hashMapOf(Pair("new", arrayListOf()), Pair("deleted", arrayListOf()), Pair("updated", arrayListOf()));

        val idMatching: HashMap<Int, HashMap<String, Function>> = hashMapOf();

        functions.forEach{
            val id = it.offsets[0];
            if(!idMatching.containsKey(id)){
                idMatching[id] = hashMapOf();
            }
            idMatching[id]?.set("new", it);
        }

        allFunctions.forEach{
            val id = it.offsets[0];
            if(!idMatching.containsKey(id)){
                idMatching[id] = hashMapOf();
            }
            idMatching[id]?.set("old", it);
        }

        fun compareFunctions(fun1: Function, fun2: Function): Int {
            return Levenshtein.distance(fun1.body, fun2.body);
        }

        idMatching.forEach{
            val funcPair = it.value;
            if(funcPair.containsKey("old")){
                if(funcPair.containsKey("new")){
                    if(compareFunctions(funcPair["new"]!!, funcPair["old"]!!) >=LEVENSHTEIN_UPDATE_THRESHOLD){
                        returnObj["updated"]?.add(funcPair["new"]!!);
                    }
                }
                else{
                    returnObj["deleted"]?.add(funcPair["old"]!!);
                }
            }
            else{
                returnObj["new"]?.add(funcPair["new"]!!);
            }
        }

        return returnObj;
    }

    private fun addDocChange(doc: Document, func: Function) {
        val docChanges = getDocChanges(doc);
        val funcId = getFuncID(func);
        docChanges[funcId] = func;
    }

    private fun deleteDocChange(doc: Document, func: Function){
        val funcID = getFuncID(func);
        val changes = getDocChanges(doc);
        changes.remove(funcID);
    }

    override fun getDocChanges(doc: Document): HashMap<String, Function> {
        val trackID = validateDoc(doc);
        return changedFunctions[trackID]!!;
    }

    override fun getHistory(doc: Document): DocumentState {
        val trackID = validateDoc(doc)
        return fileInfo[trackID]!!;
    }

    override fun updateFunctionRanges(event: DocumentEvent) {
        TODO("Not yet implemented")
    }

    private fun validateDoc(doc: Document): String {
        val trackID = getDocID(doc);
        if(!fileInfo.containsKey(trackID)){
            fileInfo[trackID] = DocumentState(listOf(), hashMapOf());
        }
        if(!changedFunctions.containsKey(trackID)){
            changedFunctions[trackID] = hashMapOf();
        }
        return trackID;
    }



    private fun getDocID(doc: Document): String {
        val input = FileDocumentManager.getInstance().getFile(doc)?.path
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input?.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun getFuncID(func: Function): String {
        val input = func.offsets[0].toString();
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')

    }


}
