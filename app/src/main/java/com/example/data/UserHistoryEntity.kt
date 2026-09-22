package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_history")
data class UserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commandText: String,
    val executedSuccessfully: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
