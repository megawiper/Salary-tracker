package com.salarytracker.app

import java.util.Calendar

object CalendarUtils {

    val hindiMonthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    /** Diye gaye month ka pehla din kis weekday (0=Sunday) se start hota hai */
    fun firstWeekdayOfMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY = 1, isliye -1
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun isSunday(year: Int, month: Int, day: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day)
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    fun isToday(year: Int, month: Int, day: Int): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == year &&
            today.get(Calendar.MONTH) + 1 == month &&
            today.get(Calendar.DAY_OF_MONTH) == day
    }

    fun isFutureDate(year: Int, month: Int, day: Int): Boolean {
        val target = Calendar.getInstance()
        target.set(year, month - 1, day, 23, 59, 59)
        val now = Calendar.getInstance()
        return target.after(now)
    }
}
