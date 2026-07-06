package com.ganot.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lectures ORDER BY name ASC")
    fun getAllLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    fun getLectureById(id: Int): Flow<Lecture?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: Lecture): Long

    @Update
    suspend fun updateLecture(lecture: Lecture)

    @Delete
    suspend fun deleteLecture(lecture: Lecture)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY dueDate ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent): Long

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE lectureId = :lectureId ORDER BY timestamp DESC")
    fun getSessionsForLecture(lectureId: Int): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Delete
    suspend fun deleteSession(session: FocusSession)
}
