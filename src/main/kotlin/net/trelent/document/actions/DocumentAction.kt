package net.trelent.document.actions


import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.printlnError
import net.trelent.document.helpers.*
import net.trelent.document.helpers.Function
import net.trelent.document.services.ChangeDetectionService
import net.trelent.document.settings.TrelentSettingsState
import org.jetbrains.annotations.NotNull

class DocumentAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(@NotNull e: AnActionEvent) {

        try {
            // Get the open project and editor, if there is one
            val project: Project = e.getRequiredData(CommonDataKeys.PROJECT)
            val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
            if (editor == null) {
                showError(
                    "No text editor is open! To write a docstring, you must have a text editor open and click within somewhere on a function.",
                    project
                )
                return
            }

            // Get the editor's contents, language, and cursor
            val document: Document = editor.document
            val cursor: Caret = editor.caretModel.currentCaret
            val file = FileEditorManager.getInstance(project).selectedFiles[0]
            var language = getExtensionLanguage(file.extension!!)!!

            // Get a user id for the user on this machine
            val userId = System.getProperty("user.name")

            // Check if this is a supported language
            if (!SUPPORTED_LANGUAGES.contains(language)) {
                showError(
                    "We do not support this language yet. Currently supported languages include C#, Java, JavaScript and Python.",
                    project
                )
                return
            }

            val parsedFunctions = ChangeDetectionService.getInstance(project).getHistory(document).allFunctions;

            var offset = 0
            ApplicationManager.getApplication().runReadAction {
                offset = cursor.offset
            }

            val currentFunction = getCurrentFunction(parsedFunctions, offset)
            if (currentFunction == null) {
                showError(
                    "Your cursor is not inside a valid function. Please click inside the function body if you have not already.",
                    project
                )
                return
            }

            writeDocstringsFromFunctions(listOf(currentFunction), editor, project);
        }
        catch(e: Exception){
            printlnError("Error parsing: ${e.stackTraceToString()}")
        }
    }

    fun getCurrentFunction(functions: List<Function>, offset: Int): Function?{
        val within: ArrayList<Function> = arrayListOf()

        functions.forEach{
            if(it.offsets[0] <= offset && it.offsets[1] >= offset){
                within.add(it)
            }
        }
        val bestChoice = within.stream().min(Comparator.comparing{
            it.offsets[1] - it.offsets[0]
        })
        if(bestChoice.isEmpty){
            return null
        }
        return bestChoice.get()
    }

}

fun showError(message: String, project: Project) {
    val errNotification = Notification(
        "Trelent Error Notification Group",
        "Error writing docstring",
        message,
        NotificationType.ERROR
    )
    Notifications.Bus.notify(errNotification, project)
}

fun getFormat(language: String, settings: TrelentSettingsState.TrelentSettings): String
{
    return when (language) {
        "csharp" -> {
            settings.csharpFormat
        }
        "java" -> {
            settings.javaFormat
        }
        "javascript" -> {
            settings.javascriptFormat
        }
        "python" -> {
            settings.pythonFormat
        }
        else -> {
            ""
        }
    }
}