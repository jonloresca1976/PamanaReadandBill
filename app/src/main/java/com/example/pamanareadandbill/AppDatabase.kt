package com.example.pamanareadandbill

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerInfo::class,
        MeterReading::class,
        WaterRates::class,
        MeterReaders::class,
        FieldFindings::class,
        ReadHistory::class,
        ProxAlerts::class],
    version = 6
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun historyDao(): HistoryDao

    abstract fun findingsDao(): FindingsDao

    abstract fun waterRatesDao(): WaterRatesDao

    abstract fun meterReadingDao(): MeterReadingDao

    abstract fun meterReaderDao(): MeterReaderDao
}




