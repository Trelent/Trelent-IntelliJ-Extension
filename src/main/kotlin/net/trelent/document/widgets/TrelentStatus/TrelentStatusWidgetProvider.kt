package net.trelent.document.widgets.TrelentStatus

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class TrelentStatusWidgetProvider: StatusBarEditorBasedWidgetFactory() {
    override fun getId(): String {
        return TrelentStatusWidget.WIDGET_ID
    }

    override fun getDisplayName(): String {
        return TrelentStatusWidget.WIDGET_NAME
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return TrelentStatusWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true;
    }
}