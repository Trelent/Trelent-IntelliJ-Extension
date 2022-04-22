package net.trelent.document.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
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
import net.trelent.document.settings.TrelentSettingsState

val intellijVersion = ApplicationInfo.getInstance().versionName.split(" ")[1]

class DocumentAction : AnAction() {

    override fun update(e: AnActionEvent) {
        // Using the event, evaluate the context, and enable or disable the action.
    }

    override fun actionPerformed(@NotNull e: AnActionEvent) {


        // Get the open project and editor, if there is one
        val project: Project = e.getRequiredData(CommonDataKeys.PROJECT)
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        if(editor == null)
        {
            showError("No text editor is open! To write a docstring, you must have a text editor open and click within somewhere on a function.", project)
            return
        }



        // Get the editor's contents, language, and cursor
        val document: Document = editor.document
        val cursor: Caret = editor.caretModel.currentCaret
        val sourceCode = document.text
        val file = FileEditorManager.getInstance(project).selectedFiles[0]
        val language = if (file.extension == "py") "python" else file.fileType.displayName.lowercase()

        // Get a user id for the user on this machine
        val userId = System.getProperty("user.name")

        // Check if this is a supported language
        if(!SUPPORTED_LANGUAGES.contains(language))
        {
            showError("We do not support this language yet. Currently supported languages include C#, Java, JavaScript and Python.", project)
            return
        }

        val task = object : Task.Backgroundable(project, "Writing docstring") {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Writing docstring..."
                indicator.isIndeterminate = true
                val currentFunction = parseCurrentFunction(
                    arrayOf(cursor.visualPosition.getLine(), cursor.visualPosition.getColumn()),
                    language,
                    sourceCode
                )

                if (currentFunction == null) {
                    showError("Your cursor is not inside a valid function. Please click inside the function body if you have not already.", project)
                    return
                }

                // We got the current function!
                val funcName   = currentFunction.name
                var funcParams = currentFunction.params
                val funcText   = currentFunction.text

                // Cleanup empty params
                if(funcParams == null) {
                    funcParams = arrayOf()
                }

                // Get the docstring format
                val settings = TrelentSettingsState.getInstance()
                val format   = getFormat(language, settings)

                // Request a docstring
                val docstring = getDocstring(
                    format,
                    language,
                    funcName,
                    funcParams,
                    "ext-intellij",
                    funcText,
                    userId
                )

                if(!docstring.successful)
                {
                    val error = docstring.error

                    // Check if this is a usage limit error, and prompt for login if it is
                    if(error.contains("100 docs/month") || error.contains("paid account"))
                    {
                        showUsageError("You have reached your usage limit. Please log in to Trelent to continue.", project)
                        return
                    }
                    else
                    {
                        showError(docstring.error, project)
                    }

                    return
                }

                // Get docstring and related metadata
                val docStringPoint = currentFunction.docstring_point
                val docStringLine = docStringPoint[0]
                val docStringColumn = docStringPoint[1]
                var docStringPosition = document.getLineStartOffset(docStringLine)

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
                else {
                    if(language != "python") {
                        docStringPosition = document.getLineEndOffset(docStringLine)
                    }

                    val docstringText = if(language == "python") {
                        docstring.data?.docstring!!.prependIndent(" ".repeat(docStringColumn)) + "\n"
                    } else {
                        "\n" + docstring.data?.docstring!!.prependIndent(" ".repeat(docStringColumn))
                    }

                    // Insert the docstring
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.insertString(docStringPosition, docstringText)
                    }
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

    fun showError(message: String, project: Project)
    {
        val errNotification = Notification(
            "Trelent Error Notification Group",
            "Error writing docstring",
            message,
            NotificationType.ERROR
        )
        Notifications.Bus.notify(errNotification, project)
    }

    fun showUsageError(message: String, project: Project)
    {
        val errNotification = Notification(
            "Trelent Error Notification Group",
            "Usage limit exceeded",
            message,
            NotificationType.ERROR
        )

        // Add login and signup buttons to the notification
        val loginAction = LoginNotificationAction("Login")
        val signupAction = SignupNotificationAction("Sign up")

        errNotification.addAction(loginAction)
        errNotification.addAction(signupAction)

        Notifications.Bus.notify(errNotification, project)
    }

    fun getFormat(language: String, settings: TrelentSettingsState): String
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

}