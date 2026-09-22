package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
