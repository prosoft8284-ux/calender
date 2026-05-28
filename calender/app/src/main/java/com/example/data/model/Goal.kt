package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val startDate: Long,
    val targetDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
