package com.ganot.app.data

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun createDatabase(): RoomDatabase.Builder<AppDatabase>
}
