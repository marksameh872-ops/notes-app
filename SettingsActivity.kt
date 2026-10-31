package com.notes.premium

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)
        val dark = Theme.isDark(this)

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Theme.bg(dark))
        root.setPadding(Ui.dp(this, 22f), Ui.dp(this, 14f), Ui.dp(this, 22f), Ui.dp(this, 60f))

        // header
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val back = Ui.label(this, "←", 22f, Theme.text(dark), true)
        back.setPadding(0, Ui.dp(this, 10f), Ui.dp(this, 14f), Ui.dp(this, 10f))
        back.setOnClickListener { finish() }
        head.addView(back)
        head.addView(Ui.label(this, "الإعدادات", 22f, Theme.text(dark), true))
        root.addView(head, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // appearance
        root.addView(sectionTitle("المظهر", dark))
        val modeNames = arrayOf("تلقائي", "فاتح", "داكن")
        root.addView(chipRow(modeNames, NoteStore.darkMode(this), dark) { i ->
            NoteStore.setDarkMode(this, i)
            Theme.apply(this)
            recreate()
        })

        // default paper
        root.addView(sectionTitle("نمط الورق الافتراضي", dark))
        val styleNames = arrayOf("سطور", "أبيض", "نقاط", "مربعات")
        val styleCodes = arrayOf("ruled", "blank", "dots", "grid")
        val selStyle = styleCodes.indexOf(NoteStore.defaultPaper(this)).coerceAtLeast(0)
        root.addView(chipRow(styleNames, selStyle, dark) { i ->
            NoteStore.setDefaultPaper(this, styleCodes[i])
            recreate()
        })

        // default font size
        root.addView(sectionTitle("حجم خط الملاحظات الجديدة", dark))
        val sizeNames = arrayOf("صغير 14", "عادي 17", "كبير 20", "ضخم 24")
        val sizes = intArrayOf(14, 17, 20, 24)
        val selSize = sizes.indexOf(NoteStore.defaultFont(this)).coerceAtLeast(0)
        root.addView(chipRow(sizeNames, selSize, dark) { i ->
            NoteStore.setDefaultFont(this, sizes[i])
            recreate()
        })

        // about
        root.addView(sectionTitle("عن التطبيق", dark))
        val about = Ui.label(this,
            "ملاحظاتي ✨ — تطبيق ملاحظات فاخر بتصميم زجاجي\n" +
                    "ورق مسطّر يتماشى مع الكتابة تمامًا، حفظ تلقائي دائم، " +
                    "وتصدير الملاحظة كاملة كصورة PNG بجودة عالية، " +
                    "مع دعم كامل للعربية والإنجليزية.\n\nالإصدار 1.0",
            14f, Theme.textSec(dark))
        about.setLineSpacing(Ui.dpF(this, 3f), 1f)
        about.background = Ui.glassBg(this, dark, 18f)
        about.setPadding(Ui.dp(this, 18f), Ui.dp(this, 16f), Ui.dp(this, 18f), Ui.dp(this, 16f))
        val alp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        alp.topMargin = Ui.dp(this, 6f)
        root.addView(about, alp)

        scroll.addView(root, ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
    }

    private fun sectionTitle(t: String, dark: Boolean): TextView {
        val v = Ui.label(this, t, 13f, Theme.textSec(dark), true)
        v.setPadding(Ui.dp(this, 4f), Ui.dp(this, 22f), 0, Ui.dp(this, 8f))
        return v
    }

    private fun chipRow(
        labels: Array<String>, selected: Int, dark: Boolean, onPick: (Int) -> Unit
    ): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (i in labels.indices) {
            val c = Ui.chip(this, labels[i], i == selected, dark, Theme.ACCENT)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = Ui.dp(this, 8f)
            val idx = i
            c.setOnClickListener { onPick(idx) }
            row.addView(c, lp)
        }
        return row
    }
}
