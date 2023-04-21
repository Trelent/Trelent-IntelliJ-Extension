package net.trelent.document.ui.highlighters

import com.intellij.codeInsight.daemon.NonHideableIconGutterMark
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ObjectUtils
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.event.MouseEvent
import javax.swing.Icon


public abstract class TrelentGutterRenderer(val myIcon: Icon, @Nullable @NlsContexts.Tooltip val myTooltip: String): GutterIconRenderer(), NonHideableIconGutterMark{

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
        return Alignment.LEFT
    }

    @Nullable
    override fun getClickAction(): AnAction? {
        return DumbAwareAction.create { e: AnActionEvent -> performAction(e) }
    }

    override fun equals(other: Any?): Boolean {
        if(other == null) return false;
        return other === this
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    protected open fun performAction(@NotNull e: AnActionEvent) {
        try{
            val mouseEvent: MouseEvent = ObjectUtils.tryCast(e.inputEvent, MouseEvent::class.java)!!;
            if (mouseEvent.button == MouseEvent.BUTTON1) {
                handleMouseClick()
            }
        }
        finally{

        }

    }

    protected abstract fun handleMouseClick()
}
