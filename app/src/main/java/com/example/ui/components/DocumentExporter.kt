package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.TeacherProfile
import java.io.FileOutputStream

object DocumentExporter {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(context: Context, title: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Perangkat Mengajar")
        context.startActivity(shareIntent)
    }

    fun printOrSavePdf(context: Context, title: String, content: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = title.ifBlank { "Perangkat_Mengajar_AjarMerdeka" }.take(60)
        val cleanContent = content
            .replace(Regex("(?m)^#{1,3}\\s*"), "")
            .replace("**", "")
            .replace("`", "")

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var pageHeightPts = 842
                private var pageWidthPts = 595
                private var pagesText = listOf<List<String>>()

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val mediaSize = newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
                    pageWidthPts = (mediaSize.widthMils * 72f / 1000f).toInt().coerceAtLeast(400)
                    pageHeightPts = (mediaSize.heightMils * 72f / 1000f).toInt().coerceAtLeast(550)

                    val margin = 44
                    val contentWidth = (pageWidthPts - margin * 2).coerceAtLeast(200)
                    val usableHeight = (pageHeightPts - margin * 2 - 64).coerceAtLeast(200)

                    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10.5f
                        color = android.graphics.Color.BLACK
                    }

                    val rawLines = cleanContent.split("\n")
                    val paged = mutableListOf<List<String>>()
                    var currentLines = mutableListOf<String>()
                    var currentHeight = 0

                    for (line in rawLines) {
                        val layout = StaticLayout.Builder
                            .obtain(line.ifEmpty { " " }, 0, line.ifEmpty { " " }.length, bodyPaint, contentWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(2f, 1.15f)
                            .build()
                        val lineH = layout.height
                        if (currentHeight + lineH > usableHeight && currentLines.isNotEmpty()) {
                            paged.add(currentLines)
                            currentLines = mutableListOf()
                            currentHeight = 0
                        }
                        currentLines.add(line)
                        currentHeight += lineH
                    }
                    if (currentLines.isNotEmpty()) {
                        paged.add(currentLines)
                    }
                    if (paged.isEmpty()) {
                        paged.add(listOf(title))
                    }
                    pagesText = paged

                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pagesText.size)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    val pdfDoc = PdfDocument()
                    val margin = 44f
                    val contentWidth = (pageWidthPts - (margin * 2).toInt()).coerceAtLeast(200)

                    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 9f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = android.graphics.Color.rgb(13, 79, 75)
                    }
                    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 8.5f
                        color = android.graphics.Color.DKGRAY
                    }
                    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10.5f
                        color = android.graphics.Color.BLACK
                    }

                    try {
                        pagesText.forEachIndexed { index, lines ->
                            if (cancellationSignal?.isCanceled == true) {
                                pdfDoc.close()
                                callback.onWriteCancelled()
                                return
                            }
                            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPts, pageHeightPts, index + 1).create()
                            val page = pdfDoc.startPage(pageInfo)
                            val canvas = page.canvas

                            canvas.drawText(
                                "${TeacherProfile.POWERED_BY_ATTRIBUTION}  |  AjarMerdeka",
                                margin,
                                28f,
                                headerPaint
                            )
                            canvas.drawLine(margin, 34f, pageWidthPts - margin, 34f, footerPaint)

                            var y = 50f
                            for (line in lines) {
                                val text = line.ifEmpty { " " }
                                val layout = StaticLayout.Builder
                                    .obtain(text, 0, text.length, bodyPaint, contentWidth)
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(2f, 1.15f)
                                    .build()
                                canvas.save()
                                canvas.translate(margin, y)
                                layout.draw(canvas)
                                canvas.restore()
                                y += layout.height
                            }

                            canvas.drawLine(margin, pageHeightPts - 34f, pageWidthPts - margin, pageHeightPts - 34f, footerPaint)
                            canvas.drawText(
                                "${TeacherProfile.POWERED_BY_ATTRIBUTION} — Hal. ${index + 1}/${pagesText.size}",
                                margin,
                                pageHeightPts - 20f,
                                footerPaint
                            )
                            pdfDoc.finishPage(page)
                        }

                        FileOutputStream(destination.fileDescriptor).use { out ->
                            pdfDoc.writeTo(out)
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage)
                    } finally {
                        pdfDoc.close()
                    }
                }
            },
            null
        )
    }
}
