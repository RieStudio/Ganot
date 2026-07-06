package com.ganot.app

import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.Lecture

interface PlatformHelper {
    fun triggerNotification(id: Int, title: String, content: String)
    fun checkAndTriggerNotifications(events: List<CalendarEvent>, reminderDays: Int, reminderTimeStr: String)
    fun checkAndTriggerAbsenceNotifications(lectures: List<Lecture>, isEnabled: Boolean)
    fun triggerWidgetUpdate()
    fun formatDate(timestamp: Long, pattern: String): String
    fun showToast(message: String)
    fun sendEmail(address: String, subject: String, body: String)
    fun requestWidgetPin()
    fun formatDouble(value: Double, decimals: Int): String
    fun toUppercaseTurkish(text: String): String
    
    // Platform-specific pickers (simplified for now, ideally they'd be Composables or use a KMP lib)
    suspend fun showDatePicker(initialMs: Long): Long?
    suspend fun showTimePicker(hour: Int, minute: Int): Pair<Int, Int>?
}

expect fun getPlatformHelper(): PlatformHelper
