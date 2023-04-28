package net.trelent.document.services

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.printlnError
import kotlinx.coroutines.*
import net.trelent.document.helpers.*
import net.trelent.document.helpers.Function
import net.trelent.document.listeners.TrelentListeners

@Service(Service.Level.PROJECT)
class CodeParserService(private val project: Project): Disposable {

    companion object{
        fun getInstance(project: Project): CodeParserService {
            return project.service<CodeParserService>();
        }
    }

    private var parseJob: Job? = null;

    init{
        //Connect to range update, should cancel parse job as this can cause race condition
        project.messageBus.connect().subscribe(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE, object: TrelentListeners.RangeUpdateListener{
            override fun rangeUpdate(document: Document) {
                parseJob?.cancel();
            }

        })
    }

    fun runParseJob(document: Document, track: Boolean = true){
        parseJob?.cancel()
        parseJob = GlobalScope.launch{
            parseDocument(document, track);
        }
    }

    private suspend fun parseDocument(document: Document, track: Boolean) {
            try {
                //Get file metadata
                val file = FileDocumentManager.getInstance().getFile(document);
                if (file == null || file.extension == null || getExtensionLanguage(file.extension!!) == null) {
                    return
                }
                val language = getExtensionLanguage(file.extension!!)!!
                val sourceCode = document.text

                //Parse functions
                val functions = parseFunctions(
                    language,
                    sourceCode
                );

                //check if the coroutine is cancelled, if so this will throw an exception
                yield()

                //Check if there are any functions present from the parser, if so track the state
                if (functions.isNotEmpty() && track) {
                    ChangeDetectionService.getInstance(project).trackState(document, functions.toList());
                }

                ApplicationManager.getApplication().invokeLater {
                    //Fire a parse event
                    project.messageBus.syncPublisher(TrelentListeners.ParseListener.TRELENT_PARSE_TRACK_ACTION)
                        .parse(document, language)
                }


            } catch (e: CancellationException) {
                println("Parse task cancelled")
            }
            finally{
            }


    }

    private fun parseFunctions(language: String, source: String): Array<Function> {
        val req = ParsingRequest(language = language, source = source)
        val body = Gson().toJson(req)

        try {
            val returned = sendRequest(body, PARSE_URL).body()
            return Gson().fromJson(returned, Array<Function>::class.java)
        } catch (e: Exception) {
            printlnError(e.message.toString())
        }


        return arrayOf()
    }
    override fun dispose() {
        parseJob?.cancel()
    }
}