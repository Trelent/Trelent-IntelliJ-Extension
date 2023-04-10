package net.trelent.document.helpers

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter


const val LAYER_PRIORITY_STEP = 5 // BASE_LAYER..LINE_MARKER_LAYER
const val DEFAULT_LAYER = HighlighterLayer.SELECTION - 102

fun getHighlights(editor: Editor, functions: List<Function>): ArrayList<RangeHighlighter> {
    val highlighters: ArrayList<RangeHighlighter> = arrayListOf()

    val doc = editor.document
    ApplicationManager.getApplication().invokeLater{
        functions.forEach{function ->

            val firstLine = doc.getLineNumber(function.offsets[0])
            val lastLine = doc.getLineNumber(function.offsets[1])
            val isEmptyRange = firstLine == lastLine;
            val isFirstLine =  firstLine == 0;
            val isLastLine = lastLine == doc.lineCount;

            val offsets = DiffUtil.getLinesRange(doc, firstLine, lastLine)

            val start = function.offsets[0]
            val end = function.offsets[1]


            val attributesKey =
                TextAttributesKey.createTextAttributesKey("TRELENT_HIGHLIGHT")


            ApplicationManager.getApplication().runReadAction{
                val highlighter = editor.markupModel.addRangeHighlighter( start, end, getLayer(DEFAULT_LAYER, 10), attributesKey.defaultAttributes, HighlighterTargetArea.LINES_IN_RANGE)
                highlighters.add(highlighter)
            }

        }
    }
    return highlighters;
}

private fun getTrelentAutodocHighlighter(highlighter: RangeHighlighter){

}
private fun getLayer(layer: Int, layerPriority: Int): Int {
    return layer + layerPriority * LAYER_PRIORITY_STEP
}