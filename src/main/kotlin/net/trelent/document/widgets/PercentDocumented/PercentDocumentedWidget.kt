package net.trelent.document.widgets.PercentDocumented

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.update.Activatable
import com.jetbrains.rd.util.printlnError
import net.trelent.document.helpers.getExtensionLanguage
import net.trelent.document.helpers.parseFunctions
import org.jetbrains.annotations.NotNull
import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel


class PercentDocumentedWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, Activatable {
    companion object {
        @JvmStatic val WIDGET_ID: String = "Trelent_PercentDocumented"
        @JvmStatic val WIDGET_NAME: String = "Trelent: Percent Documented"
        @JvmStatic val ICON: Icon = AllIcons.Actions.Refresh
        @JvmStatic val FULL_COLOR: Color = JBColor.getHSBColor(1F/3F, 1F, 0.5F)
        @JvmStatic val MID_COLOR: Color = JBColor.getHSBColor(1F/6F, 1F, 0.5F)
        @JvmStatic val EMPTY_COLOR: Color = JBColor.getHSBColor(0F, 1.0F, 0.5F)
    }

    private var percentDocumented: Float = 0f;
    private var label: JLabel;

    init{
        project.messageBus.connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object: FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent){
                refreshDocumentation()
            }
        })
        label = JLabel()
        label.icon = ICON
        label.isOpaque = true;
        refreshDocumentation()
        label()
    }

    override fun dispose() {
        label.isVisible = false
        super.dispose()
    }

    private fun label(): JLabel{
        val rounder = DecimalFormat("#.##")
        rounder.roundingMode = RoundingMode.DOWN
        label.text = "File ${rounder.format(percentDocumented)}% Documented"
        val background = if(percentDocumented <= 50) ColorUtil.mix(EMPTY_COLOR, MID_COLOR, percentDocumented / 50.0) else ColorUtil.mix(MID_COLOR, FULL_COLOR,
            (percentDocumented - 50F) / 50.0)
        label.background = background
        label.isVisible = true
        return label
    }
    override fun ID(): String {
       return WIDGET_ID

    }

    override fun getComponent(): JComponent {
            return label()

    }

    override fun install(statusBar: StatusBar){
        this.label.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                refreshDocumentation()
            }
        })
    }

    private fun refreshDocumentation(){
        println("Refreshing documentation")

        try{
            val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor!!
            val document: Document = editor.document
            val sourceCode = document.text
            val file = FileEditorManager.getInstance(project).selectedFiles[0]
            val language = getExtensionLanguage(file.extension!!)!!

            val parsedFunctions = parseFunctions(language, sourceCode)

            val documentedFunctions: Float = parsedFunctions.count {
                it.docstring != null
            }.toFloat()

            percentDocumented = (documentedFunctions/parsedFunctions.size) * 100;
            label()
        }

        catch(e: Exception){
            printlnError("Error refreshing documentation ${e.stackTraceToString()}")
            clear()
        }

    }

    private fun clear(){
        label.isVisible = false
    }

    fun update(){
        val manager = project.getService(StatusBarWidgetsManager::class.java)
        val factory = manager.widgetFactories.find{ it.id == ID()} ?: return
        manager.updateWidget(factory)
        myStatusBar.updateWidget(ID())

    }

}