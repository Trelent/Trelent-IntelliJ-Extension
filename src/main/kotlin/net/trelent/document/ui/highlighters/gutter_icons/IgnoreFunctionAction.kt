package net.trelent.document.ui.highlighters.gutter_icons

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import net.trelent.document.helpers.Function
import net.trelent.document.services.ChangeDetectionService
import javax.swing.Icon

class IgnoreFunctionAction(val function: Function, val editor: Editor, text: String?, description: String?, icon: Icon?): AnAction(text, description, icon) {
    override fun actionPerformed(e: AnActionEvent) {
        if(e.project == editor.project && e.project != null){
            ChangeDetectionService.getInstance(e.project!!).clearDocChange(editor.document, function);
        }
    }
}