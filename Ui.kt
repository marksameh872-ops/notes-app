package com.notes.premium

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

object Ui {

    fun dpF(ctx: Context, v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.resources.displayMetrics)

    fun dp(ctx: Context, v: Float): Int = dpF(ctx, v).toInt()

    fun glassBg(ctx: Context, dark: Boolean, radiusDp: Float, withStroke: Boolean = true): GradientDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.RECTANGLE)
        g.setCornerRadius(dpF(ctx, radiusDp))
        g.setColor(Theme.glass(dark))
        if (withStroke) g.setStroke(dp(ctx, 1f), Theme.stroke(dark))
        return g
    }

    fun chipBg(ctx: Context, selected: Boolean, dark: Boolean, accent: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.RECTANGLE)
        g.setCornerRadius(dpF(ctx, 14f))
        g.setColor(if (selected) accent else Theme.glassStrong(dark))
        g.setStroke(1, Theme.stroke(dark))
        return g
    }

    fun chip(ctx: Context, text: String, selected: Boolean, dark: Boolean, accent: Int): TextView {
        val t = TextView(ctx)
        t.text = text
        t.textSize = 13f
        t.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        t.setTextColor(if (selected) 0xFF1C1C1E.toInt() else Theme.text(dark))
        t.setPadding(dp(ctx, 16f), dp(ctx, 9f), dp(ctx, 16f), dp(ctx, 9f))
        t.gravity = Gravity.CENTER
        t.background = chipBg(ctx, selected, dark, accent)
        return t
    }

    fun circle(color: Int, stroke: Int = 0x00000000): GradientDrawable {
        val g = GradientDrawable()
        g.setShape(GradientDrawable.OVAL)
        g.setColor(color)
        g.setStroke(1, stroke)
        return g
    }

    fun press(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }

    fun label(ctx: Context, text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(ctx)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }
}
