package com.salarytracker.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salarytracker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val year by viewModel.currentYear.collectAsState()
    val month by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthEntries.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val history by viewModel.paymentHistory.collectAsState()
    val dueTodayPeriod by viewModel.dueToday.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val summary = remember(entries, year, month) {
        SalaryCalculator.calculateMonth(entries, year, month)
    }

    Scaffold(
        containerColor = CreamBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Salary Tracker",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White)
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Setting", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepTeal)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            dueTodayPeriod?.let { due ->
                PaymentDueTodayCard(due)
                Spacer(Modifier.height(4.dp))
            }

            MonthNavigator(
                year = year,
                month = month,
                onPrev = { viewModel.goToPreviousMonth() },
                onNext = { viewModel.goToNextMonth() }
            )

            Spacer(Modifier.height(12.dp))

            CalendarGrid(
                year = year,
                month = month,
                entries = entries,
                onDayClick = { d ->
                    val isSun = CalendarUtils.isSunday(year, month, d)
                    val current = entries.find { it.day == d }?.status
                    val next = nextStatus(current, isSun)
                    viewModel.setDayStatus(year, month, d, isSun, next)
                }
            )

            Spacer(Modifier.height(8.dp))
            LegendRow()

            Spacer(Modifier.height(20.dp))

            Text(
                "Is mahine ka hisaab",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))

            PeriodCard(summary.period1, settings.dailyWage)
            Spacer(Modifier.height(12.dp))
            PeriodCard(summary.period2, settings.dailyWage)

            Spacer(Modifier.height(12.dp))

            TotalCard(summary.monthTotal)

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showSettingsSheet) {
        WageSettingsSheet(
            currentWage = settings.dailyWage,
            onDismiss = { showSettingsSheet = false },
            onSave = {
                viewModel.updateDailyWage(it)
                showSettingsSheet = false
            }
        )
    }

    if (showHistorySheet) {
        HistorySheet(
            history = history,
            onDismiss = { showHistorySheet = false }
        )
    }
}

/** Har tap par status cycle hota hai */
private fun nextStatus(current: String?, isSunday: Boolean): String {
    return if (isSunday) {
        // Sunday default off hai. Tap karne se PRESENT ho jayega (kaam pe gaye), dubara tap se wapas OFF
        if (current == DayStatus.PRESENT) DayStatus.OFF else DayStatus.PRESENT
    } else {
        when (current) {
            null, DayStatus.OFF -> DayStatus.PRESENT
            DayStatus.PRESENT -> DayStatus.ABSENT
            else -> DayStatus.OFF // ABSENT se wapas khaali/off jaisa (koi entry nahi)
        }
    }
}

@Composable
fun MonthNavigator(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Pichla mahina", tint = DeepTeal)
        }
        Text(
            "${CalendarUtils.hindiMonthNames[month - 1]} $year",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Agla mahina", tint = DeepTeal)
        }
    }
}

@Composable
fun CalendarGrid(
    year: Int,
    month: Int,
    entries: List<DayEntry>,
    onDayClick: (Int) -> Unit
) {
    val daysInMonth = CalendarUtils.daysInMonth(year, month)
    val firstWeekday = CalendarUtils.firstWeekdayOfMonth(year, month) // 0=Sun
    val entryMap = remember(entries) { entries.associateBy { it.day } }

    val weekdayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        val totalCells = firstWeekday + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstWeekday + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..daysInMonth) {
                            val isSun = CalendarUtils.isSunday(year, month, day)
                            val entry = entryMap[day]
                            val isToday = CalendarUtils.isToday(year, month, day)
                            DayCell(
                                day = day,
                                status = entry?.status,
                                isSunday = isSun,
                                isToday = isToday,
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    status: String?,
    isSunday: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when {
        status == DayStatus.PRESENT -> PresentGreenBg to PresentGreen
        status == DayStatus.ABSENT -> AbsentRedBg to AbsentRed
        isSunday -> OffGray to OffGrayText
        else -> Color.Transparent to TextDark
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isToday) Modifier.border(1.5.dp, TodayRing, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (isSunday && status != DayStatus.PRESENT) {
                Text("off", fontSize = 8.sp, color = OffGrayText)
            } else if (status == DayStatus.PRESENT) {
                Text("✓", fontSize = 9.sp, color = PresentGreen)
            } else if (status == DayStatus.ABSENT) {
                Text("✕", fontSize = 9.sp, color = AbsentRed)
            }
        }
    }
}

@Composable
fun LegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LegendItem(PresentGreenBg, PresentGreen, "Kaam kiya")
        LegendItem(AbsentRedBg, AbsentRed, "Chutti")
        LegendItem(OffGray, OffGrayText, "Sunday off")
    }
}

@Composable
fun LegendItem(bg: Color, dot: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dot, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
fun PeriodCard(period: PeriodSummary, dailyWage: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${period.label} tarikh",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    "₹${period.totalAmount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${period.presentDays} din kaam kiya × ₹$dailyWage",
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                period.paymentDateText,
                fontSize = 12.sp,
                color = GoldAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TotalCard(total: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepTeal),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mahine ka total", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text("₹$total", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PaymentDueTodayCard(period: PeriodSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = GoldAccent),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Aaj paisa aana chahiye",
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${period.presentDays} din ka (${period.label} tarikh)",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Text(
                    "₹${period.totalAmount}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(history: List<PaymentHistory>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CreamBg) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .padding(bottom = 30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payment History", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Band karo")
                }
            }
            Spacer(Modifier.height(6.dp))

            if (history.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Abhi tak koi payment record nahi bana. Jaise hi 20 ya 5 tarikh niklegi, yahan judta jayega.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(20.dp))
            } else {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    history.forEach { item ->
                        HistoryRow(item)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(item: PaymentHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${CalendarUtils.hindiMonthNames[item.month - 1]} ${item.year} · ${item.periodLabel}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${item.presentDays} din kaam kiya",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Text(
                "₹${item.amount}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepTeal
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WageSettingsSheet(currentWage: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentWage.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CreamBg) {
        Column(modifier = Modifier.padding(20.dp).padding(bottom = 30.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Roz ki mazdoori", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Band karo")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Yeh rate ab se jo bhi din tick karoge unpar lagega",
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.all { it.isDigit() }) text = input },
                label = { Text("₹ Daily Wage") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    focusedLabelColor = DeepTeal
                )
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { text.toIntOrNull()?.let { onSave(it) } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepTeal),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save karo", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
