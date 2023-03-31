package net.trelent.document.actions


import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
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
import com.jetbrains.rd.util.printlnError
import net.trelent.document.actions.notifications.LoginNotificationAction
import net.trelent.document.actions.notifications.SignupNotificationAction
import net.trelent.document.actions.notifications.UpgradeLearnNotificationAction
import net.trelent.document.actions.notifications.UpgradeNotificationAction
import net.trelent.document.helpers.*
import net.trelent.document.helpers.Function
import net.trelent.document.settings.TrelentSettingsState
import net.trelent.document.widgets.WidgetListeners
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Range
import java.awt.Point

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
            val sourceCode = document.text
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
            val task = object : Task.Backgroundable(project, "Writing docstring") {
                override fun run(indicator: ProgressIndicator) {

                    indicator.text = "Writing docstring..."
                    indicator.isIndeterminate = true
                    val parsedFunctions = parseFunctions(
                        language,
                        sourceCode
                    )

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

                    // We got the current function!
                    val funcName = currentFunction.name
                    val funcParams = currentFunction.params
                    val funcText = currentFunction.text

                    //FIXME: Remove this when typescript support added in backend
                    if(language == "typescript"){
                        language = "javascript"
                    }

                    // Get the docstring format
                    val settings = TrelentSettingsState.getInstance()
                    val format = getFormat(language, settings)

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

                    if (!docstring.successful) {
                        val errorType = docstring.error_type

                        if (errorType == null) {
                            showError(docstring.error, project)
                            return
                        }

                        when (errorType) {
                            "exceeded_anonymous_quota" -> {
                                showAnonymousUsageError(
                                    "Please sign up for a free account to get an extra 50 docs/month. You've hit your anonymous usage limit!",
                                    project
                                )
                            }

                            "exceeded_free_quota" -> {
                                showFreeUsageError(
                                    "You have reached your usage limit. Please log in to Trelent to continue.",
                                    project
                                )
                            }

                            "exceeded_paid_quota" -> {
                                showPaidUsageError(
                                    "You have reached your usage limit. Please log in to Trelent to continue.",
                                    project
                                )
                            }

                            else -> {
                                showError(docstring.error, project)
                            }
                        }

                        return
                    }


                    ApplicationManager.getApplication().invokeLater {
                        // Get docstring and related metadata
                        val docStringOffset = currentFunction.docstring_offset
                        val docStringColumn = editor.offsetToVisualPosition(docStringOffset).column;

                        val docStringSplit = docstring.data!!.docstring.trim().split("\n");
                        val docStringHead = docStringSplit[0];
                        val docStringBody = (docStringSplit.subList(1, docStringSplit.size).joinToString("\n") + "\n").prependIndent(" ".repeat(docStringColumn))
                        val docstringText = docStringHead + "\n" + docStringBody;

                        // Insert the docstring
                        WriteCommandAction.runWriteCommandAction(project) {
                            document.insertString(docStringOffset, docstringText)
                        }

                        // Update docs progress
                        val publisher = project.messageBus.syncPublisher(WidgetListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION);
                        publisher.documented(editor, language);
                    }
                }
            }
            ProgressManager.getInstance().run(task)
        }
        catch(e: Exception){
            printlnError("Error parsing: ${e.stackTraceToString()}")
        }
    }

    fun getCurrentFunction(functions: Array<Function>, offset: Int): Function?{
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

fun showAnonymousUsageError(message: String, project: Project)
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

fun showFreeUsageError(message: String, project: Project)
{
    val errNotification = Notification(
        "Trelent Error Notification Group",
        "Usage limit exceeded",
        message,
        NotificationType.ERROR
    )

    // Add login and signup buttons to the notification

    val upgradeAction = UpgradeNotificationAction("Upgrade")
    val upgradeLearnAction = UpgradeLearnNotificationAction("Learn More")

    errNotification.addAction(upgradeAction)
    errNotification.addAction(upgradeLearnAction)

    Notifications.Bus.notify(errNotification, project)
}

fun showPaidUsageError(message: String, project: Project)
{
    val errNotification = Notification(
        "Trelent Error Notification Group",
        "Usage limit exceeded",
        message,
        NotificationType.ERROR
    )

    // Add login and signup buttons to the notification
    /*
    val contactAction = ContactNotificationAction("Contact Us")

    errNotification.addAction(contactAction)

     */

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