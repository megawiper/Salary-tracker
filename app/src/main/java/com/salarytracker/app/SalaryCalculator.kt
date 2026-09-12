package com.salarytracker.app

import java.util.Calendar

data class PeriodSummary(
    val periodKey: String,   // "yyyy-MM-P1" ya "yyyy-MM-P2" - history save karne ke liye unique id
    val label: String,       // "1 - 15" ya "16 - 30"
    val presentDays: Int,
    val totalAmount: Int,
    val paymentDateText: String, // "Paisa milega: 20 tarikh ko"
    val isPaymentDueToday: Boolean, // aaj hi payment ka din hai kya
    val paymentYear: Int,    // jis saal/mahine paisa milta hai (period 2 ke liye agla mahina)
    val paymentMonth: Int,
    val paymentDay: Int
)

data class MonthSummary(
    val period1: PeriodSummary,
    val period2: PeriodSummary,
    val monthTotal: Int
)

object SalaryCalculator {

    fun lastDayOfMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 12) (year + 1) to 1 else year to (month + 1)
    }

    fun calculateMonth(entries: List<DayEntry>, year: Int, month: Int, today: Calendar = Calendar.getInstance()): MonthSummary {
        val lastDay = lastDayOfMonth(year, month)

        val firstHalf = entries.filter { it.day in 1..15 }
        val secondHalf = entries.filter { it.day in 16..lastDay }

        val p1Present = firstHalf.count { it.status == DayStatus.PRESENT }
        val p1Amount = firstHalf.filter { it.status == DayStatus.PRESENT }
            .sumOf { it.wageOnThatDay }

        val p2Present = secondHalf.count { it.status == DayStatus.PRESENT }
        val p2Amount = secondHalf.filter { it.status == DayStatus.PRESENT }
            .sumOf { it.wageOnThatDay }

        val todayY = today.get(Calendar.YEAR)
        val todayM = today.get(Calendar.MONTH) + 1
        val todayD = today.get(Calendar.DAY_OF_MONTH)

        // Period 1 ka paisa isi mahine ki 20 tarikh ko milta hai
        val p1DueToday = todayY == year && todayM == month && todayD == 20

        // Period 2 ka paisa agle mahine ki 5 tarikh ko milta hai
        val (payY, payM) = nextMonth(year, month)
        val p2DueToday = todayY == payY && todayM == payM && todayD == 5

        val period1 = PeriodSummary(
            periodKey = "%04d-%02d-P1".format(year, month),
            label = "1 - 15",
            presentDays = p1Present,
            totalAmount = p1Amount,
            paymentDateText = "Paisa milega: 20 tarikh ko",
            isPaymentDueToday = p1DueToday,
            paymentYear = year,
            paymentMonth = month,
            paymentDay = 20
        )

        val period2 = PeriodSummary(
            periodKey = "%04d-%02d-P2".format(year, month),
            label = "16 - $lastDay",
            presentDays = p2Present,
            totalAmount = p2Amount,
            paymentDateText = "Paisa milega: agle mahine ki 5 tarikh ko",
            isPaymentDueToday = p2DueToday,
            paymentYear = payY,
            paymentMonth = payM,
            paymentDay = 5
        )

        return MonthSummary(
            period1 = period1,
            period2 = period2,
            monthTotal = p1Amount + p2Amount
        )
    }
}
