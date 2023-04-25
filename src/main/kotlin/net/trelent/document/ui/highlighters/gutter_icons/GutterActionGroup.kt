package net.trelent.document.ui.highlighters.gutter_icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import net.trelent.document.helpers.Function

class GutterActionGroup(val function: Function, val editor: Editor): ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(GenerateDocstringAction(function, editor, "Trelent: Generate Docstring", "Generate a new docstring for this function", AllIcons.Actions.Commit),
            IgnoreFunctionAction(function, editor, "Trelent: Ignore Function Changes", "Ignore the function changes for this function", AllIcons.CodeWithMe.CwmTerminate))
    }
}