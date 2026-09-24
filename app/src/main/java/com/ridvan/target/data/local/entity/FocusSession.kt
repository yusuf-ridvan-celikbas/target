package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FocusPreset::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("userId"), Index("presetId"), Index("courseId"), Index("topicId"), Index("languageId")],
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val presetId: Long? = null,
    /** Snapshots of the preset's name/interval at the time this session ran, so history keeps
     * reading correctly even if the preset is later renamed or deleted (SET_NULL). */
    val presetName: String,
    val workMinutes: Int,
    val breakMinutes: Int,
    /** Optional link to what was being studied — exactly one of courseId/languageId set, or neither.
     * topicId (when set) belongs to whichever of the two is set. */
    val courseId: Long? = null,
    val topicId: Long? = null,
    val languageId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val cyclesCompleted: Int,
    val totalWorkMinutes: Int,
    val totalBreakMinutes: Int,
    /** Free-text annotation added afterwards on the session's detail screen, e.g. "Studied Dativ". */
    val notes: String? = null,
)
