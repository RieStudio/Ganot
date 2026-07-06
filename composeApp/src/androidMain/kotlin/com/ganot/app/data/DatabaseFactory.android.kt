package com.ganot.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun createDatabase(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("academic_assistant_database")
        return Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
    }
}
