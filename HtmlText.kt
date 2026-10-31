package com.notes.premium

import android.text.Html
import android.text.Spanned

object HtmlText {

    fun toHtml(s: CharSequence): String {
        return if (s is Spanned) {
            Html.toHtml(s, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            s.toString()
        }
    }

    fun fromHtml(h: String): Spanned {
        return Html.fromHtml(h, Html.FROM_HTML_MODE_LEGACY)
    }

    fun plain(h: String): String {
        return try {
            fromHtml(h).toString()
        } catch (e: Exception) {
            h
        }
    }
}
