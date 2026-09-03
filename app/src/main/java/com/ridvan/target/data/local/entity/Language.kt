package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "languages",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("userId")],
)
data class Language(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
