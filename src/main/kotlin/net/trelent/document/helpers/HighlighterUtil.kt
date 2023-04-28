package net.trelent.document.helpers

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.util.concurrency.annotations.RequiresEdt
import net.trelent.document.ui.highlighters.TrelentAutodocHighlighter
import net.trelent.document.helpers.Function


const val LAYER_PRIORITY_STEP = 5 // BASE_LAYER..LINE_MARKER_LAYER
const val DEFAULT_LAYER = HighlighterLayer.SELECTION - 102

@RequiresEdt
fun getHighlights(editor: Editor, functions: List<Function>): ArrayList<RangeHighlighter> {
    val highlighters: ArrayList<RangeHighlighter> = arrayListOf()

    functions.forEach{function ->

        ApplicationManager.getApplication().runReadAction{
            try{
                //Get function offsets
                val start = function.offsets[0]
                val end = function.offsets[1]

                //Ensure that the offsets are within the range of the document (Prevents exception)
                if(editor.document.text.length >= end && editor.document.text.length > start){
                    ApplicationManager.getApplication().runWriteAction{
                        val attributesKey =
                            TextAttributesKey.createTextAttributesKey("TRELENT_HIGHLIGHT")

                        val highlighter = editor.markupModel.addRangeHighlighter( start,
                            end,
                            getLayer(DEFAULT_LAYER, 10),
                            attributesKey.defaultAttributes,
                            HighlighterTargetArea.LINES_IN_RANGE)
                        highlighter.errorStripeTooltip = "Trelent: Outdated docstring"
                        highlighters.add(highlighter)
                    }

                }

            }
            finally{}

        }

    }
    return highlighters;
}

private fun getLayer(layer: Int, layerPriority: Int): Int {
    return layer + layerPriority * LAYER_PRIORITY_STEP
}