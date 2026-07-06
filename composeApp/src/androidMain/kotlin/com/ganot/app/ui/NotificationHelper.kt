package com.ganot.app.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ganot.app.MainActivity
import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.Lecture
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "academic_reminders_channel"
    private const val CHANNEL_NAME = "Akademik Hatırlatıcılar"
    private const val CHANNEL_DESC = "Sınav ve ödev hatırlatma bildirimleri"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("NotificationPermission")
    fun showNotification(context: Context, id: Int, title: String, content: String) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.ganot.app.R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    fun checkAndTriggerNotifications(context: Context, events: List<CalendarEvent>, reminderDays: Int, reminderTimeStr: String = "20:00") {
        if (reminderDays < 0) return

        val sharedPrefs = context.getSharedPreferences("academic_assistant_prefs", Context.MODE_PRIVATE)
        val notifiedIdsString = sharedPrefs.getString("notified_event_ids", "") ?: ""
        val notifiedIds = notifiedIdsString.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()

        val currentTime = System.currentTimeMillis()
        var preferencesUpdated = false

        val timeParts = reminderTimeStr.split(":")
        val targetHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
        val targetMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        events.forEach { event ->
            if (event.isCompleted) return@forEach

            val triggerCal = Calendar.getInstance().apply {
                timeInMillis = event.dueDate
                add(Calendar.DAY_OF_YEAR, -reminderDays)
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val triggerTimeMs = triggerCal.timeInMillis

            if (currentTime >= triggerTimeMs && currentTime < event.dueDate) {
                if (!notifiedIds.contains(event.id)) {
                    val eventTypeTurkish = when (event.type) {
                        "SINAV" -> "Sınavı"
                        "ÖDEV" -> "Ödevi"
                        "PROJE" -> "Projesi"
                        else -> "Etkinliği"
                    }
                    
                    val title = "Yaklaşan Akademik Görev! 📅"
                    
                    val differenceMs = event.dueDate - currentTime
                    val diffDays = (differenceMs / (1000 * 60 * 60 * 24)).toInt()
                    
                    val content = if (diffDays <= 0) {
                        "${event.title} $eventTypeTurkish bugün gerçekleşecek/teslim edilecek!"
                    } else {
                        "${event.title} $eventTypeTurkish için son $diffDays gün kaldı!"
                    }

                    showNotification(context, event.id, title, content)
                    notifiedIds.add(event.id)
                    preferencesUpdated = true
                }
            }
        }

        if (preferencesUpdated) {
            sharedPrefs.edit().putString("notified_event_ids", notifiedIds.joinToString(",")).apply()
        }
    }

    fun checkAndTriggerAbsenceNotifications(context: Context, lectures: List<Lecture>, isEnabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("academic_assistant_prefs", Context.MODE_PRIVATE)
        val actualEnabled = sharedPrefs.getBoolean("absence_notification_enabled", true)
        if (!isEnabled || !actualEnabled) {
            return
        }

        val notifiedIdsString = sharedPrefs.getString("notified_absence_lecture_ids", "") ?: ""
        val notifiedIds = notifiedIdsString.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }.toMutableSet()

        var preferencesUpdated = false

        lectures.forEach { lecture ->
            val remaining = lecture.maxAbsenteeism - lecture.currentAbsenteeism
            if (remaining == 1 && lecture.maxAbsenteeism > 0) {
                if (!notifiedIds.contains(lecture.id)) {
                    val title = "Devamsızlık Sınırı Uyarısı! ⚠️"
                    val content = "${lecture.name} dersi için sadece 1 devamsızlık hakkınız kaldı! Durum: ${lecture.currentAbsenteeism}/${lecture.maxAbsenteeism}"

                    showNotification(context, lecture.id + 10000, title, content)
                    notifiedIds.add(lecture.id)
                    preferencesUpdated = true
                }
            } else {
                if (notifiedIds.contains(lecture.id)) {
                    notifiedIds.remove(lecture.id)
                    preferencesUpdated = true
                }
            }
        }

        if (preferencesUpdated) {
            sharedPrefs.edit().putString("notified_absence_lecture_ids", notifiedIds.joinToString(",")).apply()
        }
    }
}
