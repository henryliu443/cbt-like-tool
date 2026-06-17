package com.henryliu.cbtreframe.android.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryExportManager {

    @Serializable
    data class Envelope(
        val schemaVersion: Int,
        val appMarketingVersion: String,
        val appBuild: String,
        val exportedAt: String,
        val entryCount: Int,
        val entries: List<Row>
    )

    @Serializable
    data class Row(
        val id: String,
        val createdAt: String,
        val inputThought: String,
        val moodTag: String,
        val therapyTemplateRaw: String,
        val analysisDepthRaw: String,
        val responseStyleRaw: String,
        val distortion: String,
        val alternative: String,
        val action: String,
        val isFavorite: Boolean,
        val providerName: String,
        val modelName: String,
        val resultExtrasJSON: String
    )

    private val jsonFormat = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun isoDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }

    private fun rowFrom(entry: HistoryEntity): Row {
        return Row(
            id = entry.id,
            createdAt = isoDate(entry.timestamp),
            inputThought = entry.inputText,
            moodTag = entry.moodTag,
            therapyTemplateRaw = entry.therapyTemplateRaw,
            analysisDepthRaw = entry.analysisDepthRaw,
            responseStyleRaw = entry.responseStyleRaw,
            distortion = entry.distortion,
            alternative = entry.alternative,
            action = entry.action,
            isFavorite = entry.isFavorite == 1L,
            providerName = entry.providerName,
            modelName = entry.modelName,
            resultExtrasJSON = entry.resultExtrasJSON
        )
    }

    fun makeTemporaryJSONFile(context: Context, entries: List<HistoryEntity>): File? {
        val envelope = Envelope(
            schemaVersion = 1,
            appMarketingVersion = "1.0.0", // Hardcoded for now
            appBuild = "1",
            exportedAt = isoDate(System.currentTimeMillis()),
            entryCount = entries.size,
            entries = entries.map { rowFrom(it) }
        )

        return try {
            val jsonString = jsonFormat.encodeToString(envelope)
            val file = File(context.cacheDir, "history_export.json")
            file.writeText(jsonString)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun makeTemporaryCSVFile(context: Context, entries: List<HistoryEntity>): File? {
        val header = "createdAt,inputThought,moodTag,template,distortion,alternative,action,provider,model,isFavorite\n"
        val rows = entries.joinToString(separator = "\n") { e ->
            listOf(
                isoDate(e.timestamp),
                csvEscape(e.inputText),
                csvEscape(e.moodTag),
                csvEscape(e.therapyTemplateRaw),
                csvEscape(e.distortion),
                csvEscape(e.alternative),
                csvEscape(e.action),
                csvEscape(e.providerName),
                csvEscape(e.modelName),
                if (e.isFavorite == 1L) "1" else "0"
            ).joinToString(separator = ",")
        }
        val content = header + rows

        return try {
            val file = File(context.cacheDir, "history_export.csv")
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun makeTemporaryPDFFile(context: Context, entries: List<HistoryEntity>): File? {
        return try {
            val document = PdfDocument()
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.DEFAULT
            }

            var y = 40f
            val margin = 40f
            val maxWidth = 595f - margin * 2
            val lineHeight = paint.descent() - paint.ascent() + 4f

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            for ((i, entry) in entries.withIndex()) {
                val dateStr = sdf.format(Date(entry.timestamp))
                val rawText = "${i + 1}. [$dateStr] ${entry.inputText}\n扭曲: ${entry.distortion}\n替代: ${entry.alternative}\n行动: ${entry.action}\n\n"
                
                // Simple word wrap
                val lines = rawText.split("\n").flatMap { paragraph ->
                    val wrappedLines = mutableListOf<String>()
                    var currentLine = ""
                    for (char in paragraph) {
                        val testLine = currentLine + char
                        if (paint.measureText(testLine) > maxWidth) {
                            wrappedLines.add(currentLine)
                            currentLine = char.toString()
                        } else {
                            currentLine = testLine
                        }
                    }
                    if (currentLine.isNotEmpty()) wrappedLines.add(currentLine)
                    if (wrappedLines.isEmpty()) wrappedLines.add("") // preserve empty lines
                    wrappedLines
                }

                for (line in lines) {
                    if (y + lineHeight > 800f) {
                        document.finishPage(page)
                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        y = 40f
                    }
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
                y += lineHeight // Extra space between entries
            }

            document.finishPage(page)

            val file = File(context.cacheDir, "history_export.pdf")
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
