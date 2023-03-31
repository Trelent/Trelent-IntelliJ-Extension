package net.trelent.document.widgets.TrelentStatus

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.AnimatedIcon
import com.intellij.util.ui.update.Activatable
import net.trelent.document.settings.TrelentIcons
import net.trelent.document.widgets.WidgetListeners
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel


class TrelentStatusWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, Activatable {
    companion object {
        @JvmStatic val ICON: Icon = TrelentIcons.TRELENT_ICON
        @JvmStatic val LOADING_ICON = AnimatedIcon(1000/TrelentIcons.LOADING_ICONS.size, *TrelentIcons.LOADING_ICONS);
        @JvmStatic val WIDGET_ID: String = "Trelent_DocStatus"
        @JvmStatic val WIDGET_NAME: String = "Trelent: Documentation Status"
    }

    private var label: JLabel = JLabel();
    init {
        label.icon = ICON;

        project.messageBus.connect(this).subscribe(
            WidgetListeners.TrelentDocumentationListener.TRELENT_DOCUMENTATION_ACTION,
            object : WidgetListeners.TrelentDocumentationListener {
                override fun updateDocState(state: Boolean) {
                    setLabelState(state);
                }
            })
    }



    override fun dispose() {
        super.dispose()
    }

    override fun ID(): String {
       return WIDGET_ID

    }

    override fun getComponent(): JComponent {
            return label;
    }

    fun setLabelState(documenting: Boolean){
        label.icon = if(documenting) LOADING_ICON else ICON;
    }

}