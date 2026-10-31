package com.notes.premium

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private var tab = 0 // 0 notes, 1 favorites, 2 folders, 3 trash
    private var query = ""
    private var openFolder: String? = null
    private var appliedDark: Boolean? = null

    private lateinit var listHost: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var subtitle: TextView
    private lateinit var tabButtons: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NoteStore.init(applicationContext)
        Theme.apply(this)
        appliedDark = Theme.isDark(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        val d = Theme.isDark(this)
        if (appliedDark != null && appliedDark != d) {
            appliedDark = d
            Theme.apply(this)
            recreate()
            return
        }
        Theme.apply(this)
        render()
    }

    // =====================================================================
    // UI
    // =====================================================================

    private fun dp(v: Float) = Ui.dp(this, v)

    private fun buildUi() {
        val dark = Theme.isDark(this)
        val root = FrameLayout(this)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(Theme.bg(dark))

        // header
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(22f), dp(16f), dp(22f), dp(4f))
        header.addView(
            Ui.label(this, "ملاحظاتي", 26f, Theme.text(dark), true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val gear = Ui.label(this, "⚙", 22f, Theme.text(dark))
        gear.setPadding(dp(8f), dp(8f), 0, dp(8f))
        gear.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        Ui.press(gear)
        header.addView(gear)
        main.addView(header, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        subtitle = Ui.label(this, "", 12f, Theme.textSec(dark))
        subtitle.setPadding(dp(24f), 0, dp(24f), dp(6f))
        main.addView(subtitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // search
        val searchWrap = LinearLayout(this)
        searchWrap.orientation = LinearLayout.VERTICAL
        searchWrap.setPadding(dp(22f), dp(6f), dp(22f), dp(10f))
        val search = EditText(this)
        search.hint = "🔍 بحث في الملاحظات"
        search.textSize = 15f
        search.setTextColor(Theme.text(dark))
        search.setHintTextColor(Theme.textSec(dark))
        search.background = Ui.glassBg(this, dark, 16f)
        search.setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        search.inputType = InputType.TYPE_CLASS_TEXT
        search.isSingleLine = true
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s.toString().trim()
                render()
            }
        })
        searchWrap.addView(search, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        main.addView(searchWrap, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // notes list
        scroll = ScrollView(this)
        listHost = LinearLayout(this)
        listHost.orientation = LinearLayout.VERTICAL
        listHost.setPadding(dp(14f), dp(4f), dp(14f), dp(140f))
        scroll.addView(listHost, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        main.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // bottom tab bar
        val tabBar = LinearLayout(this)
        tabBar.orientation = LinearLayout.HORIZONTAL
        tabBar.gravity = Gravity.CENTER_VERTICAL
        tabBar.setPadding(dp(8f), dp(10f), dp(8f), dp(16f))
        val tb = GradientDrawable()
        tb.setShape(GradientDrawable.RECTANGLE)
        val r = Ui.dpF(this, 22f)
        tb.setCornerRadii(floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f))
        tb.setColor(if (dark) 0xF21B1B23.toInt() else 0xF7FFFFFF.toInt())
        tb.setStroke(1, Theme.stroke(dark))
        tabBar.background = tb
        tabBar.elevation = Ui.dpF(this, 12f)

        val tabs = arrayOf("🗒️\nملاحظات", "⭐\nالمفضلة", "📁\nالمجلدات", "🗑️\nالمهملات")
        val buttons = ArrayList<TextView>()
        tabs.forEachIndexed { i, t ->
            val b = TextView(this)
            b.text = t
            b.textSize = 11f
            b.gravity = Gravity.CENTER
            b.setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            b.setOnClickListener { tab = i; openFolder = null; styleTabs(); render() }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            tabBar.addView(b, lp)
            buttons.add(b)
        }
        tabButtons = buttons
        styleTabs()
        main.addView(tabBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(main, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // floating action button
        val fab = TextView(this)
        fab.text = "＋"
        fab.textSize = 30f
        fab.setTextColor(0xFF1C1C1E.toInt())
        fab.typeface = Typeface.DEFAULT_BOLD
        fab.gravity = Gravity.CENTER
        val fg = GradientDrawable()
        fg.setShape(GradientDrawable.OVAL)
        fg.setColor(Theme.ACCENT)
        fab.background = fg
        fab.elevation = Ui.dpF(this, 10f)
        fab.setOnClickListener {
            val i = Intent(this, EditorActivity::class.java)
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        Ui.press(fab)
        val flp = FrameLayout.LayoutParams(dp(62f), dp(62f))
        flp.gravity = Gravity.BOTTOM or Gravity.END
        flp.marginEnd = dp(20f)
        flp.bottomMargin = dp(96f)
        root.addView(fab, flp)

        setContentView(root)
    }

    private fun styleTabs() {
        val dark = Theme.isDark(this)
        tabButtons.forEachIndexed { i, b ->
            if (i == tab) {
                b.setTextColor(if (dark) Theme.ACCENT else Theme.ACCENT_DARK)
                b.typeface = Typeface.DEFAULT_BOLD
            } else {
                b.setTextColor(Theme.textSec(dark))
                b.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun dialogTheme(): Int =
        if (Theme.isDark(this)) android.R.style.Theme_DeviceDefault_Dialog_Alert
        else android.R.style.Theme_DeviceDefault_Light_Dialog_Alert

    // =====================================================================
    // Rendering
    // =====================================================================

    private fun folderLabel(): String =
        if (openFolder == "__none__") "بدون مجلد" else (openFolder ?: "")

    private fun render() {
        val dark = Theme.isDark(this)
        listHost.removeAllViews()
        subtitle.text = when (tab) {
            1 -> "ملاحظاتك المفضلة ⭐"
            2 -> if (openFolder == null) "نظّم ملاحظاتك في مجلدات 📁" else "📁 ${folderLabel()}"
            3 -> "المهملات — اضغط ضغطة مطوّلة للاستعادة"
            else -> "كل ملاحظاتك في مكان واحد ✨"
        }
        if (tab == 2 && openFolder == null) {
            renderFoldersList(dark)
            return
        }

        var list: List<Note> = NoteStore.notes.filter { n ->
            val alive = if (tab == 3) n.trashed else !n.trashed
            val favOk = if (tab == 1) n.favorite else true
            val folderOk = when (openFolder) {
                null -> true
                "__none__" -> n.folder == null
                else -> n.folder == openFolder
            }
            alive && favOk && folderOk
        }
        if (query.isNotEmpty()) {
            list = list.filter { (it.title + " " + it.snippet()).contains(query, true) }
        }
        list = if (tab == 3) list.sortedByDescending { it.modifiedAt }
        else list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.modifiedAt })

        if (tab == 2 && openFolder != null) {
            val back = Ui.label(this, "‹  كل المجلدات", 14f,
                if (dark) Theme.ACCENT else Theme.ACCENT_DARK, true)
            back.setPadding(dp(6f), dp(4f), dp(6f), dp(12f))
            back.setOnClickListener { openFolder = null; render() }
            listHost.addView(back, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        if (list.isEmpty()) {
            listHost.addView(emptyView(dark), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            return
        }

        val twoCols = resources.displayMetrics.widthPixels >= dp(340f)
        if (twoCols) {
            list.chunked(2).forEach { rowNotes ->
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                rowNotes.forEachIndexed { idx, n ->
                    val clp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    clp.bottomMargin = dp(12f)
                    if (idx == 0) clp.marginEnd = dp(6f)
                    row.addView(noteCard(n, dark), clp)
                }
                if (rowNotes.size == 1) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))
                }
                listHost.addView(row, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
        } else {
            list.forEach { n ->
                val clp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                clp.bottomMargin = dp(12f)
                listHost.addView(noteCard(n, dark), clp)
            }
        }

        if (tab == 3) addPurgeButton(dark)
    }

    private fun renderFoldersList(dark: Boolean) {
        val add = TextView(this)
        add.text = "➕ مجلد جديد"
        add.textSize = 15f
        add.typeface = Typeface.DEFAULT_BOLD
        add.setTextColor(if (dark) Theme.ACCENT else Theme.ACCENT_DARK)
        add.gravity = Gravity.CENTER
        add.background = Ui.glassBg(this, dark, 18f)
        add.setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        add.elevation = Ui.dpF(this, 5f)
        Ui.press(add)
        add.setOnClickListener { promptNewFolder { name -> NoteStore.addFolder(this, name); render() } }
        val alp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        alp.bottomMargin = dp(10f)
        listHost.addView(add, alp)

        val folders = NoteStore.folders(this).sorted()
        val unfiled = NoteStore.notes.count { !it.trashed && it.folder == null }
        if (folders.isEmpty() && unfiled == 0) {
            listHost.addView(emptyView(dark), LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            return
        }

        val none = folderRow("📒 بدون مجلد", unfiled, dark)
        none.setOnClickListener { openFolder = "__none__"; render() }
        listHost.addView(none, rowLp())

        folders.forEach { f ->
            val cnt = NoteStore.notes.count { it.folder == f && !it.trashed }
            val row = folderRow("📁 $f", cnt, dark)
            row.setOnClickListener { openFolder = f; render() }
            listHost.addView(row, rowLp())
        }
    }

    private fun rowLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10f)
        return lp
    }

    private fun folderRow(title: String, count: Int, dark: Boolean): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.background = Ui.glassBg(this, dark, 18f)
        r.setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        r.elevation = Ui.dpF(this, 5f)
        r.addView(
            Ui.label(this, title, 15f, Theme.text(dark), true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (count >= 0) r.addView(Ui.label(this, "$count ملاحظة", 12f, Theme.textSec(dark)))
        Ui.press(r)
        return r
    }

    private fun addPurgeButton(dark: Boolean) {
        val hasTrash = NoteStore.notes.any { it.trashed }
        if (!hasTrash) return
        val purge = TextView(this)
        purge.text = "🧹 تفريغ المهملات نهائيًا"
        purge.textSize = 14f
        purge.typeface = Typeface.DEFAULT_BOLD
        purge.setTextColor(0xFFC0392B.toInt())
        purge.gravity = Gravity.CENTER
        purge.background = Ui.glassBg(this, dark, 16f)
        purge.setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        purge.setOnClickListener {
            AlertDialog.Builder(this, dialogTheme())
                .setMessage("هل تريد حذف كل الملاحظات في المهملات نهائيًا؟")
                .setPositiveButton("حذف") { _, _ ->
                    NoteStore.notes.removeAll { it.trashed }
                    NoteStore.save()
                    render()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
        val plp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        plp.topMargin = dp(10f)
        listHost.addView(purge, plp)
    }

    private fun noteCard(n: Note, dark: Boolean): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = Ui.glassBg(this, dark, 20f)
        c.elevation = Ui.dpF(this, 6f)
        c.setPadding(dp(16f), dp(14f), dp(16f), dp(12f))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        if (n.paperColor > 0) {
            val dot = View(this)
            dot.background = Ui.circle(Papers.paper(n.paperColor, dark), Theme.stroke(dark))
            val dlp = LinearLayout.LayoutParams(dp(14f), dp(14f))
            dlp.marginEnd = dp(6f)
            top.addView(dot, dlp)
        }
        val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        val meta = df.format(Date(n.modifiedAt)) +
                (if (n.folder != null) "  •  📁 " + n.folder else "")
        top.addView(
            Ui.label(this, meta, 11f, Theme.textSec(dark)),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (n.pinned) top.addView(Ui.label(this, "📌", 11f, Theme.textSec(dark)))
        if (n.favorite) top.addView(Ui.label(this, "⭐", 11f, Theme.textSec(dark)))
        c.addView(top, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val t = Ui.label(this,
            if (n.title.isBlank()) "بدون عنوان" else n.title,
            16f, Theme.text(dark), true)
        t.maxLines = 2
        val tlp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        tlp.topMargin = dp(6f)
        c.addView(t, tlp)

        val s = Ui.label(this, n.snippet().ifBlank { "ملاحظة فارغة" }, 13f, Theme.textSec(dark))
        s.maxLines = 4
        val slp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        slp.topMargin = dp(4f)
        c.addView(s, slp)

        c.setOnClickListener { openEditor(n.id) }
        c.setOnLongClickListener { noteActions(n); true }
        Ui.press(c)
        return c
    }

    private fun emptyView(dark: Boolean): LinearLayout {
        val w = LinearLayout(this)
        w.orientation = LinearLayout.VERTICAL
        w.gravity = Gravity.CENTER
        w.setPadding(dp(30f), dp(90f), dp(30f), dp(30f))
        val emoji = when (tab) { 1 -> "⭐"; 2 -> "📁"; 3 -> "🧹"; else -> "🗒️" }
        w.addView(Ui.label(this, emoji, 44f, Theme.text(dark)), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val msg = when {
            tab == 1 -> "لا توجد ملاحظات مفضلة بعد"
            tab == 2 -> "أنشئ أول مجلد من الزر بالأعلى ➕"
            tab == 3 -> "المهملات فارغة"
            query.isNotEmpty() -> "لا نتائج مطابقة لبحثك"
            else -> "لا توجد ملاحظات بعد\nاضغط زر ＋ لإنشاء أول ملاحظة ✨"
        }
        val m = Ui.label(this, msg, 14f, Theme.textSec(dark))
        m.gravity = Gravity.CENTER
        val mlp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        mlp.topMargin = dp(10f)
        w.addView(m, mlp)
        return w
    }

    private fun openEditor(id: String) {
        val i = Intent(this, EditorActivity::class.java)
        i.putExtra("id", id)
        startActivity(i)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // =====================================================================
    // Note actions
    // =====================================================================

    private fun noteActions(n: Note) {
        val items: Array<String> = if (n.trashed)
            arrayOf("♻️ استعادة", "🗑️ حذف نهائي")
        else
            arrayOf(
                if (n.pinned) "📌 إلغاء التثبيت" else "📌 تثبيت",
                if (n.favorite) "☆ إزالة من المفضلة" else "⭐ إضافة للمفضلة",
                "📁 نقل إلى مجلد",
                "🖼️ تصدير كصورة",
                "🗑️ نقل إلى المهملات")
        val title = n.title.ifBlank { "ملاحظة" }.take(30)
        AlertDialog.Builder(this, dialogTheme())
            .setTitle(title)
            .setItems(items) { _, w ->
                when {
                    n.trashed && w == 0 -> { n.trashed = false; NoteStore.upsert(n); render() }
                    n.trashed && w == 1 -> { NoteStore.removePermanent(n.id); render() }
                    w == 0 -> { n.pinned = !n.pinned; NoteStore.upsert(n); render() }
                    w == 1 -> { n.favorite = !n.favorite; NoteStore.upsert(n); render() }
                    w == 2 -> moveDialog(n)
                    w == 3 -> exportFromCard(n)
                    w == 4 -> { n.trashed = true; n.pinned = false; NoteStore.upsert(n); render() }
                }
            }
            .show()
    }

    private fun moveDialog(n: Note) {
        val opts = mutableListOf("بدون مجلد")
        opts.addAll(NoteStore.folders(this).sorted())
        opts.add("➕ مجلد جديد")
        AlertDialog.Builder(this, dialogTheme())
            .setTitle("نقل إلى مجلد")
            .setItems(opts.toTypedArray()) { _, w ->
                when {
                    w == 0 -> { n.folder = null; NoteStore.upsert(n); render() }
                    w == opts.size - 1 -> promptNewFolder { name ->
                        n.folder = name
                        NoteStore.addFolder(this, name)
                        NoteStore.upsert(n)
                        render()
                    }
                    else -> { n.folder = opts[w]; NoteStore.upsert(n); render() }
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
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) cb(name)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun exportFromCard(n: Note) {
        if (Build.VERSION.SDK_INT < 29 &&
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 8)
            return
        }
        Toast.makeText(this, "جارٍ إنشاء الصورة…", Toast.LENGTH_SHORT).show()
        Thread {
            runCatching {
                val bmp = ExportUtils.render(n, 1440, Theme.isDark(this))
                runOnUiThread {
                    val uri = ExportUtils.saveToGallery(this, bmp)
                    if (uri != null) Toast.makeText(this, "✓ حُفظت الصورة في المعرض", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "تعذر الحفظ", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 8 && grantResults.isNotEmpty() &&
            grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم منح الإذن — جرب مرة أخرى", Toast.LENGTH_SHORT).show()
        }
    }
}
