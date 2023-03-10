package net.trelent.document.widgets.PercentDocumented

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class PercentDocumentedWidgetProvider: StatusBarEditorBasedWidgetFactory() {
    override fun getId(): String {
        return PercentDocumentedWidget.WIDGET_ID
    }

    override fun getDisplayName(): String {
        return PercentDocumentedWidget.WIDGET_NAME
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return PercentDocumentedWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget);
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}