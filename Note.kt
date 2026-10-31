package com.notes.premium

import org.json.JSONObject
import java.util.UUID

data class Note(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    var folder: String? = null,
    var pinned: Boolean = false,
    var favorite: Boolean = false,
    var trashed: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis(),
    var paperStyle: String = "ruled",
    var paperColor: Int = 0,
    var lineColor: Int = 0,
    var fontSize: Int = 17
) {

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("title", title)
        o.put("content", content)
        o.put("folder", folder ?: JSONObject.NULL)
        o.put("pinned", pinned)
        o.put("favorite", favorite)
        o.put("trashed", trashed)
        o.put("createdAt", createdAt)
        o.put("modifiedAt", modifiedAt)
        o.put("paperStyle", paperStyle)
        o.put("paperColor", paperColor)
        o.put("lineColor", lineColor)
        o.put("fontSize", fontSize)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Note {
            return Note(
                id = o.optString("id", UUID.randomUUID().toString()),
                title = o.optString("title"),
                content = o.optString("content"),
                folder = if (o.isNull("folder")) null else o.optString("folder"),
                pinned = o.optBoolean("pinned", false),
                favorite = o.optBoolean("favorite", false),
                trashed = o.optBoolean("trashed", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                modifiedAt = o.optLong("modifiedAt", System.currentTimeMillis()),
                paperStyle = o.optString("paperStyle", "ruled"),
                paperColor = o.optInt("paperColor", 0),
                lineColor = o.optInt("lineColor", 0),
                fontSize = o.optInt("fontSize", 17)
            )
        }
    }

    fun snippet(): String {
        val plain = HtmlText.plain(content)
        return plain.replace('\n', ' ').trim().take(300)
    }
}
