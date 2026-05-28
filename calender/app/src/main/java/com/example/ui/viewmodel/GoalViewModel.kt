package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Goal
import com.example.data.model.GoalDayStatus
import com.example.data.repository.GoalRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DayState(
    val date: Date,
    val dateString: String,
    val jalaliDate: String,
    val dayOfWeek: String,
    val relativeDayNumber: Int,
    val isCompleted: Boolean,
    val isPast: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val note: String?
)

data class GoalDetailUiState(
    val goal: Goal,
    val days: List<DayState> = emptyList(),
    val totalDays: Int = 0,
    val elapsedDays: Int = 0,
    val completedDays: Int = 0,
    val remainingDays: Int = 0,
    val progressPercent: Float = 0f
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GoalRepository(AppDatabase.getDatabase(application).goalDao())

    // List of all goals
    val allGoals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected goal ID
    private val _selectedGoalId = MutableStateFlow<Int?>(null)
    val selectedGoalId: StateFlow<Int?> = _selectedGoalId.asStateFlow()

    // Screen State
    // "Dashboard", "AddGoal", "GoalDetail"
    private val _currentScreenState = MutableStateFlow<String>("Dashboard")
    val currentScreenState: StateFlow<String> = _currentScreenState.asStateFlow()

    init {
        // Auto-select first goal when goals load or are updated, or reset if list is empty
        viewModelScope.launch {
            allGoals.collect { goals ->
                val current = _selectedGoalId.value
                if (goals.isNotEmpty()) {
                    if (current == null || goals.none { it.id == current }) {
                        _selectedGoalId.value = goals.first().id
                    }
                } else {
                    _selectedGoalId.value = null
                }
            }
        }
    }

    // Live detail of the selected goal
    val goalDetailUiState: StateFlow<GoalDetailUiState?> = _selectedGoalId
        .flatMapLatest { goalId ->
            if (goalId == null) {
                flowOf(null)
            } else {
                combine(
                    repository.getGoalById(goalId),
                    repository.getDayStatusesForGoal(goalId)
                ) { goal, statuses ->
                    if (goal == null) return@combine null

                    val dates = DateUtils.getDatesBetween(goal.startDate, goal.targetDate)
                    val statusMap = statuses.associateBy { it.dateString }

                    var completedCount = 0
                    var elapsedCount = 0
                    val todayString = DateUtils.getTodayString()

                    val daysList = dates.mapIndexed { index, date ->
                        val dateString = DateUtils.getDateString(date)
                        val status = statusMap[dateString]
                        val isCompleted = status?.isCompleted ?: false

                        val compare = DateUtils.compareToToday(dateString)
                        val isPast = compare < 0
                        val isToday = compare == 0
                        val isFuture = compare > 0

                        if (isCompleted) {
                            completedCount++
                        }
                        if (isPast || isToday) {
                            elapsedCount++
                        }

                        DayState(
                            date = date,
                            dateString = dateString,
                            jalaliDate = DateUtils.formatToJalaliMonthDay(date),
                            dayOfWeek = DateUtils.getDayOfWeekPersianName(date),
                            relativeDayNumber = index + 1,
                            isCompleted = isCompleted,
                            isPast = isPast,
                            isToday = isToday,
                            isFuture = isFuture,
                            note = status?.note
                        )
                    }

                    val total = daysList.size
                    val remaining = maxOf(0, total - elapsedCount)
                    val percent = if (total > 0) (completedCount.toFloat() / total.toFloat()) else 0f

                    GoalDetailUiState(
                        goal = goal,
                        days = daysList,
                        totalDays = total,
                        elapsedDays = elapsedCount,
                        completedDays = completedCount,
                        remainingDays = remaining,
                        progressPercent = percent
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun navigateTo(screen: String) {
        _currentScreenState.value = screen
    }

    fun selectGoal(goalId: Int?) {
        _selectedGoalId.value = goalId
    }

    fun createGoal(title: String, description: String, durationDays: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            // Midnight today
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis

            // Add duration of days minus 1 (e.g. 30-day goal includes today as Day 1)
            cal.add(Calendar.DAY_OF_YEAR, durationDays - 1)
            val targetMs = cal.timeInMillis

            val goal = Goal(
                title = title,
                description = description,
                startDate = startMs,
                targetDate = targetMs
            )
            val newId = repository.insertGoal(goal).toInt()
            _selectedGoalId.value = newId
        }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
            if (_selectedGoalId.value == goalId) {
                _selectedGoalId.value = null
            }
        }
    }

    fun toggleDayStatus(goalId: Int, dateString: String, isCompleted: Boolean, note: String? = null) {
        viewModelScope.launch {
            val status = GoalDayStatus(
                goalId = goalId,
                dateString = dateString,
                isCompleted = isCompleted,
                note = note
            )
            repository.saveDayStatus(status)
        }
    }
}
