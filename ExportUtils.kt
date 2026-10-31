package com.notes.premium

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint

object ExportUtils {

    /** Renders the whole note (title + content + paper lines) as one tall PNG. */
    fun render(note: Note, targetWidth: Int, dark: Boolean): Bitmap {
        var width = targetWidth
        var bmp = renderOnce(note, width, dark)
        if (bmp.height > 14000) {
            val k = 14000f / bmp.height
            bmp.recycle()
            width = (width * k).toInt().coerceAtLeast(720)
            bmp = renderOnce(note, width, dark)
        }
        return bmp
    }

    private fun renderOnce(note: Note, width: Int, dark: Boolean): Bitmap {
        val s = width / 1080f
        val pad = (84 * s).toInt()
        val contentW = width - pad * 2
        val paper = Papers.paper(note.paperColor, dark)
        val lineC = Papers.line(note.lineColor, dark)
        val txt = Papers.textOn(note.paperColor, dark)

        val titleText = HtmlText.plain(note.title).ifBlank { "بدون عنوان" }
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.textSize = 60f * s
        tp.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        tp.color = txt
        val tLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, tp, contentW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG)
            .setLineSpacing(0f, 1.15f)
            .build()

        val body: CharSequence = HtmlText.fromHtml(note.content.ifBlank { " " })
        val bp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        bp.textSize = (note.fontSize * 2.63f) * s
        bp.color = txt
        val extra = 12f * 2.63f * s
        val bLayout = StaticLayout.Builder.obtain(body, 0, body.length, bp, contentW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG)
            .setLineSpacing(extra, 1f)
            .build()

        val top = (96 * s).toInt()
        val gap = (40 * s).toInt()
        val bottom = (130 * s).toInt()
        val h = top + tLayout.height + gap + bLayout.height + bottom

        val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(paper)

        val contentTop = top + tLayout.height + gap

        if (note.paperStyle != "blank") {
            val lp = Paint(Paint.ANTI_ALIAS_FLAG)
            lp.color = lineC
            lp.strokeWidth = maxOf(1f, s * 1.4f)
            val off = 5f * 2.63f * s
            val step = bp.fontSpacing + extra
            if (note.paperStyle == "ruled" || note.paperStyle == "grid") {
                var y = contentTop + off
                var i = 0
                while (i < bLayout.lineCount || y < h - 10) {
                    c.drawLine(pad.toFloat(), y, (width - pad).toFloat(), y, lp)
                    i++
                    y = if (i < bLayout.lineCount) contentTop + bLayout.getLineBaseline(i) + off else y + step
                }
                if (note.paperStyle == "grid") {
                    var x = pad.toFloat()
                    while (x < width - pad) {
                        c.drawLine(x, contentTop.toFloat(), x, h.toFloat(), lp)
                        x += 26f * 2.63f * s
                    }
                }
            } else if (note.paperStyle == "dots") {
                var y = contentTop + off
                var i = 0
                while (i < bLayout.lineCount || y < h - 10) {
                    var x = pad.toFloat()
                    while (x < width - pad) {
                        c.drawCircle(x, y, 1.7f * s, lp)
                        x += 26f * 2.63f * s
                    }
                    i++
                    y = if (i < bLayout.lineCount) contentTop + bLayout.getLineBaseline(i) + off else y + step
                }
            }
        }

        c.save()
        c.translate(pad.toFloat(), top.toFloat())
        tLayout.draw(c)
        c.restore()

        c.save()
        c.translate(pad.toFloat(), contentTop.toFloat())
        bLayout.draw(c)
        c.restore()

        return bmp
    }

    /** Saves the bitmap into Pictures/MyNotes and returns its content Uri. */
    fun saveToGallery(ctx: Context, bmp: Bitmap): Uri? {
        val name = "note-" + System.currentTimeMillis() + ".png"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues()
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyNotes")
                cv.put(MediaStore.MediaColumns.IS_PENDING, 1)
                val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: return null
                ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val done = ContentValues()
                done.put(MediaStore.MediaColumns.IS_PENDING, 0)
                ctx.contentResolver.update(uri, done, null, null)
                uri
            } else {
                val cv = ContentValues()
                cv.put(MediaStore.Images.Media.DISPLAY_NAME, name)
                cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                val uri = MediaStore.Images.Media.insert(
                    ctx.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
                ) ?: return null
                ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                uri
            }
        } catch (e: Exception) {
            null
        }
    }

    fun share(ctx: Context, uri: Uri) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "image/png"
        i.putExtra(Intent.EXTRA_STREAM, uri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "مشاركة الملاحظة"))
    }
}
