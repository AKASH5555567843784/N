package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // e.g., "USER_PREFERENCE", "COMMAND", "FACT", etc.
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
