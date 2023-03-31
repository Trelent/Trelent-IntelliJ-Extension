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
import com.intellij.openapi.editor.Editor
import net.trelent.document.widgets.WidgetListeners


@State(name = "net.trelent.document.settings.TrelentSettingsState", storages = [Storage("TrelentSettings.xml")])
class TrelentSettingsState : PersistentStateComponent<TrelentSettingsState.TrelentSettings> {

    init{
        ApplicationManager.getApplication().messageBus.connect().subscribe(WidgetListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION, object : WidgetListeners.DocumentedListener{
            override fun documented(editor: Editor, language: String) {
                settings.numDocumented++;
                if(settings.numDocumented == DOC_THRESHOLD){
                    showDiscordNotification()
                }
            }

        })
    }

    var settings: TrelentSettings = TrelentSettings()

    data class TrelentSettings(
        @Transient
        var csharpFormat: String = "xml",
        @Transient
        var javaFormat: String = "javadoc",
        @Transient
        var javascriptFormat: String = "jsdoc",
        @Transient
        var pythonFormat: String = "rest",
        var numDocumented: Int = 0
    )

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
            return ApplicationManager.getApplication().getService(TrelentSettingsState::class.java)
        }
    }

    override fun getState(): TrelentSettings? {
        return settings
    }
}