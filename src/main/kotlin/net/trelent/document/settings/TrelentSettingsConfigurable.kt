package net.trelent.document.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


/**
 * Provides controller functionality for Trelent settings.
 */
class TrelentSettingsConfigurable : Configurable {
    private var mySettingsComponent: TrelentSettingsComponent? = null

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP

    // A default constructor with no arguments is required because this implementation
    // is registered as an applicationConfigurable EP
    override fun getDisplayName(): String {
        return "Trelent Settings"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.getPreferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = TrelentSettingsComponent()
        return mySettingsComponent?.getPanel()
    }

    override fun isModified(): Boolean {
        val settings: TrelentSettingsState = TrelentSettingsState.getInstance()
        var modified: Boolean = !mySettingsComponent?.getCsharpFormat().equals(settings.csharpFormat)
        modified = modified or (mySettingsComponent?.getJavaFormat() !== settings.javaFormat)
        modified = modified or (mySettingsComponent?.getJavascriptFormat() !== settings.javascriptFormat)
        modified = modified or (mySettingsComponent?.getPythonFormat() !== settings.pythonFormat)
        return modified
    }

    override fun apply() {
        val settings: TrelentSettingsState = TrelentSettingsState.getInstance()
        settings.csharpFormat = mySettingsComponent?.getCsharpFormat()!!
        settings.javaFormat = mySettingsComponent?.getJavaFormat()!!
        settings.javascriptFormat = mySettingsComponent?.getJavascriptFormat()!!
        settings.pythonFormat = mySettingsComponent?.getPythonFormat()!!
    }

    override fun reset() {
        val settings: TrelentSettingsState = TrelentSettingsState.getInstance()
        mySettingsComponent?.setCSharpFormat(settings.csharpFormat)
        mySettingsComponent?.setJavaFormat(settings.javaFormat)
        mySettingsComponent?.setJavascriptFormat(settings.javascriptFormat)
        mySettingsComponent?.setPythonFormat(settings.pythonFormat)
    }
}