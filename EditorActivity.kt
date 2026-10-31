package com.notes.premium

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class EditorActivity : Activity() {

    private lateinit var note: Note
    private lateinit var titleEdit: EditText
    private lateinit var edit: EditText
    private lateinit var paper: RuledPaperView
    private lateinit var status: TextView
    private lateinit var frame: FrameLayout
    private lateinit var scroll: ScrollView

    private var isNew = false
    private var discarded = false
    private var restoring = false

    private val handler = Handler(Looper.getMainLooper())
    private val undoStack = ArrayDeque<Pair<String, Int>>()
    private val redoStack = ArrayDeque<Pair<String, Int>>()
    private var lastCommitted = ""
    private var pendingHistory = false

    private var pendingExport: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.apply(this)

        val id = intent.getStringExtra("id")
        val found = NoteStore.get(id)
        if (found != null) {
            note = found
        } else {
            note = Note(
                paperStyle = NoteStore.defaultPaper(this),
                fontSize = NoteStore.defaultFont(this)
            )
            NoteStore.upsert(note)
            isNew = true
        }

        buildUi()

        restoring = true
        titleEdit.setText(note.title)
        edit.setText(HtmlText.fromHtml(note.content))
        edit.setSelection(edit.text.length)
        restoring = false
        lastCommitted = edit.text.toString()
        applyPaper()
    }

    // =====================================================================
    // UI
    // =====================================================================

    private fun dp(v: Float) = Ui.dp(this, v)

    private fun buildUi() {
        val dark = Theme.isDark(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Theme.bg(dark))

        // ---------- top toolbar ----------
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(10f), dp(8f), dp(14f), dp(8f))
        bar.setBackgroundColor(Theme.bg(dark))

        val back = barButton("←", 22f, dark)
        back.setOnClickListener { finishWithSave() }
        bar.addView(back)

        status = TextView(this)
        status.textSize = 12f
        status.setTextColor(Theme.textSec(dark))
        status.text = "✓ محفوظة"
        val sp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        sp.marginStart = dp(6f)
        status.layoutParams = sp
        bar.addView(status)

        val undoB = barButton("↩", 20f, dark)
        undoB.setOnClickListener { doUndo() }
        val redoB = barButton("↪", 20f, dark)
        redoB.setOnClickListener { doRedo() }
        val moreB = barButton("⋯", 22f, dark)
        moreB.setOnClickListener { showMoreMenu() }
        bar.addView(undoB)
        bar.addView(redoB)
        bar.addView(moreB)
        root.addView(bar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // ---------- title ----------
        titleEdit = EditText(this)
        titleEdit.background = null
        titleEdit.hint = "العنوان"
        titleEdit.textSize = 24f
        titleEdit.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        titleEdit.setTextColor(Theme.text(dark))
        titleEdit.setHintTextColor(Theme.textSec(dark))
        titleEdit.setSingleLine(true)
        titleEdit.imeOptions = EditorInfo.IME_ACTION_DONE
        titleEdit.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        titleEdit.setPadding(dp(24f), dp(14f), dp(24f), dp(6f))
        root.addView(titleEdit, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val divider = View(this)
        divider.setBackgroundColor(Theme.stroke(dark))
        root.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1))

        // ---------- paper + editor ----------
        scroll = ScrollView(this)
        frame = FrameLayout(this)
        paper = RuledPaperView(this)
        edit = EditText(this)
        paper.edit = edit

        edit.background = null
        edit.hint = "ابدأ الكتابة…"
        edit.textSize = note.fontSize.toFloat()
        edit.setTextColor(Theme.text(dark))
        edit.setHintTextColor(Theme.textSec(dark))
        edit.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        edit.gravity = Gravity.TOP or Gravity.START
        edit.textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        edit.setLineSpacing(dp(12f).toFloat(), 1f)
        edit.isVerticalScrollBarEnabled = false
        edit.setHorizontallyScrolling(false)
        edit.setPadding(dp(24f), dp(18f), dp(24f), dp(150f))

        frame.addView(paper, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        frame.addView(edit, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        scroll.addView(frame, ViewGroupParams())
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        scroll.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (scroll.height > 0 && edit.minimumHeight < scroll.height) {
                        edit.setMinimumHeight(scroll.height - dp(30f))
                    }
                    paper.invalidate()
                }
            })

        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!restoring) onTextEdited()
            }
        })

        titleEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!restoring) scheduleSave()
            }
        })

        // ---------- formatting bar ----------
        val fbar = HorizontalScrollView(this)
        fbar.isHorizontalScrollBarEnabled = false
        fbar.setFillViewport(true)
        fbar.setBackgroundColor(if (dark) 0xF21B1B23.toInt() else 0xF7FFFFFF.toInt())
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(12f), dp(8f), dp(12f), dp(10f))

        val bB = fmtButton(row, "B", dark) { toggleStyleSpan(Typeface.BOLD) }
        val bI = fmtButton(row, "I", dark) { toggleStyleSpan(Typeface.ITALIC) }
        bI.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        val bU = fmtButton(row, "U", dark) { toggleUnderline() }
        bU.paintFlags = bU.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        fmtButton(row, "🖍", dark) { toggleHighlight() }
        fmtButton(row, "🎨", dark) { showColorPalette() }
        fmtButton(row, "Aa", dark) { showSizeMenu() }
        fmtButton(row, "•", dark) { toggleBullets() }
        fmtButton(row, "1.", dark) { toggleNumbered() }
        fmtButton(row, "😀", dark) { showEmojiPanel() }
        fmtButton(row, "📄", dark) { showPaperMenu() }
        fmtButton(row, "⬇️", dark) { showExportDialog() }

        fbar.addView(row, ViewGroupParams())
        root.addView(fbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        setContentView(root)
    }

    private fun ViewGroupParams(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT)

    private fun barButton(txt: String, size: Float, dark: Boolean): TextView {
        val t = TextView(this)
        t.text = txt
        t.textSize = size
        t.setTextColor(Theme.text(dark))
        t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        Ui.press(t)
        return t
    }

    private fun fmtButton(row: LinearLayout, txt: String, dark: Boolean, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = txt
        t.textSize = 15f
        t.setTextColor(Theme.text(dark))
        t.typeface = Typeface.DEFAULT_BOLD
        t.gravity = Gravity.CENTER
        t.minWidth = dp(44f)
        t.minHeight = dp(40f)
        t.setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        t.background = Ui.glassBg(this, dark, 12f)
        t.setOnClickListener { onClick() }
        Ui.press(t)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8f)
        row.addView(t, lp)
        return t
    }

    private fun dialogTheme(): Int =
        if (Theme.isDark(this)) android.R.style.Theme_DeviceDefault_Dialog_Alert
        else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // =====================================================================
    // Paper
    // =====================================================================

    private fun applyPaper() {
        val dark = Theme.isDark(this)
        paper.paperStyle = note.paperStyle
        paper.lineColor = Papers.line(note.lineColor, dark)
        val pc = Papers.paper(note.paperColor, dark)
        paper.setBackgroundColor(pc)
        scroll.setBackgroundColor(pc)
        edit.setTextColor(Papers.textOn(note.paperColor, dark))
        paper.invalidate()
    }

    private fun showPaperMenu() {
        val dark = Theme.isDark(this)
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(dp(24f), dp(14f), dp(24f), dp(10f))

        wrap.addView(Ui.label(this, "نمط الورق", 13f, Theme.textSec(dark), true))
        val styles = arrayOf("سطور", "أبيض", "نقاط", "مربعات")
        val codes = arrayOf("ruled", "blank", "dots", "grid")
        val srow = LinearLayout(this)
        srow.orientation = LinearLayout.HORIZONTAL
        for (i in codes.indices) {
            val c = Ui.chip(this, styles[i], note.paperStyle == codes[i], dark, Theme.ACCENT)
            val clp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            clp.marginEnd = dp(8f)
            clp.topMargin = dp(6f)
            val idx = i
            c.setOnClickListener {
                note.paperStyle = codes[idx]
                applyPaper()
                scheduleSave()
                for (j in 0 until srow.childCount) {
                    val child = srow.getChildAt(j) as TextView
                    child.background = Ui.chipBg(this, j == idx, dark, Theme.ACCENT)
                    child.typeface = if (j == idx) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    child.setTextColor(if (j == idx) 0xFF1C1C1E.toInt() else Theme.text(dark))
                }
            }
            srow.addView(c, clp)
        }
        wrap.addView(srow)

        wrap.addView(Ui.label(this, "لون الورق", 13f, Theme.textSec(dark), true))
        val prow = LinearLayout(this)
        prow.orientation = LinearLayout.HORIZONTAL
        for (i in Papers.paperColors.indices) {
            val v = View(this)
            v.background = Ui.circle(Papers.paper(i, dark), Theme.stroke(dark))
            val plp = LinearLayout.LayoutParams(dp(34f), dp(34f))
            plp.marginEnd = dp(10f)
            plp.topMargin = dp(8f)
            val idx = i
            v.setOnClickListener { note.paperColor = idx; applyPaper(); scheduleSave() }
            prow.addView(v, plp)
        }
        wrap.addView(prow)

        wrap.addView(Ui.label(this, "لون السطور", 13f, Theme.textSec(dark), true))
        val lrow = LinearLayout(this)
        lrow.orientation = LinearLayout.HORIZONTAL
        for (i in Papers.lineColors.indices) {
            val v = View(this)
            v.background = Ui.circle(Papers.line(i, dark), Theme.stroke(dark))
            val llp = LinearLayout.LayoutParams(dp(34f), dp(34f))
            llp.marginEnd = dp(10f)
            llp.topMargin = dp(8f)
            val idx = i
            v.setOnClickListener { note.lineColor = idx; applyPaper(); scheduleSave() }
            lrow.addView(v, llp)
        }
        wrap.addView(lrow)

        AlertDialog.Builder(this, dialogTheme())
            .setTitle("شكل الورقة")
            .setView(wrap)
            .show()
    }

    // =====================================================================
    // Auto save
    // =====================================================================

    private val saveRunnable = Runnable { doSave("✓ تم الحفظ") }

    private fun scheduleSave() {
        status.text = "جارٍ الحفظ…"
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, 700)
    }

    private fun doSave(msg: String) {
        note.title = titleEdit.text.toString()
        note.content = HtmlText.toHtml(edit.text)
        note.modifiedAt = System.currentTimeMillis()
        NoteStore.upsert(note)
        status.text = msg
    }

    private fun finishWithSave() {
        val empty = isNew && titleEdit.text.toString().isBlank() && edit.text.toString().isBlank()
        if (empty) {
            discarded = true
            NoteStore.removePermanent(note.id)
        } else {
            doSave("✓ تم الحفظ")
        }
        finish()
    }

    override fun onBackPressed() {
        finishWithSave()
    }

    override fun onPause() {
        super.onPause()
        if (!discarded) doSave("✓ تم الحفظ")
    }

    // =====================================================================
    // Undo / redo
    // =====================================================================

    private val historyRunnable = Runnable {
        pendingHistory = false
        lastCommitted = edit.text.toString()
    }

    private fun onTextEdited() {
        if (!pendingHistory) {
            pendingHistory = true
            undoStack.addFirst(Pair(lastCommitted, edit.selectionEnd))
            if (undoStack.size > 100) undoStack.removeLast()
            redoStack.clear()
        }
        handler.removeCallbacks(historyRunnable)
        handler.postDelayed(historyRunnable, 450)
        scheduleSave()
    }

    private fun doUndo() {
        if (undoStack.isEmpty()) return
        redoStack.addFirst(Pair(edit.text.toString(), edit.selectionEnd))
        applySnapshot(undoStack.removeFirst())
    }

    private fun doRedo() {
        if (redoStack.isEmpty()) return
        undoStack.addFirst(Pair(edit.text.toString(), edit.selectionEnd))
        applySnapshot(redoStack.removeFirst())
    }

    private fun applySnapshot(s: Pair<String, Int>) {
        restoring = true
        edit.setText(s.first)
        val pos = min(s.second, edit.text.length).coerceAtLeast(0)
        edit.setSelection(pos)
        restoring = false
        lastCommitted = s.first
        scheduleSave()
        paper.invalidate()
    }

    // =====================================================================
    // Text formatting
    // =====================================================================

    private fun selRange(): Pair<Int, Int>? {
        val a = edit.selectionStart
        val b = edit.selectionEnd
        if (a < 0 || b < 0 || a == b) return null
        return Pair(min(a, b), max(a, b))
    }

    private fun toggleStyleSpan(style: Int) {
        val r = selRange()
        if (r == null) { toast("حدّد النص أولاً"); return }
        val t = edit.editableText
        val spans = t.getSpans(r.first, r.second, StyleSpan::class.java).filter { it.style == style }
        if (spans.isNotEmpty()) spans.forEach { t.removeSpan(it) }
        else t.setSpan(StyleSpan(style), r.first, r.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        scheduleSave()
    }

    private fun toggleUnderline() {
        val r = selRange()
        if (r == null) { toast("حدّد النص أولاً"); return }
        val t = edit.editableText
        val spans = t.getSpans(r.first, r.second, UnderlineSpan::class.java)
        if (spans.isNotEmpty()) spans.forEach { t.removeSpan(it) }
        else t.setSpan(UnderlineSpan(), r.first, r.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        scheduleSave()
    }

    private fun toggleHighlight() {
        val r = selRange()
        if (r == null) { toast("حدّد النص أولاً"); return }
        val t = edit.editableText
        val spans = t.getSpans(r.first, r.second, BackgroundColorSpan::class.java)
        if (spans.isNotEmpty()) spans.forEach { t.removeSpan(it) }
        else t.setSpan(BackgroundColorSpan(0xFFFFE082.toInt()), r.first, r.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        scheduleSave()
    }

    private fun applyTextColor(color: Int) {
        val r = selRange()
        if (r == null) { toast("حدّد النص أولاً"); return }
        val t = edit.editableText
        t.getSpans(r.first, r.second, ForegroundColorSpan::class.java).forEach { t.removeSpan(it) }
        t.setSpan(ForegroundColorSpan(color), r.first, r.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        scheduleSave()
    }

    private fun toggleBullets() {
        transformLines { line -> if (line.startsWith("• ")) line.substring(2) else "• $line" }
    }

    private fun transformLines(f: (String) -> String) {
        val t = edit.editableText
        var a = edit.selectionStart.coerceAtLeast(0)
        var b = edit.selectionEnd.coerceAtLeast(0)
        if (a > b) { val tmp = a; a = b; b = tmp }
        var ls = a
        while (ls > 0 && t[ls - 1] != '\n') ls--
        var le = b
        while (le < t.length && t[le] != '\n') le++
        val region = t.subSequence(ls, le).toString()
        val parts = region.split("\n")
        val sb = StringBuilder()
        parts.forEachIndexed { idx, line ->
            if (idx > 0) sb.append("\n")
            sb.append(f(line))
        }
        restoring = true
        t.replace(ls, le, sb.toString())
        restoring = false
        lastCommitted = edit.text.toString()
        edit.setSelection(min(ls + sb.length, edit.text.length))
        scheduleSave()
        paper.invalidate()
    }

    private fun toggleNumbered() {
        val t = edit.editableText
        var a = edit.selectionStart.coerceAtLeast(0)
        var b = edit.selectionEnd.coerceAtLeast(0)
        if (a > b) { val tmp = a; a = b; b = tmp }
        var ls = a
        while (ls > 0 && t[ls - 1] != '\n') ls--
        var le = b
        while (le < t.length && t[le] != '\n') le++
        val region = t.subSequence(ls, le).toString()
        val parts = region.split("\n")
        val numbered = parts.any { Regex("^\\d+\\.\\s").containsMatchIn(it) }
        val sb = StringBuilder()
        var n = 1
        parts.forEachIndexed { idx, line ->
            if (idx > 0) sb.append("\n")
            if (numbered) sb.append(line.replaceFirst(Regex("^\\d+\\.\\s"), ""))
            else { sb.append("$n. "); sb.append(line); n++ }
        }
        restoring = true
        t.replace(ls, le, sb.toString())
        restoring = false
        lastCommitted = edit.text.toString()
        edit.setSelection(min(ls + sb.length, edit.text.length))
        scheduleSave()
        paper.invalidate()
    }

    // =====================================================================
    // Menus / dialogs
    // =====================================================================

    private fun showSizeMenu() {
        val names = arrayOf("صغير 14", "عادي 17", "كبير 20", "ضخم 24")
        val sizes = intArrayOf(14, 17, 20, 24)
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("حجم الخط")
            .setItems(names) { _, w ->
                note.fontSize = sizes[w]
                edit.textSize = sizes[w].toFloat()
                paper.invalidate()
                scheduleSave()
            }
            .show()
    }

    private fun showColorPalette() {
        val dark = Theme.isDark(this)
        val colors = intArrayOf(
            0xFF1C1C1E.toInt(), 0xFFC0392B.toInt(), 0xFFE67E22.toInt(), 0xFF27AE60.toInt(),
            0xFF2980B9.toInt(), 0xFF8E44AD.toInt(), 0xFFD81B60.toInt(), 0xFF00838F.toInt())
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.gravity = Gravity.CENTER_HORIZONTAL
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.setPadding(dp(16f), dp(12f), dp(16f), dp(6f))
        for (c in colors) {
            val v = View(this)
            v.background = Ui.circle(c, Theme.stroke(dark))
            val vlp = LinearLayout.LayoutParams(dp(32f), dp(32f))
            vlp.marginEnd = dp(9f)
            v.setOnClickListener { applyTextColor(c) }
            row.addView(v, vlp)
        }
        wrap.addView(row)
        val close = Ui.chip(this, "تم", false, dark, Theme.ACCENT)
        val dlg = AlertDialog.Builder(this, dialogTheme())
            .setTitle("لون النص — حدد النص أولاً")
            .setView(wrap)
            .create()
        close.setOnClickListener { dlg.dismiss() }
        val clp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        clp.topMargin = dp(8f)
        clp.bottomMargin = dp(10f)
        wrap.addView(close, clp)
        dlg.show()
    }

    private fun showEmojiPanel() {
        val emojis = arrayOf(
            "😀", "😂", "🥰", "😍", "😎", "🤩", "😭", "😡",
            "👍", "👏", "🙏", "💪", "🔥", "✨", "🌟", "❤️",
            "💙", "💚", "🎉", "🎈", "🎁", "☕", "🍕", "🍰",
            "🌸", "🌈", "☀️", "🌙", "⚡", "❄️", "🎵", "📚",
            "✅", "❌", "‼️", "❓", "💡", "📌", "📝", "🗓️",
            "⏰", "✈️", "🚗", "🏠", "⚽", "🏆", "🎯", "🧠")
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
        emojis.toList().chunked(6).forEach { chunk ->
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER
            chunk.forEach { e ->
                val t = TextView(this)
                t.text = e
                t.textSize = 22f
                t.setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
                t.setOnClickListener { insertText(e) }
                r.addView(t)
            }
            wrap.addView(r)
        }
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("الرموز التعبيرية")
            .setView(wrap)
            .show()
    }

    private fun insertText(s: String) {
        val pos = edit.selectionEnd.coerceAtLeast(0).coerceAtMost(edit.text.length)
        edit.text.insert(pos, s)
    }

    private fun showMoreMenu() {
        val items = arrayOf("إحصائيات الملاحظة 📊", "نقل إلى مجلد 📁", "تصدير كصورة PNG ⬇️")
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("خيارات")
            .setItems(items) { _, w ->
                when (w) {
                    0 -> showStats()
                    1 -> showFolderPicker()
                    2 -> showExportDialog()
                }
            }
            .show()
    }

    private fun showStats() {
        val text = edit.text.toString()
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val chars = text.length
        val mins = if (words == 0) 0 else ceil(words / 200.0).toInt()
        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        val reading = if (mins == 0) "أقل من دقيقة" else "$mins دقيقة"
        val msg = "عدد الكلمات: $words\n" +
                "عدد الأحرف: $chars\n" +
                "وقت القراءة: $reading\n\n" +
                "أُنشئت: ${df.format(java.util.Date(note.createdAt))}\n" +
                "آخر تعديل: ${df.format(java.util.Date(note.modifiedAt))}"
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("إحصائيات")
            .setMessage(msg)
            .setPositiveButton("حسنًا", null)
            .show()
    }

    private fun showFolderPicker() {
        val opts = mutableListOf("بدون مجلد")
        opts.addAll(NoteStore.folders(this).sorted())
        opts.add("➕ مجلد جديد")
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("نقل إلى مجلد")
            .setItems(opts.toTypedArray()) { _, w ->
                when {
                    w == 0 -> { note.folder = null; scheduleSave(); toast("تم النقل") }
                    w == opts.size - 1 -> promptNewFolder { name ->
                        note.folder = name
                        NoteStore.addFolder(this, name)
                        scheduleSave()
                    }
                    else -> { note.folder = opts[w]; scheduleSave(); toast("تم النقل") }
                }
            }
            .show()
    }

    private fun promptNewFolder(cb: (String) -> Unit) {
        val input = EditText(this)
        input.hint = "اسم المجلد"
        input.setPadding(dp(48f), dp(32f), dp(48f), dp(16f))
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("مجلد جديد")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) cb(n)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // =====================================================================
    // Export as PNG
    // =====================================================================

    private fun showExportDialog() {
        val dark = Theme.isDark(this)
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(dp(28f), dp(18f), dp(28f), dp(12f))
        wrap.addView(Ui.label(this, "اختر جودة الصورة", 13f, Theme.textSec(dark), true))

        val qualities = arrayOf("عادية (1080px)", "عالية (1440px)", "فائقة (2160px)")
        val widths = intArrayOf(1080, 1440, 2160)
        var chosen = 1
        val rg = RadioGroup(this)
        rg.orientation = LinearLayout.VERTICAL
        qualities.forEachIndexed { i, q ->
            val rb = RadioButton(this)
            rb.id = 100 + i
            rb.text = q
            rb.textSize = 15f
            rb.setTextColor(Theme.text(dark))
            rb.isChecked = i == chosen
            rg.addView(rb)
        }
        rg.setOnCheckedChangeListener { _, checkedId -> chosen = checkedId - 100 }
        wrap.addView(rg)

        val rowBtns = LinearLayout(this)
        rowBtns.orientation = LinearLayout.HORIZONTAL
        rowBtns.gravity = Gravity.CENTER
        val saveB = bigButton("💾 حفظ في المعرض")
        val shareB = bigButton("📤 مشاركة")
        val blp1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        blp1.marginEnd = dp(8f)
        rowBtns.addView(saveB, blp1)
        rowBtns.addView(shareB, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        wrap.addView(rowBtns)

        val dlg = AlertDialog.Builder(this, dialogTheme())
            .setTitle("تصدير الملاحظة كصورة")
            .setView(wrap)
            .create()
        saveB.setOnClickListener { doExport(widths[chosen], false, dlg) }
        shareB.setOnClickListener { doExport(widths[chosen], true, dlg) }
        dlg.show()
    }

    private fun bigButton(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 14f
        t.typeface = Typeface.DEFAULT_BOLD
        t.setTextColor(0xFF1C1C1E.toInt())
        t.gravity = Gravity.CENTER
        t.setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        val g = GradientDrawable()
        g.setShape(GradientDrawable.RECTANGLE)
        g.setCornerRadius(dp(14f).toFloat())
        g.setColor(Theme.ACCENT)
        t.background = g
        Ui.press(t)
        return t
    }

    private fun doExport(width: Int, shareAfter: Boolean, dlg: AlertDialog?) {
        if (Build.VERSION.SDK_INT < 29 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingExport = { granted ->
                if (granted) runExport(width, shareAfter) else toast("إذن التخزين مطلوب")
            }
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 7)
            return
        }
        runExport(width, shareAfter)
        dlg?.dismiss()
    }

    private fun runExport(width: Int, shareAfter: Boolean) {
        doSave("✓ تم الحفظ")
        toast("جارٍ إنشاء الصورة…")
        Thread {
            runCatching {
                val snapshot = note.copy()
                snapshot.title = titleEdit.text.toString()
                snapshot.content = HtmlText.toHtml(edit.text)
                val bmp = ExportUtils.render(snapshot, width, Theme.isDark(this))
                runOnUiThread {
                    val uri = ExportUtils.saveToGallery(this, bmp)
                    if (uri == null) {
                        toast("تعذر الحفظ")
                    } else if (shareAfter) {
                        ExportUtils.share(this, uri)
                    } else {
                        toast("✓ حُفظت الصورة في Pictures/MyNotes")
                    }
                }
            }.onFailure {
                runOnUiThread { toast("حدث خطأ أثناء التصدير") }
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            pendingExport?.invoke(granted)
            pendingExport = null
        }
    }
}
