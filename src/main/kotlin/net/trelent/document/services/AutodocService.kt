package net.trelent.document.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import net.trelent.document.helpers.Function
import net.trelent.document.helpers.parseDocument


interface AutodocService{

}
class AutodocServiceImpl: Disposable {

    val updating: HashSet<Document> = hashSetOf()

    init{
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object: DocumentListener{
            override fun documentChanged(event: DocumentEvent) {
                try{
                    val changeDetectionService = ChangeDetectionService.getInstance()!!;
                    changeDetectionService.updateFunctionRanges(event);
                    super.documentChanged(event)
                }
                finally{}

            }
        }, this)
    }

    fun updateDocstrings(doc: Document){
        try{
            val changeDetectionService = ChangeDetectionService.getInstance()!!
            val editor = EditorFactory.getInstance().allEditors.find{
                it.document == doc
            }!!;
            val parsedFunctions = parseDocument(editor, editor.project!!);

            //TODO: Apply highlights

            val fileHistory = changeDetectionService.getHistory(doc).allFunctions;

            if(updating.contains(doc)){
                return;
            }
            updating.add(doc);

            var functionsToDocument = changeDetectionService.getDocChanges(doc);

            //Get function tags

            val taggedFunctions = getFunctionTags(functionsToDocument.values.toList());



        }finally{

        }



    }

    private fun getFunctionTags(functions: List<Function>): HashMap<Function, TrelentTag>{
        val returnObj: HashMap<Function, TrelentTag> = hashMapOf();

        functions.stream().forEach{
            var text = it.text;
            if(it.docstring != null){
                text += it.docstring!!
            }

            val regex = "${TrelentTag.AUTO}|${TrelentTag.HIGHLIGHT}|${TrelentTag.IGNORE}g".toRegex()
            returnObj[it] = try{
                 when(regex.matchEntire(text)!!.value){
                    TrelentTag.AUTO.toString() -> {
                        TrelentTag.AUTO;
                    }
                    TrelentTag.HIGHLIGHT.toString() -> {
                        TrelentTag.HIGHLIGHT
                    }
                    TrelentTag.IGNORE.toString() -> {
                        TrelentTag.IGNORE
                    }
                    else -> {
                        TrelentTag.NONE
                    }
                }
            }
            catch(e: Exception){
                TrelentTag.NONE
            }

        }
        return returnObj
    }

    override fun dispose() {
    }

}

enum class TrelentTag(s: String) {
    AUTO("@trelent-auto"),
    HIGHLIGHT("@trelent-highlight"),
    IGNORE("@trelent-ignore"),
    NONE("")
}