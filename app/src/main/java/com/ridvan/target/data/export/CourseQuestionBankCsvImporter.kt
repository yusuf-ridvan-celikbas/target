package com.ridvan.target.data.export

data class QuestionBankCsvRow(
    val resourceName: String,
    val publisher: String?,
    val topicName: String,
    val testCount: Int,
    val questionCount: Int,
)

object CourseQuestionBankCsvImporter {
    /**
     * Parses the app's own export format (resource name, publisher, topic, test count, question
     * count) — quote-aware so it round-trips values [CourseQuestionBankCsvExporter] escaped (a
     * comma/quote/newline inside a field). The first row is always treated as a header and skipped.
     */
    fun parse(csvContent: String): List<QuestionBankCsvRow> {
        val allRows = parseCsvRows(csvContent)
        if (allRows.size <= 1) return emptyList()
        return allRows.drop(1)
            .filter { it.size >= 5 }
            .mapNotNull { fields ->
                val resourceName = fields[0].trim()
                val topicName = fields[2].trim()
                if (resourceName.isEmpty() || topicName.isEmpty()) return@mapNotNull null
                QuestionBankCsvRow(
                    resourceName = resourceName,
                    publisher = fields[1].trim().ifBlank { null },
                    topicName = topicName,
                    testCount = fields[3].trim().toIntOrNull() ?: 0,
                    questionCount = fields[4].trim().toIntOrNull() ?: 0,
                )
            }
    }

    private fun parseCsvRows(content: String): List<List<String>> {
        val text = content.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    currentRow.add(field.toString())
                    field.clear()
                }
                c == '\r' -> {}
                c == '\n' -> {
                    currentRow.add(field.toString())
                    field.clear()
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            rows.add(currentRow)
        }
        return rows
    }
}
