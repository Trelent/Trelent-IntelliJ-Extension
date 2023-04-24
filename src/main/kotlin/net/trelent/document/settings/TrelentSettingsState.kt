package net.trelent.document.settings

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import net.trelent.document.helpers.Function
import net.trelent.document.listeners.TrelentListeners


@State(name = "net.trelent.document.settings.TrelentSettingsState", storages = [Storage("TrelentSettings.xml")])
class TrelentSettingsState : PersistentStateComponent<TrelentSettingsState.TrelentSettings> {

    init{
        ApplicationManager.getApplication().messageBus.connect().subscribe(TrelentListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION, object : TrelentListeners.DocumentedListener{
            override fun documented(document: Document, function: Function, language: String) {
                settings.numDocumented++;
                if(settings.numDocumented == DOC_THRESHOLD){
                    showDiscordNotification()
                }
            }

        })
    }

    var settings: TrelentSettings = TrelentSettings()

    data class TrelentSettings(
        var csharpFormat: String = "xml",
        var javaFormat: String = "javadoc",
        var javascriptFormat: String = "jsdoc",
        var pythonFormat: String = "rest",
        var numDocumented: Int = 0,
        var threshold: AutodocThreshold = AutodocThreshold.NEUTRAL,
        var mode: TrelentTag = TrelentTag.HIGHLIGHT
    )

    enum class AutodocThreshold(val num: Int){
        PASSIVE(1250),
        NEUTRAL(750),
        AGGRESSIVE(250),
        NONE(0),
    }

    enum class TrelentTag(val tag: String) {
        AUTO("@trelent-auto"),
        HIGHLIGHT("@trelent-highlight"),
        IGNORE("@trelent-ignore"),
        NONE("")
    }

    private fun showDiscordNotification(){
        val errNotification = Notification(
            "Trelent Info Notification Group",
            "We see you're enjoying Trelent!",
            "We'd love to see you in our Discord community, where you can help shape the future of Trelent!",
            NotificationType.INFORMATION
        )
        errNotification.addAction(object: NotificationAction("Join community") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                BrowserUtil.browse(DISCORD_LINK)
            }

        })
        Notifications.Bus.notify(errNotification)
    }

    override fun loadState(state: TrelentSettings) {
        this.settings = state;
    }

    companion object {

        private val DOC_THRESHOLD = 10;
        private val DISCORD_LINK = "https://discord.gg/trelent"
        fun getInstance(): TrelentSettingsState {
            return service<TrelentSettingsState>()
        }
    }

    override fun getState(): TrelentSettings {
        return settings
    }
}