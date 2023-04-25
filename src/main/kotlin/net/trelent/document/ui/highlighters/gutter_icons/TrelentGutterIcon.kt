package net.trelent.document.ui.highlighters.gutter_icons

import com.intellij.codeInsight.daemon.NonHideableIconGutterMark
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.NlsContexts
import net.trelent.document.helpers.Function
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import javax.swing.Icon


class TrelentGutterRenderer(val editor: Editor, val function: Function, val myIcon: Icon, @Nullable @NlsContexts.Tooltip val myTooltip: String): GutterIconRenderer(), NonHideableIconGutterMark{

    @NotNull
    override fun getIcon(): Icon {
        return myIcon
    }

    @Nullable
    override fun getTooltipText(): @NlsContexts.Tooltip String {
        return myTooltip
    }


    override fun isNavigateAction(): Boolean {
        return true
    }

    override fun isDumbAware(): Boolean {
        return true
    }

    @NotNull
    override fun getAlignment(): Alignment {
        return Alignment.RIGHT
    }

    override fun equals(other: Any?): Boolean {
        if(other == null) return false;
        return other === this
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun getPopupMenuActions(): ActionGroup {

        return GutterActionGroup(function, editor);
    }

    fun performAction(@NotNull e: AnActionEvent) {

    }

    //protected abstract fun handleMouseClick()
}
