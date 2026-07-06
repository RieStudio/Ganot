package com.ganot.app.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual class DatabaseFactory {
    actual fun createDatabase(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = NSHomeDirectory() + "/academic_assistant_database.db"
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile,
            factory =  { AppDatabase::class.instantiateImpl() }
        )
    }
}
