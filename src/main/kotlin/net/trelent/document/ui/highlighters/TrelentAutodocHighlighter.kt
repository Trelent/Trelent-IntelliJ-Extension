package net.trelent.document.ui.highlighters

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import icons.TrelentPluginIcons
import net.trelent.document.helpers.Function
import net.trelent.document.ui.highlighters.gutter_icons.TrelentGutterRenderer
import javax.swing.Icon


abstract class TrelentAutodocHighlighter(editor: Editor, offset: Int) : Disposable {
    private lateinit var highlighter: RangeHighlighter;


    init {
        if(editor.document.text.length <= offset){
            throw IllegalStateException()
        }
        ApplicationManager.getApplication().runWriteAction{
            highlighter = editor.markupModel.addRangeHighlighter(null, offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX,
                HighlighterTargetArea.LINES_IN_RANGE)
        }

    }
    override fun dispose() {
        highlighter.dispose();
    }

    fun update(){
        if(highlighter.isValid){
            highlighter.gutterIconRenderer = createRenderer()
        }
    }


    protected abstract fun createRenderer(): GutterIconRenderer?


    class TrelentAutodocIcon(val editor: Editor, val function: Function): TrelentAutodocHighlighter(editor, function.offsets[0]) {

        init{
            update()
        }
        override fun createRenderer(): GutterIconRenderer {
            val icon: Icon = TrelentPluginIcons.DocumentActionIcon;
            return TrelentGutterRenderer(editor, function, icon, "Trelent: Change Detected");
        }

    }
}