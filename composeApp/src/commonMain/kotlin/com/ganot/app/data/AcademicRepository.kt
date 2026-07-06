package com.ganot.app.data

import kotlinx.coroutines.flow.Flow

class AcademicRepository(private val database: AppDatabase) {
    val lectureDao = database.lectureDao()
    val calendarEventDao = database.calendarEventDao()
    val focusSessionDao = database.focusSessionDao()

    val allLectures: Flow<List<Lecture>> = lectureDao.getAllLectures()
    val allEvents: Flow<List<CalendarEvent>> = calendarEventDao.getAllEvents()
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()

    fun getLectureById(id: Int): Flow<Lecture?> {
        return lectureDao.getLectureById(id)
    }

    suspend fun insertLecture(lecture: Lecture): Long {
        return lectureDao.insertLecture(lecture)
    }

    suspend fun updateLecture(lecture: Lecture) {
        lectureDao.updateLecture(lecture)
    }

    suspend fun deleteLecture(lecture: Lecture) {
        lectureDao.deleteLecture(lecture)
    }

    suspend fun insertEvent(event: CalendarEvent): Long {
        return calendarEventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: CalendarEvent) {
        calendarEventDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: CalendarEvent) {
        calendarEventDao.deleteEvent(event)
    }

    suspend fun insertSession(session: FocusSession): Long {
        return focusSessionDao.insertSession(session)
    }

    suspend fun deleteSession(session: FocusSession) {
        focusSessionDao.deleteSession(session)
    }

    fun getSessionsForLecture(lectureId: Int): Flow<List<FocusSession>> {
        return focusSessionDao.getSessionsForLecture(lectureId)
    }
}
