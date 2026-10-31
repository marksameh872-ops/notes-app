package com.notes.premium

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.View

object Theme {

    val ACCENT = 0xFFFFC400.toInt()
    val ACCENT_DARK = 0xFFF0A500.toInt()

    fun isDark(ctx: Context): Boolean {
        return when (NoteStore.darkMode(ctx)) {
            1 -> false
            2 -> true
            else -> {
                val mask = ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mask == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun bg(dark: Boolean) = if (dark) 0xFF0E0E12.toInt() else 0xFFF1F2F7.toInt()
    fun glass(dark: Boolean) = if (dark) 0xF01D1D25.toInt() else 0xE8FFFFFF.toInt()
    fun glassStrong(dark: Boolean) = if (dark) 0xFF23232C.toInt() else 0xFFFFFFFF.toInt()
    fun stroke(dark: Boolean) = if (dark) 0x33FFFFFF.toInt() else 0x80FFFFFF.toInt()
    fun text(dark: Boolean) = if (dark) 0xFFF2F2F7.toInt() else 0xFF1C1C1E.toInt()
    fun textSec(dark: Boolean) = if (dark) 0xFF9A9AA2.toInt() else 0xFF84848B.toInt()
    fun paperDefault(dark: Boolean) = if (dark) 0xFF17171C.toInt() else 0xFFFFFFFF.toInt()
    fun lineDefault(dark: Boolean) = if (dark) 0xFF2C2C36.toInt() else 0xFFD9E4F2.toInt()

    fun apply(activity: Activity) {
        val dark = isDark(activity)
        activity.window.statusBarColor = bg(dark)
        activity.window.navigationBarColor = bg(dark)
        activity.window.setBackgroundDrawable(ColorDrawable(bg(dark)))
        val v = activity.window.decorView
        var flags = v.systemUiVisibility
        flags = if (!dark) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        v.systemUiVisibility = flags
    }
}

object Papers {
    // index 0 = default (follows dark mode), the rest are fixed colors
    val paperColors = intArrayOf(0, 0xFFFFF9E6.toInt(), 0xFFEFFCF6.toInt(), 0xFFEDF4FF.toInt(), 0xFFFDEFF5.toInt())
    val lineColors = intArrayOf(0, 0xFFB9CFE8.toInt(), 0xFFC9C9D0.toInt(), 0xFFBFE3C8.toInt(), 0xFFEFC6D8.toInt())

    fun paper(idx: Int, dark: Boolean): Int =
        if (idx <= 0) Theme.paperDefault(dark) else paperColors[idx]

    fun line(idx: Int, dark: Boolean): Int =
        if (idx <= 0) Theme.lineDefault(dark) else lineColors[idx]

    fun textOn(paperIdx: Int, dark: Boolean): Int =
        if (paperIdx <= 0) Theme.text(dark) else 0xFF1C1C1E.toInt()
}
