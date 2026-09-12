package com.salarytracker.app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Payment history ka record. Ek entry tab banti hai jab period ka paisa mil chuka hota hai
 * (20 tarikh ya 5 tarikh cross ho chuki hoti hai).
 * periodKey format: "yyyy-MM-P1" (1-15 ke liye) ya "yyyy-MM-P2" (16-30/31 ke liye) - unique
 */
@Entity(tableName = "payment_history")
data class PaymentHistory(
    @PrimaryKey val periodKey: String,
    val year: Int,
    val month: Int,
    val periodLabel: String,    // "1 - 15" ya "16 - 30"
    val presentDays: Int,
    val amount: Int,
    val paymentDateText: String, // "20 tarikh ko mila" / "5 tarikh ko mila"
    val markedPaidAt: Long       // timestamp jab yeh history me save hua
)
