package net.trelent.document.ui.highlighters

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import icons.TrelentPluginIcons
import net.trelent.document.helpers.Function
import javax.swing.Icon


abstract class TrelentAutodocHighlighter(editor: Editor, offset: Int) : Disposable {
    var highlighter: RangeHighlighter;


    init {
        highlighter = editor.markupModel.addRangeHighlighter(null, offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX,
            HighlighterTargetArea.LINES_IN_RANGE)
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


    class TrelentIgnore(editor: Editor, function: Function): TrelentAutodocHighlighter(editor, function.offsets[0]) {

        init{
            update()
        }
        override fun createRenderer(): GutterIconRenderer {
            val icon: Icon = TrelentPluginIcons.DocumentActionIcon;
            return object :TrelentGutterRenderer(icon, "Ignore Changes"){
                override fun handleMouseClick() {
                    println("Mouse clicked! (Ignore)")
                }

            }
        }

    }

    class TrelentAccept(editor: Editor, function: Function): TrelentAutodocHighlighter(editor, function.offsets[0]) {

        init{
            update()
        }
        override fun createRenderer(): GutterIconRenderer {
            val icon: Icon = TrelentPluginIcons.DocumentActionIcon;
            return object :TrelentGutterRenderer(icon, "Trelent: Generate new docstring"){
                override fun handleMouseClick() {
                    println("Mouse clicked! (Accept)")
                }

            }
        }

    }
}