package net.trelent.document.services

import com.intellij.openapi.Disposable
import kotlinx.coroutines.selects.select
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import net.trelent.document.helpers.getExtensionLanguage
import net.trelent.document.helpers.parseFunctions
import net.trelent.document.listeners.TrelentListeners

@Service(Service.Level.PROJECT)
class CodeParserService(private val project: Project): Disposable {

    companion object{
        fun getInstance(project: Project): CodeParserService {
            return project.service<CodeParserService>();
        }
    }

    private var parseIndicator: ProgressIndicator? = null;

    init{
        project.messageBus.connect().subscribe(TrelentListeners.RangeUpdateListener.TRELENT_RANGE_UPDATE, object: TrelentListeners.RangeUpdateListener{
            override fun rangeUpdate(document: Document) {
                parseIndicator?.cancel();
            }

        })
    }

    fun parseDocument(document: Document, track: Boolean = true) {
                val task = object : Task.Backgroundable(project, "Parsing Document...", true){
                    override fun run(progressIndicator: ProgressIndicator){
                        parseIndicator = progressIndicator
                        runBlocking{
                            @Suppress("HardCodedStringLiteral")
                            val execution = launch{
                                try{
                                    val file = FileDocumentManager.getInstance().getFile(document);
                                    if(file == null || file.extension == null || getExtensionLanguage(file.extension!!) == null){
                                        return@launch
                                    }
                                    val language = getExtensionLanguage(file.extension!!)!!
                                    val functions = try{
                                        val sourceCode = document.text
                                        parseFunctions(
                                            language,
                                            sourceCode
                                        );
                                    }
                                    catch(_: Exception){
                                        arrayOf();
                                    }

                                    if(functions.isNotEmpty() && track) {
                                        val changeDetectionService = ChangeDetectionService.getInstance(project);
                                        changeDetectionService.trackState(document, functions.toList());
                                    }
                                    project.messageBus.syncPublisher(TrelentListeners.ParseListener.TRELENT_PARSE_TRACK_ACTION).parse(document, language, functions.toList())

                                }
                                catch(e: Exception){
                                    println("Parse task cancelled")
                                }
                            }
                            val cancellation = launch{ progressIndicator.awaitCancellation()}
                            launch{
                                select {
                                    execution.onJoin{
                                        cancellation.cancel();
                                    }
                                    cancellation.onJoin{
                                        execution.cancel();
                                    }
                                }
                            }
                        }
                    }

                }
                parseIndicator?.cancel()

                ProgressManager.getInstance().run(task);

    }

    private suspend fun ProgressIndicator.awaitCancellation() {
        while (currentCoroutineContext().isActive) {
            if (isCanceled) return
            delay(50)
        }
    }

    override fun dispose() {
    }
}