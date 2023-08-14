package net.trelent.document.services

import com.intellij.AppTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.parseDocument
import java.math.BigInteger
import java.security.MessageDigest


class ChangeDetectionService: Disposable{
    companion object{

        @JvmStatic
        private val LEVENSHTEIN_UPDATE_THRESHOLD = 50;

        fun getInstance(): ChangeDetectionService {
            return service<ChangeDetectionService>()
        }

        fun getDocID(doc: Document): String {
            val input = FileDocumentManager.getInstance().getFile(doc)?.path
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(input?.toByteArray())).toString(16).padStart(32, '0')
        }

        fun getFuncID(func: Function): String {
            val input = func.offsets[0].toString();
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')

        }
    }

    private var timeout: Job? = null;

    private val DELAY = 500L;

    private val fileInfo: HashMap<String, DocumentState> = hashMapOf();

    private val changedFunctions: HashMap<String, HashMap<String, Function>> = hashMapOf();

    val parseBlocker = Mutex()

    val rangeBlocker = Mutex();

    init{
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            //On document changed
            override fun documentChanged(event: DocumentEvent) {
                try {
                    //Call to super (Could be redundant)
                    super.documentChanged(event)
                        //Create job on dispatch thread, and cancel old one if it exists
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                // *Timeout job*
                                timeout?.cancel();
                                //Get change detection service, and apply range changes
                                updateFunctionRanges(event);
                                //Create new job with an initial delay (essentially a timeout)
                                timeout = GlobalScope.launch {
                                    delay(DELAY)
                                    try {
                                        val file = FileDocumentManager.getInstance().getFile(event.document)
                                        if(file != null){
                                            docLoad(event.document, file)
                                        }
                                    } finally {}
                                }
                            } finally {}
                        }
                } finally {}
            }
        }, this)
        //On document opened/reloaded from disk
        ApplicationManager.getApplication().messageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, object:
            FileDocumentManagerListener {
            //When a new file is opened
            override fun fileContentLoaded(file: VirtualFile, document: Document) {
                //Call to super, just for safety
                super.fileContentLoaded(file, document)
                docLoad(document, file);
            }
            //When a file is reloaded from disk
            override fun fileContentReloaded(file: VirtualFile, document: Document) {
                //call to super for safety
                super.fileContentReloaded(file, document)
                docLoad(document, file)
            }
        });
    }

    fun docLoad(document: Document, file: VirtualFile){
        try{
            //Attempt to locate editor
            val project = ProjectLocator.getInstance().guessProjectForFile(file);
            //If we found the editor, and the project is valid, then proceed
            if(project != null){
                parseDocument(document, project)
            }
        }
        finally{}
    }

    data class DocumentState(var allFunctions: List<Function>, var updates: HashMap<String, ArrayList<Function>>);

     fun trackState(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
        val trackID = validateDoc(doc);

        val updateThese = getChangedFunctions(doc, functions);

         fileInfo[trackID]?.allFunctions = functions

         //Delete deleted functions
        updateThese["deleted"]?.forEach{
            deleteDocChange(doc, it);
        };

         //add new updates
         updateThese.keys.stream().filter{
             it != "deleted";
         }.map{
             updateThese[it]!!
         }.flatMap{
             it.stream()
         }.forEach{
             addDocChange(doc, it);
         }



         //Reload doc changes
         reloadDocChanges(doc, functions);

        return updateThese;

    }

    private fun getChangedFunctions(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
        val allFunctions = getHistory(doc).allFunctions;

        val returnObj: HashMap<String, ArrayList<Function>> = hashMapOf(Pair("new", arrayListOf()), Pair("deleted", arrayListOf()), Pair("updated", arrayListOf()));

        if(allFunctions.isEmpty()){
            return returnObj
        }
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
            return levenshteinDistance(fun1.body, fun2.body);
        }

        idMatching.forEach{
            val funcPair = it.value;
            if(funcPair.containsKey("old")){
                if(funcPair.containsKey("new")){
                    if(compareFunctions(funcPair["new"]!!, funcPair["old"]!!) >= LEVENSHTEIN_UPDATE_THRESHOLD){
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

    fun getDocChanges(doc: Document): HashMap<String, Function> {
        val trackID = validateDoc(doc);
        return changedFunctions[trackID]!!;
    }

    fun getHistory(doc: Document): DocumentState {
        val trackID = validateDoc(doc)
        return fileInfo[trackID]!!;
    }

    fun updateFunctionRanges(event: DocumentEvent) {
        val doc = event.document

        val offsetDiff = event.newLength - event.oldLength;
        val oldEndIndex = event.offset + event.oldLength;
        ApplicationManager.getApplication().invokeLater{
            runBlocking{
                rangeBlocker.withLock{
                    parseBlocker.withLock{
                        val functions = getHistory(doc).allFunctions
                        functions.forEach{function ->
                            try{
                                val bottomOffset = function.offsets[1];

                                if(oldEndIndex <= bottomOffset){
                                    if(function.docstring_range_offsets != null){
                                        val docRange = function.docstring_range_offsets!!;
                                        if(oldEndIndex <= docRange[0]) docRange[0] += offsetDiff
                                        if(oldEndIndex <= docRange[1]) docRange[1] += offsetDiff
                                    }
                                    if(oldEndIndex <= function.docstring_offset) function.docstring_offset += offsetDiff
                                    if(oldEndIndex <= function.offsets[0]) function.offsets[0] += offsetDiff
                                    if(oldEndIndex <= function.offsets[1]) function.offsets[1] += offsetDiff
                                }
                            }
                            finally{

                            }


                        }
                        refreshDocChanges(doc);
                    }
                }


            }
        }


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

    private fun refreshDocChanges(doc: Document){
        val trackID = validateDoc(doc);

        val changedFunctions = changedFunctions[trackID]?.values?.map{
            it
        }
        val iterator = this.changedFunctions[trackID]?.iterator()
        //Need this block to fix concurrency problems
        if(iterator != null){
            while(iterator.hasNext()){
                iterator.next();
                iterator.remove();
            }
        }
        this.changedFunctions[trackID]?.iterator()?.forEach{

        }
        changedFunctions?.forEach{function ->
            this.changedFunctions[trackID]?.put(getFuncID(function), function);
        }


    }

    private fun reloadDocChanges(doc: Document, functions: List<Function>){
        val trackID = validateDoc(doc);
        val changedFunctions = this.changedFunctions[trackID]


            val keys = changedFunctions!!.keys.map{
                it
            }.toHashSet()
            functions.forEach{
                val funcID = getFuncID(it);

                if(keys.contains(funcID)){
                    changedFunctions[funcID] = it
                }
        }



    }

    override fun dispose() {
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val lenStr1 = str1.length
        val lenStr2 = str2.length

        val dp = Array(lenStr1 + 1) { IntArray(lenStr2 + 1) }

        for (i in 0..lenStr1) {
            for (j in 0..lenStr2) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    str1[i - 1] == str2[j - 1] -> dp[i][j] = dp[i - 1][j - 1]
                    else -> dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        println("Dist is ${dp[lenStr1][lenStr2]}");
        return dp[lenStr1][lenStr2]
    }


}
