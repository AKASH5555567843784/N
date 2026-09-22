package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "95"
    val text: String,
    val emotion: String, // String representation of AssistantEmotion
    val timestamp: Long = System.currentTimeMillis()
)
