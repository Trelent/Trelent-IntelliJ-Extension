package net.trelent.document.settings

import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Supports creating and managing a [JPanel] for the Trelent Settings Dialog.
 */
class TrelentSettingsComponent {

    private val myMainPanel: JPanel

    private val csharpFormat = JBList("xml")
    private val javaFormat = JBList("javadoc")
    private val javascriptFormat = JBList("jsdoc")
    private val pythonFormat = JBList("rest", "google", "numpy")

    init {
        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("C# docstring format", csharpFormat, 1)
            .addLabeledComponent("Java docstring format", javaFormat, 10)
            .addLabeledComponent("JavaScript docstring format", javascriptFormat, 10)
            .addLabeledComponent("Python docstring format", pythonFormat, 10)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel(): JPanel {
        return myMainPanel
    }

    fun getPreferredFocusedComponent(): JComponent {
        return pythonFormat
    }

    @NotNull
    fun getCsharpFormat(): String {
        return csharpFormat.selectedValue
    }

    fun setCSharpFormat(@NotNull newText: String?) {
        csharpFormat.setSelectedValue(newText, true)
    }

    @NotNull
    fun getJavaFormat(): String {
        return javaFormat.selectedValue
    }

    fun setJavaFormat(@NotNull newText: String?) {
        javaFormat.setSelectedValue(newText, true)
    }

    @NotNull
    fun getJavascriptFormat(): String {
        return javascriptFormat.selectedValue
    }

    fun setJavascriptFormat(@NotNull newText: String?) {
        javascriptFormat.setSelectedValue(newText, true)
    }

    @NotNull
    fun getPythonFormat(): String {
        return pythonFormat.selectedValue
    }

    fun setPythonFormat(@NotNull newText: String?) {
        pythonFormat.setSelectedValue(newText, true)
    }
}