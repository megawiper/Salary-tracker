package com.salarytracker.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Har din ka record. dateKey format: "yyyy-MM-dd" (unique key).
 * status: "PRESENT" (kaam kiya, paisa milega)
 *         "ABSENT"  (kaam nahi kiya, paisa nahi)
 *         "OFF"     (Sunday default off, paisa nahi - jab tak manually present na kiya ho)
 */
@Entity(tableName = "day_entries")
data class DayEntry(
    @PrimaryKey val dateKey: String,
    val year: Int,
    val month: Int, // 1-12
    val day: Int,
    val isSunday: Boolean,
    val status: String, // PRESENT / ABSENT / OFF
    val wageOnThatDay: Int // us din ki dar (rate change hone par purane din wahi rahenge)
)

object DayStatus {
    const val PRESENT = "PRESENT"
    const val ABSENT = "ABSENT"
    const val OFF = "OFF"
}
