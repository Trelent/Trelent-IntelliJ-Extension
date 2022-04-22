package net.trelent.document.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


@State(name = "net.trelent.document.settings.TrelentSettingsState", storages = [Storage("TrelentSettings.xml")])
class TrelentSettingsState : PersistentStateComponent<TrelentSettingsState> {
    var csharpFormat = "xml"
    var javaFormat = "javadoc"
    var javascriptFormat = "jsdoc"
    var pythonFormat = "rest"

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