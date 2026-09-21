package com.studyflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val language: String = "en",
    val themePref: String = "light",
    val xpPoints: Int = 0
)
