package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_day_statuses",
    indices = [Index(value = ["goalId", "dateString"], unique = true)]
)
data class GoalDayStatus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val dateString: String, // format "YYYY-MM-DD" style key
    val isCompleted: Boolean,
    val note: String? = null
)
