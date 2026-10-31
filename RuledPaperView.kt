package com.notes.premium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.view.View
import android.widget.EditText

/**
 * Draws notebook paper that is PERFECTLY aligned with the text baselines.
 * It reads the real line baselines from the EditText layout, so the ruled
 * lines always sit exactly under the text, whatever the font size, line
 * spacing, screen size or language (RTL/LTR).
 */
class RuledPaperView(context: Context) : View(context) {

    var paperStyle = "ruled"
    var lineColor = Theme.lineDefault(false)
    lateinit var edit: EditText

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun dpf(v: Float) = Ui.dpF(context, v)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::edit.isInitialized) return
        val layout = edit.layout ?: return
        if (layout.lineCount <= 0) return

        val left = edit.paddingLeft.toFloat()
        val right = width - edit.paddingRight.toFloat()
        val off = dpf(5f)
        val step = lineStep(layout)

        linePaint.strokeWidth = dpf(1f)
        linePaint.color = lineColor
        dotPaint.color = lineColor
        dotPaint.strokeWidth = dpf(2.2f)

        when (paperStyle) {
            "ruled", "grid" -> {
                var i = 0
                var y = layout.getLineBaseline(0) + off
                while (y < height + step) {
                    canvas.drawLine(left, y, right, y, linePaint)
                    i++
                    y = if (i < layout.lineCount) layout.getLineBaseline(i) + off else y + step
                }
                if (paperStyle == "grid") {
                    var x = left
                    while (x < right) {
                        canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
                        x += dpf(26f)
                    }
                }
            }
            "dots" -> {
                var i = 0
                var y = layout.getLineBaseline(0) + off
                while (y < height + step) {
                    var x = left
                    while (x < right) {
                        canvas.drawCircle(x, y, dpf(1.3f), dotPaint)
                        x += dpf(26f)
                    }
                    i++
                    y = if (i < layout.lineCount) layout.getLineBaseline(i) + off else y + step
                }
            }
        }
    }

    private fun lineStep(layout: Layout): Float {
        if (layout.lineCount >= 2) {
            val s = layout.getLineBaseline(1) - layout.getLineBaseline(0)
            if (s > 0) return s.toFloat()
        }
        return edit.paint.fontSpacing + edit.lineSpacingExtra
    }
}
