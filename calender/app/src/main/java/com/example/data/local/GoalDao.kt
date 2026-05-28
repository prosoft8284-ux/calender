package com.example.data.local

import androidx.room.*
import com.example.data.model.Goal
import com.example.data.model.GoalDayStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun getGoalById(id: Int): Flow<Goal?>

    @Query("SELECT * FROM goal_day_statuses WHERE goalId = :goalId")
    fun getDayStatusesForGoal(goalId: Int): Flow<List<GoalDayStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayStatus(status: GoalDayStatus)

    @Query("DELETE FROM goal_day_statuses WHERE goalId = :goalId")
    suspend fun deleteStatusesForGoal(goalId: Int)
}
