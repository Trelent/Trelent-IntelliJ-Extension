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
    /*
    private var formats = mapOf(
        "csharp" to arrayOf("xml"),
        "java" to arrayOf("javadoc"),
        "javascript" to arrayOf("jsdoc"),
        "python" to arrayOf("rest", "google", "numpy"),
    )*/

    private val myMainPanel: JPanel

    private val csharpFormat = JBList<String>("xml")
    private val javaFormat = JBList<String>("javadoc")
    private val javascriptFormat = JBList<String>("jsdoc")
    private val pythonFormat = JBList<String>("rest", "google", "numpy")

    init {
        myMainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("C# Docstring Format", csharpFormat, 1)
            .addLabeledComponent("Java Docstring Format", javaFormat, 10)
            .addLabeledComponent("JavaScript Docstring Format", javascriptFormat, 10)
            .addLabeledComponent("Python Docstring Format", pythonFormat, 10)
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