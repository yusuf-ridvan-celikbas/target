package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A book read in reading-preset Focus sessions. Per-user, like Course/Language. Progress isn't
 * stored here — the current page is the furthest end page any of its sessions reached (see ReadingStats.kt).
 */
@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId")],
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val title: String,
    val author: String? = null,
    /** Optional — without it there's no percentage, just "page X". */
    val totalPages: Int? = null,
    val isFinished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val genre: BookGenre? = null,
    val publisher: String? = null,
    val publishedYear: Int? = null,
    /** 1–5 stars, null when unrated. */
    val rating: Int? = null,
    val notes: String? = null,
    /** On the "Want to read" shelf. Status is derived: finished wins, then want-to-read, else reading (see bookStatus()). */
    val isWantToRead: Boolean = false,
    /** Index into the cover palette (ui/reading/BookCover.kt) — picked at random when the book is added. */
    val coverColor: Int = 0,
    /** Optional "finish by" day (start-of-day millis) — the daily page goal is derived from it (ui/reading/ReadingForecast.kt). */
    val goalDate: Long? = null,
)
