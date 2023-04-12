package net.trelent.document.helpers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import net.trelent.document.actions.getFormat
import net.trelent.document.services.ChangeDetectionService
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.listeners.TrelentListeners
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList


fun writeDocstringsFromFunctions(functions: List<Function>, editor: Editor, project: Project){

    try {

        val document: Document = editor.document
        val file = FileEditorManager.getInstance(project).selectedFiles[0]
        var language = getExtensionLanguage(file.extension!!)!!

        val task = object : Task.Backgroundable(project, "Writing docstrings") {
            override fun run(indicator: ProgressIndicator) {

                indicator.text = "Writing docstrings..."
                indicator.isIndeterminate = true


                //FIXME: Remove this when typescript support added in backend
                if (language == "typescript") {
                    language = "javascript"
                }

                val userId = System.getProperty("user.name")

                // Get the docstring format
                val settings = TrelentSettingsState.getInstance().settings
                val format = getFormat(language, settings);

                val futures = functions.stream().map {
                    // We got the current function!
                    val funcName = it.name
                    val funcParams = it.params
                    val funcText = it.text
                    // Request a docstring
                    CompletableFuture.supplyAsync {
                        getDocstring(
                            format,
                            language,
                            funcName,
                            funcParams,
                            "ext-intellij",
                            funcText,
                            userId
                        )
                    }
                }.toList()

                val docstrings: HashMap<Function, DocstringResponse> = hashMapOf();

                futures.zip(functions).forEach {
                    try {
                        val future = it.first
                        val function = it.second;
                        future.join();
                        val docstring = future.get();
                        if (!docstring.successful) {
                            println(docstring.error_type);
                        } else {
                            docstrings[function] = docstring;
                        }
                    } finally {

                    }

                };

                ApplicationManager.getApplication().invokeLater {

                    docstrings.forEach {
                        val currentFunction = it.key;
                        val docstring = it.value;
                        // Get docstring and related metadata
                        val docStringOffset = currentFunction.docstring_offset
                        val docStringColumn = editor.offsetToVisualPosition(docStringOffset).column;

                        val docStringSplit = docstring.data!!.docstring.trim().split("\n");
                        val docStringHead = docStringSplit[0];
                        val docStringBody = (docStringSplit.subList(1, docStringSplit.size)
                            .joinToString("\n") + "\n").prependIndent(" ".repeat(docStringColumn))
                        val docstringText = docStringHead + "\n" + docStringBody;

                        // Insert the docstring
                        WriteCommandAction.runWriteCommandAction(project) {
                            document.insertString(docStringOffset, docstringText)
                        }

                        // Update docs progress
                        val publisher =
                            project.messageBus.syncPublisher(TrelentListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION);
                        publisher.documented(editor, language);
                    }


                }
            }
        }
        ProgressManager.getInstance().run(task)
    }
    finally{

    }

}

fun parseDocument(editor: Editor, project: Project, track: Boolean = true): Array<Function> {
    val functions = try{
        val document: Document = editor.document
        val sourceCode = document.text
        val file = FileEditorManager.getInstance(project).selectedFiles[0]
        val language = getExtensionLanguage(file.extension!!)!!

        parseFunctions(
            language,
            sourceCode
        );
    }
    catch(_: Exception){
        arrayOf();
    }

    if(functions.isNotEmpty() && track){
        val changeDetectionService = ChangeDetectionService.getInstance();
        changeDetectionService.trackState(editor.document, functions.toList());

    }
    return functions;


}