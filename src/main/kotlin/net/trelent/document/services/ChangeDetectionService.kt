package net.trelent.document.services

import com.intellij.AppTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.trelent.document.helpers.Function
import net.trelent.document.listeners.TrelentListeners
import org.apache.xmlbeans.impl.common.Levenshtein
import java.math.BigInteger
import java.security.MessageDigest

@Service(Service.Level.PROJECT)

class ChangeDetectionService(private val project: Project): Disposable{
    companion object{

        @JvmStatic
        private val LEVENSHTEIN_UPDATE_THRESHOLD = 50;

        @JvmStatic
        private val DELAY = 500L;

        fun getInstance(project: Project): ChangeDetectionService {
            return project.service<ChangeDetectionService>()
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

    private var loadJob: Job? = null;

    private val fileInfo: HashMap<String, DocumentState> = hashMapOf();

    private val changedFunctions: HashMap<String, HashMap<String, Function>> = hashMapOf();

    private val functionUpdateBlocker = Mutex()


    init{

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {

            //On document changed
            override fun documentChanged(event: DocumentEvent) {
                //Call to super (Could be redundant)
                super.documentChanged(event)
                try {
                    val thisProject = FileDocumentManager.getInstance().getFile(event.document)
                        ?.let { ProjectLocator.getInstance().guessProjectForFile(it) }
                    if(thisProject == null || thisProject != project){
                        return;
                    }

                    //Create job on dispatch thread, and cancel old one if it exists
                    try {
                        // *Timeout job*
                        timeout?.cancel();
                        //Get change detection service, and apply range changes
                        updateFunctionRanges(event);
                        //Create new job with an initial delay (essentially a timeout)
                        timeout = GlobalScope.launch {
                            delay(DELAY)
                            try {
                                docLoad(event.document)
                            } finally {}
                        }
                    } finally {}
                } finally {}
            }
        }, this)

        //On document opened/reloaded from disk
        project.messageBus.connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, object: FileDocumentManagerListener {
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

    }

     fun docLoad(document: Document){
             try{
                     CodeParserService.getInstance(project).runParseJob(document)
             }
             finally{}

    }

     fun trackState(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
         var updateThese: HashMap<String, ArrayList<Function>> = hashMapOf();
         runBlocking{
             functionUpdateBlocker.withLock{
                 val trackID = validateDoc(doc);

                 updateThese = getChangedFunctions(doc, functions);

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
             }
         }

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
            return Levenshtein.distance(fun1.body, fun2.body);
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
        runBlocking{
            functionUpdateBlocker.withLock{
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
                project.messageBus.syncPublisher(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE).rangeUpdate(doc);
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
        functionUpdateBlocker.preventFreeze()
    }

    data class DocumentState(var allFunctions: List<Function>, var updates: HashMap<String, ArrayList<Function>>);


}
