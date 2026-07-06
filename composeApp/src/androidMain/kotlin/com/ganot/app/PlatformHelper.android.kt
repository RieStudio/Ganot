package com.ganot.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.Lecture
import com.ganot.app.ui.NotificationHelper
import com.ganot.app.ui.AcademicAppWidgetProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class AndroidPlatformHelper(private val context: Context) : PlatformHelper {
    override fun triggerNotification(id: Int, title: String, content: String) {
        NotificationHelper.showNotification(context, id, title, content)
    }

    override fun checkAndTriggerNotifications(events: List<CalendarEvent>, reminderDays: Int, reminderTimeStr: String) {
        NotificationHelper.checkAndTriggerNotifications(context, events, reminderDays, reminderTimeStr)
    }

    override fun checkAndTriggerAbsenceNotifications(lectures: List<Lecture>, isEnabled: Boolean) {
        NotificationHelper.checkAndTriggerAbsenceNotifications(context, lectures, isEnabled)
    }

    override fun triggerWidgetUpdate() {
        AcademicAppWidgetProvider.triggerUpdate(context)
    }

    override fun formatDate(timestamp: Long, pattern: String): String {
        val sdf = java.text.SimpleDateFormat(pattern, Locale("tr"))
        return sdf.format(Date(timestamp))
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun sendEmail(address: String, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$address")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("E-posta uygulaması bulunamadı.")
        }
    }

    override fun requestWidgetPin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            val myProvider = ComponentName(context, AcademicAppWidgetProvider::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null)
            } else {
                showToast("Widget bu cihazda desteklenmiyor.")
            }
        } else {
            showToast("Widget eklemek için ana ekranınıza basılı tutun.")
        }
    }

    override fun formatDouble(value: Double, decimals: Int): String {
        return "%.${decimals}f".format(Locale.US, value)
    }

    override fun toUppercaseTurkish(text: String): String {
        return text.uppercase(Locale("tr"))
    }

    override suspend fun showDatePicker(initialMs: Long): Long? = suspendCancellableCoroutine { continuation ->
        val cal = Calendar.getInstance().apply { timeInMillis = initialMs }
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                continuation.resume(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setOnCancelListener { continuation.resume(null) }
        dialog.show()
    }

    override suspend fun showTimePicker(hour: Int, minute: Int): Pair<Int, Int>? = suspendCancellableCoroutine { continuation ->
        val dialog = TimePickerDialog(
            context,
            { _, h, m ->
                continuation.resume(h to m)
            },
            hour,
            minute,
            true
        )
        dialog.setOnCancelListener { continuation.resume(null) }
        dialog.show()
    }
}

private var _platformHelper: PlatformHelper? = null

fun initPlatformHelper(context: Context) {
    _platformHelper = AndroidPlatformHelper(context)
}

actual fun getPlatformHelper(): PlatformHelper {
    return _platformHelper ?: throw IllegalStateException("PlatformHelper not initialized")
}
