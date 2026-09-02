package com.ridvan.target.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)],
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val middleName: String? = null,
    val preferredName: String,
    val preferredNameSource: PreferredNameSource = PreferredNameSource.OTHER,
    val lastName: String,
    val username: String,
    val email: String? = null,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long = System.currentTimeMillis(),
)
