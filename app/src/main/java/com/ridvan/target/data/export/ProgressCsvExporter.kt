package com.ridvan.target.data.export

import android.content.Context
import java.io.File

private const val UTF8_BOM = "﻿"

object ProgressCsvExporter {
    fun writeProgressCsv(
        context: Context,
        scopeName: String,
        headers: List<String>,
        rows: List<List<String>>,
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = scopeName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val file = File(exportsDir, "$safeName - Progress.csv")
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(UTF8_BOM)
            writer.write(headers.joinToString(",") { csvField(it) })
            writer.newLine()
            rows.forEach { row ->
                writer.write(row.joinToString(",") { csvField(it) })
                writer.newLine()
            }
        }
        return file
    }

    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
