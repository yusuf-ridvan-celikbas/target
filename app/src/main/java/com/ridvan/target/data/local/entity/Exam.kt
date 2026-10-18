package com.ridvan.target.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = ExamType::class,
            parentColumns = ["id"],
            childColumns = ["examTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["id"],
            childColumns = ["languageId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("examTypeId"), Index("userId"), Index("languageId")],
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val examTypeId: Long,
    val userId: Long? = null,
    val hasSections: Boolean,
    val examDate: Long? = null,
    val studyStartDate: Long? = null,
    val languageId: Long? = null,
    val level: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Mock exams of this exam get an ALES score card (courses split by Quantitative/Verbal category). */
    @ColumnInfo(defaultValue = "0") val alesScoring: Boolean = false,
    /** Optional per-term conversion typed in by the user: score ≈ base + coef × weighted net. */
    val alesSayBase: Double? = null,
    val alesSayCoef: Double? = null,
    val alesSozBase: Double? = null,
    val alesSozCoef: Double? = null,
    val alesEaBase: Double? = null,
    val alesEaCoef: Double? = null,
)
