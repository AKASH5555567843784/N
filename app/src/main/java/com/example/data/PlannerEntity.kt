package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_items")
data class PlannerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: String, // "TASK", "NOTE", "REMINDER"
    val dueTime: Long = 0L,
    val priority: Int = 1, // 1 = Low, 2 = Medium, 3 = High
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
