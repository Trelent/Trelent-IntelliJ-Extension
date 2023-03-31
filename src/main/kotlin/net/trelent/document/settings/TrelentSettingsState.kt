package net.trelent.document.settings

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Editor
import com.intellij.util.xmlb.XmlSerializerUtil
import net.trelent.document.helpers.showNotification
import net.trelent.document.widgets.WidgetListeners
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


@State(name = "net.trelent.document.settings.TrelentSettingsState", storages = [Storage("TrelentSettings.xml")], category = SettingsCategory.PLUGINS)
class TrelentSettingsState : PersistentStateComponent<TrelentSettingsState> {
    var csharpFormat = "xml"
    var javaFormat = "javadoc"
    var javascriptFormat = "jsdoc"
    var pythonFormat = "rest"
    var numDocumented = 0;

    private val DOC_THRESHOLD = 10;
    private val DISCORD_LINK = "https://discord.gg/trelent"

    init{
        ApplicationManager.getApplication().messageBus.connect().subscribe(WidgetListeners.DocumentedListener.TRELENT_DOCUMENTED_ACTION, object : WidgetListeners.DocumentedListener{
            override fun documented(editor: Editor, language: String) {
                numDocumented++;
                if(numDocumented == DOC_THRESHOLD){
                    showDiscordNotification()
                }
            }

        })
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

    @Nullable
    override fun getState(): TrelentSettingsState {
        return this
    }

    override fun loadState(@NotNull state: TrelentSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): TrelentSettingsState {
            return ApplicationManager.getApplication().getService(TrelentSettingsState::class.java)
        }
    }
}