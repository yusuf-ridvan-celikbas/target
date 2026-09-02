package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetDate: Long? = null,
    val studyStartDate: Long? = null,
    val examLocation: String? = null,
    val registrationStartDate: Long? = null,
    val registrationEndDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
