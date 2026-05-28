package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Goal
import com.example.ui.viewmodel.DayState
import com.example.ui.viewmodel.GoalViewModel
import com.example.utils.DateUtils
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    viewModel: GoalViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.goalDetailUiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detailState?.goal?.title ?: "جزئیات هدف",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = detailState != null,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { 40 })
        ) {
            val state = detailState ?: return@AnimatedVisibility

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Header Card
                GoalHeaderCard(goal = state.goal, state = state)

                // Legends / Explanations of Days Status
                DayLegends()

                // Calendar Grid Header (شنبه تا جمعه)
                WeekdaysHeader()

                // Main Calendar Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    GoalCalendarGrid(
                        days = state.days,
                        goalStartDate = state.goal.startDate,
                        onDayClick = { dayState ->
                            viewModel.toggleDayStatus(
                                goalId = state.goal.id,
                                dateString = dayState.dateString,
                                isCompleted = !dayState.isCompleted
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalHeaderCard(goal: Goal, state: com.example.ui.viewmodel.GoalDetailUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (goal.description.isNotEmpty()) {
                Text(
                    text = goal.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            // Interactive stats display with modern circular badge or beautiful grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "کل دوره", value = "${DateUtils.toPersianDigits(state.totalDays)} روز", color = MaterialTheme.colorScheme.primary)
                StatItem(label = "سپری شده", value = "${DateUtils.toPersianDigits(state.elapsedDays)} روز", color = MaterialTheme.colorScheme.onSurface)
                StatItem(label = "باقی‌مانده", value = "${DateUtils.toPersianDigits(state.remainingDays)} روز", color = MaterialTheme.colorScheme.secondary)
                StatItem(label = "موفقیت", value = "${DateUtils.toPersianDigits(state.completedDays)} روز", color = Color(0xFF10B981))
            }

            // Custom elegant adherence scale
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val percentageText = (state.progressPercent * 100).toInt()
                    Text(
                        "میزان پایبندی و تعهد: ${DateUtils.toPersianDigits(percentageText)}٪",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = if (percentageText >= 80) "عالی" else if (percentageText >= 50) "خوب" else "نیاز به تلاش",
                        fontSize = 11.sp,
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
                
                // Animated progress line
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progressPercent,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                    label = "TargetPercentage"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF10B981).copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label, 
            fontSize = 11.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color
        )
    }
}

@Composable
fun DayLegends() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = Color(0xFF10B981), label = "انجام شد", isPastCompleted = true)
        LegendItem(color = MaterialTheme.colorScheme.error, label = "سوخته", isPastMissed = true)
        LegendItem(color = MaterialTheme.colorScheme.primary, label = "امروز", isToday = true)
        LegendItem(color = MaterialTheme.colorScheme.outlineVariant, label = "آینده", isFuture = true)
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    isToday: Boolean = false,
    isFuture: Boolean = false,
    isPastCompleted: Boolean = false,
    isPastMissed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isToday) MaterialTheme.colorScheme.surface else if (isFuture) color.copy(alpha = 0.15f) else color.copy(
                        alpha = 0.2f
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isToday) color else if (isFuture) color.copy(alpha = 0.3f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
                .drawBehind {
                    if (isPastCompleted) {
                        drawLine(
                            color = color,
                            start = Offset(0.1f * size.width, 0.9f * size.height),
                            end = Offset(0.9f * size.width, 0.1f * size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    if (isPastMissed) {
                        drawLine(
                            color = color,
                            start = Offset(0.1f * size.width, 0.9f * size.height),
                            end = Offset(0.9f * size.width, 0.1f * size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        )
        Text(
            text = label, 
            fontSize = 11.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun WeekdaysHeader() {
    val days = listOf("شنبه", "۱شنبه", "۲شنبه", "۳شنبه", "۴شنبه", "۵شنبه", "جمعه")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        days.forEach { dayName ->
            Text(
                text = dayName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalCalendarGrid(
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(gridItems) { index, item ->
            if (item == null) {
                Spacer(modifier = Modifier.size(46.dp))
            } else {
                // Introduce staggered fade/slide entrance animation based on index
                val animDelay = (index % 28) * 15L // stagger inside the viewport
                var isVisible by remember { mutableStateOf(false) }
                
                LaunchedEffect(key1 = item.dateString) {
                    delay(animDelay)
                    isVisible = true
                }

                val scale by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0.7f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                    label = "CellScale"
                )
                
                val alpha by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "CellAlpha"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    DayCell(dayState = item, onClick = { onDayClick(item) })
                }
            }
        }
    }
}

@Composable
fun DayCell(
    dayState: DayState,
    onClick: () -> Unit
) {
    val isPast = dayState.isPast
    val isToday = dayState.isToday
    val isFuture = dayState.isFuture
    val isCompleted = dayState.isCompleted

    // Tactical spring animations for pen stroke on the calendar day!
    val animatedCheckProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label = "MarkerLineProgress"
    )

    val animatedPastProgress by animateFloatAsState(
        targetValue = if (isPast && !isCompleted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f),
        label = "WarningLineProgress"
    )

    // Bounce state on press
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "ButtonPress"
    )

    val backgroundColor = when {
        isCompleted -> Color(0xFF10B981).copy(alpha = 0.16f)
        isPast -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val strokeColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isCompleted -> Color(0xFF10B981).copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(
                width = if (isToday) 2.dp else if (isCompleted) 1.dp else 0.dp,
                color = strokeColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                enabled = isToday || isPast
            ) {
                // Interactive micro feedback
                isPressed = true
                onClick()
                // Let the scale bounce back immediately
                isPressed = false
            }
            .drawBehind {
                // Visual Pen Cross-out Line! (هر روز که می گذرد روز ها خط می خورند)
                // Case A: Checked Complete (green marker)
                if (animatedCheckProgress > 0f) {
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(0.12f * size.width, 0.88f * size.height),
                        end = Offset(
                            0.12f * size.width + (0.76f * size.width) * animatedCheckProgress,
                            0.88f * size.height - (0.76f * size.height) * animatedCheckProgress
                        ),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                
                // Case B: Expired without success (red thin caution strike)
                if (animatedPastProgress > 0f) {
                    drawLine(
                        color = Color(0xFFEF4444).copy(alpha = 0.55f),
                        start = Offset(0.12f * size.width, 0.88f * size.height),
                        end = Offset(
                            0.12f * size.width + (0.76f * size.width) * animatedPastProgress,
                            0.88f * size.height - (0.76f * size.height) * animatedPastProgress
                        ),
                        strokeWidth = 2.dp.toPx()
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
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = 14.sp,
                color = when {
                    isCompleted -> Color(0xFF10B981)
                    isPast -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = DateUtils.toPersianDigits(dayState.relativeDayNumber) + " روز",
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}
