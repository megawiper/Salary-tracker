package com.salarytracker.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM day_entries WHERE year = :year AND month = :month")
    fun getEntriesForMonth(year: Int, month: Int): Flow<List<DayEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(entry: DayEntry)

    @Query("SELECT * FROM settings WHERE id = 0")
    fun getSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun getSettingsOnce(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: Settings)

    suspend fun saveSettingsIfMissing() {
        if (getSettingsOnce() == null) {
            saveSettings(Settings(id = 0, dailyWage = 580))
        }
    }

    // ---- Payment history ----

    @Query("SELECT * FROM payment_history ORDER BY markedPaidAt DESC")
    fun getAllHistory(): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_history WHERE periodKey = :periodKey")
    suspend fun getHistoryEntry(periodKey: String): PaymentHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistoryEntry(entry: PaymentHistory)
}
