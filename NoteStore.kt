package com.notes.premium

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executors

object NoteStore {

    private val io = Executors.newSingleThreadExecutor()
    private lateinit var file: File
    private lateinit var prefs: SharedPreferences
    private var initialized = false
    private var dirty = false

    val notes = ArrayList<Note>()

    @Synchronized
    fun init(ctx: Context) {
        if (initialized) return
        initialized = true
        file = File(ctx.filesDir, "notes.json")
        prefs = ctx.getSharedPreferences("mynotes_settings", Context.MODE_PRIVATE)
        synchronized(this) { notes.clear() }
        runCatching {
            if (file.exists()) {
                val arr = JSONArray(file.readText(Charsets.UTF_8))
                synchronized(this) {
                    for (i in 0 until arr.length()) notes.add(Note.fromJson(arr.getJSONObject(i)))
                }
            }
        }
    }

    // ---------- settings ----------
    fun darkMode(ctx: Context): Int = if (::prefs.isInitialized) prefs.getInt("darkMode", 0) else 0
    fun setDarkMode(ctx: Context, v: Int) { prefs.edit().putInt("darkMode", v).apply() }

    fun defaultPaper(ctx: Context): String = prefs.getString("defaultPaper", "ruled") ?: "ruled"
    fun setDefaultPaper(ctx: Context, v: String) { prefs.edit().putString("defaultPaper", v).apply() }

    fun defaultFont(ctx: Context): Int = prefs.getInt("defaultFont", 17)
    fun setDefaultFont(ctx: Context, v: Int) { prefs.edit().putInt("defaultFont", v).apply() }

    fun folders(ctx: Context): Set<String> = prefs.getStringSet("folders", emptySet())?.toSet() ?: emptySet()
    fun addFolder(ctx: Context, name: String) {
        val s = folders(ctx).toMutableSet()
        s.add(name)
        prefs.edit().putStringSet("folders", s).apply()
    }

    // ---------- persistence ----------
    @Synchronized
    fun save() {
        dirty = true
        io.execute {
            if (!dirty) return@execute
            dirty = false
            runCatching {
                val arr = JSONArray()
                synchronized(this@NoteStore) { notes.forEach { arr.put(it.toJson()) } }
                val tmp = File(file.parentFile, "notes.json.tmp")
                tmp.writeText(arr.toString(), Charsets.UTF_8)
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }

    @Synchronized
    fun upsert(n: Note) {
        val i = notes.indexOfFirst { it.id == n.id }
        if (i >= 0) notes[i] = n else notes.add(0, n)
        save()
    }

    fun get(id: String?): Note? = notes.firstOrNull { it.id == id }

    @Synchronized
    fun removePermanent(id: String) {
        notes.removeAll { it.id == id }
        save()
    }
}
