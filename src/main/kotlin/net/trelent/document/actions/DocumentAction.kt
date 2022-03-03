package net.trelent.document.actions

import com.intellij.internal.statistic.DeviceIdManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.annotations.NotNull
import net.trelent.document.helpers.parseCurrentFunction
import net.trelent.document.helpers.getDocstring
import net.trelent.document.helpers.SUPPORTED_LANGUAGES


class DocumentAction : AnAction() {
    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun actionPerformed(@NotNull e: AnActionEvent) {

        // Get the open project and editor, if there is one
        val project: Project = e.getRequiredData(CommonDataKeys.PROJECT)
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        if(editor == null)
        {
            showError("No text editor is open! To write a docstring, you must have a text editor open and click within somewhere on a function.")
            return
        }

        // Get the editor's contents, language, and cursor
        val document: Document = editor.document
        val cursor: Caret = editor.caretModel.currentCaret
        val sourceCode = document.text
        val file = FileEditorManager.getInstance(project).selectedFiles[0]
        println(file.fileType.name)
        println(file.fileType.displayName)
        val language = if (file.extension == "py") "python" else file.fileType.displayName.lowercase()

        // Get a unique identifier for this machine
        val userId = DeviceIdManager.getOrGenerateId()

        // Check if this is a supported language
        if(!SUPPORTED_LANGUAGES.contains(language))
        {
            showError("We do not support this language yet. Currently supported languages include C#, Java, JavaScript and Python.")
            return
        }

        val task = object : Task.Backgroundable(project, "Writing Docstring") {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Writing docstring..."
                indicator.isIndeterminate = true
                val currentFunction = parseCurrentFunction(
                    arrayOf<Int>(cursor.visualPosition.getLine(), cursor.visualPosition.getColumn()),
                    language,
                    sourceCode
                )

                if (currentFunction == null) {
                    showError("Your cursor is not inside a valid function. Please click inside the function body if you have not already.")
                    return
                }

                // We got the current function!
                val funcName   = currentFunction.name
                var funcParams = currentFunction.params
                val funcText   = currentFunction.text

                if(funcParams == null)
                {
                    funcParams = arrayOf<String>()
                }

                // Request a docstring
                val docstring = getDocstring(
                    language,
                    funcName,
                    funcParams,
                    "ext-intellij",
                    funcText,
                    userId
                )

                if(docstring == null)
                {
                    showError("Something went wrong on our end. Please try again later.")
                    return
                }

                // Get docstring and related metadata
                val docStringPoint = currentFunction.docstring_point
                val docStringLine = docStringPoint[0]
                val docStringColumn = docStringPoint[1]
                var docStringPosition = document.getLineStartOffset(docStringLine)
                var docstringText = ""

                // Does this line contain anything other than whitespace
                val lineContent = getLine(document, docStringLine)
                if(isEmpty(lineContent))
                {
                    docStringPosition = document.getLineStartOffset(docStringLine + 1)
                    // If so, we need to insert a newline before the docstring
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.insertString(docStringPosition, "\n")
                    }
                }

                docstringText = if(language == "python") {
                    docstring.docstring.prependIndent(" ".repeat(docStringColumn)) + "\n"
                } else {
                    "\n" + docstring.docstring.prependIndent(" ".repeat(docStringColumn))
                }

                // Insert the docstring
                WriteCommandAction.runWriteCommandAction(project) {
                    document.insertString(docStringPosition, docstringText)
                }
            }
        }
        ProgressManager.getInstance().run(task)
    }

    fun isEmpty(line: String) : Boolean
    {
        // Use regex to check if this string only contains whitespace
        val regex = Regex("^\\s+$")
        if(regex.containsMatchIn(line))
        {
            return true
        }

        return false
    }

    fun getLine(document: Document, lineNumber: Int): String {
        val startLineStartOffset = document.getLineStartOffset(lineNumber)
        val startLineEndOffset = document.getLineEndOffset(lineNumber)
        return document.getText(TextRange(startLineStartOffset, startLineEndOffset))
    }

    fun showError(message: String)
    {
        val errNotification = Notification(
            "Trelent",
            "Error writing docstring",
            message,
            NotificationType.ERROR
        )
        Notifications.Bus.notify(errNotification)
    }
}