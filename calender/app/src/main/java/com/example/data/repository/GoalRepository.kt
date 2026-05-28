package com.example.data.repository

import com.example.data.local.GoalDao
import com.example.data.model.Goal
import com.example.data.model.GoalDayStatus
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {

    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    fun getGoalById(id: Int): Flow<Goal?> = goalDao.getGoalById(id)

    fun getDayStatusesForGoal(goalId: Int): Flow<List<GoalDayStatus>> =
        goalDao.getDayStatusesForGoal(goalId)

    suspend fun insertGoal(goal: Goal): Long = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(goalId: Int) {
        goalDao.deleteStatusesForGoal(goalId)
        goalDao.deleteGoalById(goalId)
    }

    suspend fun saveDayStatus(status: GoalDayStatus) = goalDao.insertDayStatus(status)
}
