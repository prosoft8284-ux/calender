package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Goal
import com.example.ui.viewmodel.DayState
import com.example.ui.viewmodel.GoalViewModel
import com.example.utils.DateUtils
import com.example.utils.DailyInspirationProvider
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GoalViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allGoals.collectAsState()
    val selectedId by viewModel.selectedGoalId.collectAsState()
    val detailState by viewModel.goalDetailUiState.collectAsState()

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Int?>(null) }
    var selectedDayForDetail by remember { mutableStateOf<DayState?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تقویم هدف شما",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (goals.isEmpty()) {
                // Inline starting form when there are zero goals
                EmptyStateFormView(
                    onCreateGoal = { title, desc, duration ->
                        viewModel.createGoal(title, desc, duration)
                    }
                )
            } else {
                // Main Workspace Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Goal carousel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 0.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
                        ) {
                            items(goals, key = { it.id }) { goal ->
                                val isSelected = goal.id == selectedId
                                GoalSelectChip(
                                    goal = goal,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectGoal(goal.id) }
                                )
                            }
                        }

                        // Floating-styled "+" Button next to carousel
                        FilledIconButton(
                            onClick = { showAddGoalSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .testTag("quick_add_goal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "ثبت هدف جدید",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // View details and interactive grid
                    AnimatedContent(
                        targetState = detailState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(250))
                        },
                        label = "GoalCalendarContent",
                        modifier = Modifier.weight(1f)
                    ) { state ->
                        if (state != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Header Details with Percentage & Stats Progress
                                GoalDashboardHeaderCard(
                                    goal = state.goal,
                                    state = state,
                                    onDeleteClick = { showDeleteConfirmation = state.goal.id }
                                )

                                // 2. Compact descriptive legends
                                DayLegendsCompact()

                                // 3. Grid representation headers (شنبه تا جمعه)
                                WeekdaysGridHeader()

                                // 4. Interactive grid containing days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    GoalDashboardCalendarGrid(
                                        days = state.days,
                                        goalStartDate = state.goal.startDate,
                                        onDayClick = { dayState ->
                                            selectedDayForDetail = dayState
                                        }
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Inline Slide up Bottom Sheet to Add Goal
            if (showAddGoalSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddGoalSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.testTag("add_goal_bottom_sheet")
                ) {
                    AddGoalInlineForm(
                        onSubmit = { title, desc, duration ->
                            viewModel.createGoal(title, desc, duration)
                            showAddGoalSheet = false
                        },
                        onCancel = { showAddGoalSheet = false }
                    )
                }
            }

            // Daily Quran & Book Inspiration Sheet
            selectedDayForDetail?.let { dayState ->
                detailState?.goal?.id?.let { goalId ->
                    DayInspirationDetailSheet(
                        dayState = dayState,
                        goalId = goalId,
                        viewModel = viewModel,
                        onDismiss = { selectedDayForDetail = null }
                    )
                }
            }

            // Styled deletion confirmation modal window
            if (showDeleteConfirmation != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = null },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmation?.let { id ->
                                    viewModel.deleteGoal(id)
                                }
                                showDeleteConfirmation = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بله، حذف شود", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = null }) {
                            Text("انصراف", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    title = {
                        Text(
                            text = "حذف هدف و تقویم آن؟",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = "آیا مطمئن هستید که می‌خواهید این هدف و تمامی روزهای خط‌خوردهٔ آن را حذف کنید؟ این عمل غیرقابل بازگشت است.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

@Composable
fun GoalSelectChip(
    goal: Goal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "ChipBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "ChipTextColor"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = spring(),
        label = "ChipElevation"
    )

    Surface(
        onClick = onClick,
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = elevation,
        modifier = Modifier
            .height(40.dp)
            .testTag("goal_chip_${goal.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = goal.title,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GoalDashboardHeaderCard(
    goal: Goal,
    state: com.example.ui.viewmodel.GoalDetailUiState,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (goal.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                        .testTag("delete_active_goal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف این هدف",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Stat Counter Row utilizing Jalali Persian Digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardStatItem(label = "کل دوره", value = "${DateUtils.toPersianDigits(state.totalDays)} روز", color = MaterialTheme.colorScheme.primary)
                DashboardStatItem(label = "سپری شده", value = "${DateUtils.toPersianDigits(state.elapsedDays)} روز", color = MaterialTheme.colorScheme.onSurface)
                DashboardStatItem(label = "باقی‌مانده", value = "${DateUtils.toPersianDigits(state.remainingDays)} روز", color = MaterialTheme.colorScheme.secondary)
                DashboardStatItem(label = "موفقیت", value = "${DateUtils.toPersianDigits(state.completedDays)} روز", color = Color(0xFF10B981))
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val percentageText = (state.progressPercent * 100).toInt()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "میزان پایبندی و تعهد: ${DateUtils.toPersianDigits(percentageText)}٪",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = if (percentageText >= 80) "عالی" else if (percentageText >= 50) "خوب" else "نیاز به تلاش",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentageText >= 80) Color(0xFF10B981) else if (percentageText >= 50) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(
                                color = (if (percentageText >= 80) Color(0xFF10B981) else if (percentageText >= 50) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val animatedProgress by animateFloatAsState(
                    targetValue = state.progressPercent,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                    label = "ProgressBarAnimation"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF10B981).copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
fun DashboardStatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = color
        )
    }
}

@Composable
fun DayLegendsCompact() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItemView(color = Color(0xFF10B981), label = "انجام شد", isPastCompleted = true)
        LegendItemView(color = MaterialTheme.colorScheme.error, label = "سوخته", isPastMissed = true)
        LegendItemView(color = MaterialTheme.colorScheme.primary, label = "امروز", isToday = true)
        LegendItemView(color = MaterialTheme.colorScheme.outlineVariant, label = "آینده", isFuture = true)
    }
}

@Composable
fun LegendItemView(
    color: Color,
    label: String,
    isToday: Boolean = false,
    isFuture: Boolean = false,
    isPastCompleted: Boolean = false,
    isPastMissed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isToday) MaterialTheme.colorScheme.surface else if (isFuture) color.copy(alpha = 0.15f) else color.copy(
                        alpha = 0.2f
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isToday) color else if (isFuture) color.copy(alpha = 0.3f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .drawBehind {
                    if (isPastCompleted || isPastMissed) {
                        drawLine(
                            color = color,
                            start = Offset(0.1f * size.width, 0.9f * size.height),
                            end = Offset(0.9f * size.width, 0.1f * size.height),
                            strokeWidth = 2f.dp.toPx()
                        )
                    }
                }
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WeekdaysGridHeader() {
    val days = listOf("شنبه", "۱شنبه", "۲شنبه", "۳شنبه", "۴شنبه", "۵شنبه", "جمعه")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        days.forEach { dayName ->
            Text(
                text = dayName,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.width(42.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalDashboardCalendarGrid(
    days: List<DayState>,
    goalStartDate: Long,
    onDayClick: (DayState) -> Unit
) {
    if (days.isEmpty()) return

    val startCal = Calendar.getInstance()
    startCal.timeInMillis = goalStartDate

    val dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
    val startDayPadding = when (dayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
    }

    val gridItems = remember(days, startDayPadding) {
        val list = mutableListOf<DayState?>()
        repeat(startDayPadding) { list.add(null) }
        list.addAll(days)
        list
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(gridItems) { index, item ->
            if (item == null) {
                Spacer(modifier = Modifier.size(44.dp))
            } else {
                val animDelay = (index % 21) * 12L
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(key1 = item.dateString) {
                    delay(animDelay)
                    isVisible = true
                }

                val scale by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0.75f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                    label = "CellScaleAnim"
                )

                val alpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(150),
                    label = "CellAlphaAnim"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    DashboardDayCell(dayState = item, onClick = { onDayClick(item) })
                }
            }
        }
    }
}

@Composable
fun DashboardDayCell(
    dayState: DayState,
    onClick: () -> Unit
) {
    val isPast = dayState.isPast
    val isToday = dayState.isToday
    val isCompleted = dayState.isCompleted

    val animatedCheckProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 130f),
        label = "CompletePenStroke"
    )

    val animatedPastProgress by animateFloatAsState(
        targetValue = if (isPast && !isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 110f),
        label = "BurntPenStroke"
    )

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "CellPress"
    )

    val backgroundColor = when {
        isCompleted -> Color(0xFF10B981).copy(alpha = 0.15f)
        isPast -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    val strokeColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isCompleted -> Color(0xFF10B981).copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isToday) 2.dp else if (isCompleted) 1.dp else 0.dp,
                color = strokeColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isToday || isPast) {
                isPressed = true
                onClick()
                isPressed = false
            }
            .drawBehind {
                if (animatedCheckProgress > 0f) {
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(0.12f * size.width, 0.88f * size.height),
                        end = Offset(
                            0.12f * size.width + (0.76f * size.width) * animatedCheckProgress,
                            0.88f * size.height - (0.76f * size.height) * animatedCheckProgress
                        ),
                        strokeWidth = 2.5f.dp.toPx()
                    )
                }

                if (animatedPastProgress > 0f) {
                    drawLine(
                        color = Color(0xFFEF4444).copy(alpha = 0.5f),
                        start = Offset(0.12f * size.width, 0.88f * size.height),
                        end = Offset(
                            0.12f * size.width + (0.76f * size.width) * animatedPastProgress,
                            0.88f * size.height - (0.76f * size.height) * animatedPastProgress
                        ),
                        strokeWidth = 1.8f.dp.toPx()
                    )
                }
            }
            .testTag("day_cell_${dayState.dateString}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val dayLabel = Calendar.getInstance().apply { time = dayState.date }.get(Calendar.DAY_OF_MONTH)
            Text(
                text = DateUtils.toPersianDigits(dayLabel),
                fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                fontSize = 13.sp,
                color = when {
                    isCompleted -> Color(0xFF10B981)
                    isPast -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = "${DateUtils.toPersianDigits(dayState.relativeDayNumber)}روز",
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AddGoalInlineForm(
    onSubmit: (String, String, Int) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customDays by remember { mutableStateOf("") }
    var selectedPresetDays by remember { mutableStateOf(30) }
    var showError by remember { mutableStateOf(false) }

    val presets = listOf(
        7 to "۷ روز",
        21 to "۲۱ روز",
        30 to "۳۰ روز",
        40 to "۴۰ روز",
        100 to "۱۰۰ روز"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "تعریف چالش و تقویم جدید",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Title
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "عنوان هدف / چالش",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showError = false
                },
                placeholder = { Text("مثلاً: پیاده‌روی روزانه یا ترک نوشابه", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_title_input"),
                singleLine = true,
                isError = showError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
            if (showError) {
                Text(
                    text = "لطفاً عنوان چالش خود را بنویسید.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Description
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "جزییات هدف (اختیاری)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("مثال: خوردن سالاد در شب و یا هر شب خواب ساعت ۱۱", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .testTag("goal_desc_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
        }

        // Preset Duration Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "طول دوره بر اساس روز",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (days, label) ->
                    val isSelected = selectedPresetDays == days && customDays.isEmpty()
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                selectedPresetDays = days
                                customDays = ""
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Custom Days
        OutlinedTextField(
            value = customDays,
            onValueChange = {
                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                    customDays = it
                }
            },
            placeholder = { Text("تعداد روز دلخواه شما") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goal_custom_days_input"),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = { Text("روز", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Actions buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("انصراف", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = {
                    val finalTitle = title.trim()
                    if (finalTitle.isEmpty()) {
                        showError = true
                        return@Button
                    }
                    val days = customDays.toIntOrNull() ?: selectedPresetDays
                    if (days > 0) {
                        onSubmit(finalTitle, description.trim(), days)
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("شروع چالش تقویم", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EmptyStateFormView(
    onCreateGoal: (String, String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "هنوز تقویم هدفی نساخته‌اید",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "برای شروع کارهای روزانه، یک چالش یا هدف تعیین کنید. روزها به زیبائی و با انیمیشن در تقویم خط خواهند خورد.",
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ثبت اولین تقویم هدف",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                var inlineTitle by remember { mutableStateOf("") }
                var inlineDesc by remember { mutableStateOf("") }
                var inlineCustomDays by remember { mutableStateOf("") }
                var inlineSelectedPreset by remember { mutableStateOf(30) }
                var showInlineError by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = inlineTitle,
                    onValueChange = {
                        inlineTitle = it
                        showInlineError = false
                    },
                    placeholder = { Text("مثلا: یادگیری کامپیوتر یا ورزش بیست دقیقه‌ای", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input"),
                    singleLine = true,
                    isError = showInlineError,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = inlineDesc,
                    onValueChange = { inlineDesc = it },
                    placeholder = { Text("توضیحات کلی تر در مورد هدف", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("goal_desc_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(7, 21, 30, 40).forEach { days ->
                        val isSel = inlineSelectedPreset == days && inlineCustomDays.isEmpty()
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    inlineSelectedPreset = days
                                    inlineCustomDays = ""
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$days روز",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inlineCustomDays,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            inlineCustomDays = it
                        }
                    },
                    placeholder = { Text("یا تعداد روز دلخواه شما") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_custom_days_input")
                )

                Button(
                    onClick = {
                        val finalTitle = inlineTitle.trim()
                        if (finalTitle.isEmpty()) {
                            showInlineError = true
                            return@Button
                        }
                        val finalDays = inlineCustomDays.toIntOrNull() ?: inlineSelectedPreset
                        if (finalDays > 0) {
                            onCreateGoal(finalTitle, inlineDesc.trim(), finalDays)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ایجاد تقویم هدف و شروع", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayInspirationDetailSheet(
    dayState: DayState,
    goalId: Int,
    viewModel: GoalViewModel,
    onDismiss: () -> Unit
) {
    val inspiration = remember(dayState.relativeDayNumber) {
        DailyInspirationProvider.getInspirationForDay(dayState.relativeDayNumber)
    }
    
    var noteText by remember(dayState) { mutableStateOf(dayState.note ?: "") }
    var isCompleted by remember(dayState) { mutableStateOf(dayState.isCompleted) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .testTag("day_inspiration_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "روز ${DateUtils.toPersianDigits(dayState.relativeDayNumber)} چالش • ${dayState.jalaliDate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dayState.dayOfWeek,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "وضعیت تعهد شما در این روز:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Success Toggle Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { isCompleted = true },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) {
                                    Color(0xFFE8F5E9) // Soft light green
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isCompleted) 2.dp else 1.dp,
                                color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "موفقیت‌آمیز",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Failed / Not Done Toggle Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clickable { isCompleted = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isCompleted) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) // Soft red
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (!isCompleted) 2.dp else 1.dp,
                                color = if (!isCompleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "انجام‌نشده / سوخته",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (!isCompleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Note/Reflection Section
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("📝 یادداشت روزانه و بازخورد امروز شما (اختیاری)", fontSize = 12.sp) },
                placeholder = { Text("مثلاً: امروز عالی بودم، چالش‌های صبگاهی با موفقیت انجام شد...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = false
            )

            // Quran Verse Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                ),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آیه مبارک قرآن کریم • " + inspiration.verseReference,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✨", fontSize = 16.sp)
                    }

                    // Arabic Text
                    Text(
                        text = inspiration.verseArabic,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    // Persian Translation
                    Text(
                        text = inspiration.verseTranslation,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Tafsir Al-Mizan
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                ),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تدبر در المیزان (علامه طباطبایی)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📚", fontSize = 16.sp)
                    }

                    Text(
                        text = inspiration.tafsirAlMizan,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Book Quote of the day
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.08f)
                ),
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جمله‌ای آموزنده از کتاب‌ها",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌱", fontSize = 16.sp)
                    }

                    Text(
                        text = "« " + inspiration.bookQuote + " »",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "منبع: " + inspiration.bookSource,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Confirm & Save button
            Button(
                onClick = {
                    viewModel.toggleDayStatus(
                        goalId = goalId,
                        dateString = dayState.dateString,
                        isCompleted = isCompleted,
                        note = noteText.trim().ifEmpty { null }
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "ثبت وضعیت و ذخیره تغییرات امروز",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
