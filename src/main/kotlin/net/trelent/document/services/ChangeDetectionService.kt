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
import net.trelent.document.settings.TrelentSettingsState
import org.apache.xmlbeans.impl.common.Levenshtein
import java.math.BigInteger
import java.security.MessageDigest

@Service(Service.Level.PROJECT)

class ChangeDetectionService(private val project: Project): Disposable{
    companion object{


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

    private val openDocuments: HashMap<String, DocumentState> = hashMapOf();

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

        //On a document event, clear the function from the change detection service
        project.messageBus.connect().subscribe(TrelentListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION, object: TrelentListeners.DocumentedListener{
            override fun documented(document: Document, function: Function, language: String) {
                clearDocChange(document, function);
            }

        })
    }

     fun docLoad(document: Document){
             try{
                 CodeParserService.getInstance(project).runParseJob(document)
             }
             finally{}

    }

     fun trackState(doc: Document, functions: List<Function>): HashMap<String, ArrayList<Function>> {
         var updateThese: HashMap<String, ArrayList<Function>>;
         runBlocking{
             functionUpdateBlocker.withLock{
                 val trackID = validateDoc(doc);

                 updateThese = getChangedFunctions(doc, functions);

                 openDocuments[trackID]?.allFunctions = functions

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
        //Get previously stored functions for this document
        val allFunctions = getHistory(doc).allFunctions;

        val returnObj: HashMap<String, ArrayList<Function>> = hashMapOf(Pair("new", arrayListOf()), Pair("deleted", arrayListOf()), Pair("updated", arrayListOf()));

        if(allFunctions.isEmpty()){
            return returnObj
        }
        val idMatching: HashMap<Int, HashMap<String, Function>> = hashMapOf();

        //Add new functions to idMatching map
        functions.forEach{
            val id = it.offsets[0];
            if(!idMatching.containsKey(id)){
                idMatching[id] = hashMapOf();
            }
            idMatching[id]?.set("new", it);
        }

        //Add old functions to idMatching map
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

        val changeThreshold = TrelentSettingsState.getInstance().settings.threshold;

        idMatching.forEach{
            val funcPair = it.value;
            try{
                if(funcPair.containsKey("old")){
                    //If an old function is present
                    if(funcPair.containsKey("new")){
                        //If an old & new function is present

                        //Increment recordedChanges by diff between old & new function
                        funcPair["new"]!!.recordedChanges += funcPair["old"]!!.recordedChanges + compareFunctions(funcPair["new"]!!, funcPair["old"]!!);

                        //If the function is past the update threshold, mark it as updated
                        if(funcPair["new"]!!.recordedChanges >= changeThreshold.threshold){
                            returnObj["updated"]?.add(funcPair["new"]!!);
                        }
                    }
                    else{
                        //If the old function is present, but a new function isn't, mark this function as deleted
                        returnObj["deleted"]?.add(funcPair["old"]!!);
                    }
                }
                else{
                    //If the old function isn't present, the new function must be the only thing present
                    returnObj["new"]?.add(funcPair["new"]!!);
                }
            }
            finally{

            }

        }

        return returnObj;
    }

    private fun addDocChange(doc: Document, func: Function) {
        val docChanges = getDocChanges(doc);
        val funcId = validateFunc(doc, func);
        docChanges[funcId] = func;
    }

    fun clearDocChange(doc: Document, func: Function){
        if(deleteDocChange(doc, func)){
            project.messageBus.syncPublisher(TrelentListeners.ChangeUpdate.TRELENT_CHANGE_UPDATE).changeUpdate(doc);
        }
    }

    private fun deleteDocChange(doc: Document, func: Function): Boolean{
        val funcID = validateFunc(doc, func);
        val changes = getDocChanges(doc);
        if(changes.containsKey(funcID)){
                func.recordedChanges = 0;
                changes.remove(funcID);
                return true;
            }
        return false;
    }

    fun getDocChanges(doc: Document): HashMap<String, Function> {
        val trackID = validateDoc(doc);
        return changedFunctions[trackID]!!;
    }

    fun getHistory(doc: Document): DocumentState {
        val trackID = validateDoc(doc)
        return openDocuments[trackID]!!;
    }

    fun updateFunctionRanges(event: DocumentEvent) {
        val doc = event.document


        //Get offsets & old end index
        val offsetDiff = event.newLength - event.oldLength;
        val oldEndIndex = event.offset + event.oldLength;

        runBlocking{
            functionUpdateBlocker.withLock{

                //Get functions
                val functions = getHistory(doc).allFunctions
                functions.forEach{function ->
                    try{

                        //Get the bottom offset of the function as this will determine whether the diff change
                        //affects this range
                        val bottomOffset = function.offsets[1];

                        //If the change happened above the end index of the function
                        if(oldEndIndex <= bottomOffset){

                            //If there is a docstring, see if those ranges need to be updated
                            if(function.docstring_range_offsets != null){
                                val docRange = function.docstring_range_offsets!!;
                                if(oldEndIndex <= docRange[0]) docRange[0] += offsetDiff
                                if(oldEndIndex <= docRange[1]) docRange[1] += offsetDiff
                            }
                            //see what function ranges need to be updated
                            if(oldEndIndex <= function.docstring_offset) function.docstring_offset += offsetDiff
                            if(oldEndIndex <= function.offsets[0]) function.offsets[0] += offsetDiff
                            if(oldEndIndex <= function.offsets[1]) function.offsets[1] += offsetDiff
                        }

                    }
                    finally{

                    }


                }
                //Reload changedFunction map for this document
                updateChangedFunctionsOffsets(doc);
                //notify of range update
                project.messageBus.syncPublisher(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE).rangeUpdate(doc);
            }
        }
    }

    private fun validateDoc(doc: Document): String {
        //Ensure necessary fields are populated for this document, then return the ID
        val trackID = getDocID(doc);
        if(!openDocuments.containsKey(trackID)){
            openDocuments[trackID] = DocumentState(listOf(), hashMapOf());
        }
        if(!changedFunctions.containsKey(trackID)){
            changedFunctions[trackID] = hashMapOf();
        }
        return trackID;
    }

    private fun validateFunc(doc: Document, func: Function): String{
        validateDoc(doc);
        val funcID = getFuncID(func);
        return funcID
    }

    private fun updateChangedFunctionsOffsets(doc: Document){
        val trackID = validateDoc(doc);

        //Persist changed functions as code below clears the list
        val changedFunctions = changedFunctions[trackID]?.values?.map{
            it
        }

        //Needed to clear list this way to prevent ConcurrentAccessError's
        val iterator = this.changedFunctions[trackID]?.iterator()
        if(iterator != null){
            while(iterator.hasNext()){
                iterator.next();
                iterator.remove();
            }
        }
        //Put updates in the list
        changedFunctions?.forEach{function ->
            this.changedFunctions[trackID]?.put(validateFunc(doc, function), function);
        }


    }

    private fun reloadDocChanges(doc: Document, functions: List<Function>){
        val trackID = validateDoc(doc);
        val changedFunctions = this.changedFunctions[trackID]

        val keys = changedFunctions?.keys?.map{
            it
        }?.toHashSet() ?: return;
        functions.forEach{
            val funcID = validateFunc(doc, it);

            if(keys.contains(funcID)){
                changedFunctions[funcID] = it
            }
        }



    }

    override fun dispose() {
        functionUpdateBlocker.preventFreeze()
        timeout?.cancel();
    }

    data class DocumentState(var allFunctions: List<Function>, var updates: HashMap<String, ArrayList<Function>>);


}
