package com.salarytracker.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).dao()

    private val today = Calendar.getInstance()

    private val _currentYear = MutableStateFlow(today.get(Calendar.YEAR))
    private val _currentMonth = MutableStateFlow(today.get(Calendar.MONTH) + 1)

    val currentYear: StateFlow<Int> = _currentYear
    val currentMonth: StateFlow<Int> = _currentMonth

    // Settings - agar DB me kuch nahi hai to default 580 dikhega
    val settings: StateFlow<Settings> = dao.getSettings()
        .map { it ?: Settings(id = 0, dailyWage = 580) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings(id = 0, dailyWage = 580))

    // Current month + year badalne par entries bhi refresh honi chahiye
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val monthEntries: StateFlow<List<DayEntry>> = _currentYear
        .flatMapLatest { year ->
            _currentMonth.flatMapLatest { month ->
                dao.getEntriesForMonth(year, month)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Poori payment history (sab mahino ka)
    val paymentHistory: StateFlow<List<PaymentHistory>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aaj jo bhi payment due hai (chahe current screen kisi aur mahine ka ho), woh yahan milega
    private val _dueToday = MutableStateFlow<PeriodSummary?>(null)
    val dueToday: StateFlow<PeriodSummary?> = _dueToday

    init {
        viewModelScope.launch {
            dao.saveSettingsIfMissing()
        }
        // Jaise hi month entries badle, check karo ki koi payment due/paid hui hai kya - agar hai to history me save karo
        viewModelScope.launch {
            monthEntries.collect { entries ->
                checkAndSaveHistory(entries)
            }
        }
        // App khulte hi pichle 3 mahino ka bhi check kar lo, taaki bina khole bhi history ban jaye
        viewModelScope.launch {
            checkPastMonthsForHistory()
        }
        // Aaj payment due hai kya - iske liye is mahine aur pichhle mahine dono ka data dekhna padega
        viewModelScope.launch {
            computeDueToday()
        }
    }

    private suspend fun computeDueToday() {
        val y = today.get(Calendar.YEAR)
        val m = today.get(Calendar.MONTH) + 1
        var prevY = y
        var prevM = m - 1
        if (prevM < 1) { prevM = 12; prevY -= 1 }

        val currentMonthEntries = dao.getEntriesForMonth(y, m).first()
        val prevMonthEntries = dao.getEntriesForMonth(prevY, prevM).first()

        val currentSummary = SalaryCalculator.calculateMonth(currentMonthEntries, y, m)
        val prevSummary = SalaryCalculator.calculateMonth(prevMonthEntries, prevY, prevM)

        // Current month ka period1 (20 tarikh ko due) ya pichhle month ka period2 (5 tarikh ko due)
        _dueToday.value = listOf(currentSummary.period1, prevSummary.period2)
            .firstOrNull { it.isPaymentDueToday }
    }

    private suspend fun checkPastMonthsForHistory() {
        var y = today.get(Calendar.YEAR)
        var m = today.get(Calendar.MONTH) + 1
        repeat(3) {
            val entries = dao.getEntriesForMonth(y, m)
            // ek baar ka snapshot le lete hain flow se
            val list = entries.first()
            if (list.isNotEmpty()) checkAndSaveHistory(list)
            m -= 1
            if (m < 1) { m = 12; y -= 1 }
        }
    }

    /**
     * Jis period ka payment date aaj hai ya nikal chuka hai (aaj ya us se aage),
     * uska final total history table me (agar pehle se nahi hai to) save kar do.
     * Isse purane mahino ka record permanently rehta hai.
     */
    private suspend fun checkAndSaveHistory(entries: List<DayEntry>) {
        if (entries.isEmpty()) return
        val year = entries.first().year
        val month = entries.first().month
        val summary = SalaryCalculator.calculateMonth(entries, year, month)

        val now = Calendar.getInstance()

        for (period in listOf(summary.period1, summary.period2)) {
            val paymentDate = Calendar.getInstance()
            paymentDate.set(period.paymentYear, period.paymentMonth - 1, period.paymentDay, 23, 59, 59)

            val paymentDatePassed = !paymentDate.after(now)

            if (paymentDatePassed) {
                val existing = dao.getHistoryEntry(period.periodKey)
                // Agar history me nahi hai, ya amount badal gaya hai (koi din baad me tick kiya), to update/save karo
                if (existing == null || existing.amount != period.totalAmount || existing.presentDays != period.presentDays) {
                    dao.saveHistoryEntry(
                        PaymentHistory(
                            periodKey = period.periodKey,
                            year = year,
                            month = month,
                            periodLabel = period.label,
                            presentDays = period.presentDays,
                            amount = period.totalAmount,
                            paymentDateText = period.paymentDateText,
                            markedPaidAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun goToPreviousMonth() {
        var y = _currentYear.value
        var m = _currentMonth.value - 1
        if (m < 1) { m = 12; y -= 1 }
        _currentYear.value = y
        _currentMonth.value = m
    }

    fun goToNextMonth() {
        var y = _currentYear.value
        var m = _currentMonth.value + 1
        if (m > 12) { m = 1; y += 1 }
        _currentYear.value = y
        _currentMonth.value = m
    }

    fun updateDailyWage(newWage: Int) {
        viewModelScope.launch {
            dao.saveSettings(Settings(id = 0, dailyWage = newWage))
        }
    }

    /**
     * Din pe tap karne par status cycle hota hai:
     * Sunday ke liye: OFF -> PRESENT -> OFF  (default off, chaho to kaam dikha sakte ho)
     * Normal din ke liye: khaali -> PRESENT -> ABSENT -> khaali (delete)
     */
    fun setDayStatus(year: Int, month: Int, day: Int, isSunday: Boolean, newStatus: String) {
        viewModelScope.launch {
            val wage = settings.value.dailyWage
            val dateKey = "%04d-%02d-%02d".format(year, month, day)
            dao.upsertDay(
                DayEntry(
                    dateKey = dateKey,
                    year = year,
                    month = month,
                    day = day,
                    isSunday = isSunday,
                    status = newStatus,
                    wageOnThatDay = wage
                )
            )
        }
    }
}
