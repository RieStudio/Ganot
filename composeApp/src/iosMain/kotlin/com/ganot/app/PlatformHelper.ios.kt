package com.ganot.app

import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.Lecture
import platform.Foundation.*

class IosPlatformHelper : PlatformHelper {
    override fun triggerNotification(id: Int, title: String, content: String) {}
    override fun checkAndTriggerNotifications(events: List<CalendarEvent>, reminderDays: Int, reminderTimeStr: String) {}
    override fun checkAndTriggerAbsenceNotifications(lectures: List<Lecture>, isEnabled: Boolean) {}
    override fun triggerWidgetUpdate() {}
    
    override fun formatDate(timestamp: Long, pattern: String): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
        val formatter = NSDateFormatter().apply {
            this.dateFormat = pattern
            this.locale = NSLocale.localeWithLocaleIdentifier("tr_TR")
        }
        return formatter.stringFromDate(date)
    }

    override fun showToast(message: String) {
        // iOS doesn't have Toast. Could use UIAlertController or a custom Compose view.
    }

    override fun sendEmail(address: String, subject: String, body: String) {
        val url = NSURL.URLWithString("mailto:$address?subject=${subject.replace(" ", "%20")}&body=${body.replace(" ", "%20")}")
        if (url != null) {
            platform.UIKit.UIApplication.sharedApplication.openURL(url)
        }
    }

    override fun requestWidgetPin() {
        // Not directly supported in the same way on iOS
    }

    override fun formatDouble(value: Double, decimals: Int): String {
        return NSString.stringWithFormat("%.${decimals}f", value)
    }

    override fun toUppercaseTurkish(text: String): String {
        val locale = NSLocale.localeWithLocaleIdentifier("tr_TR")
        return (text as Any as NSString).uppercaseStringWithLocale(locale)
    }

    override suspend fun showDatePicker(initialMs: Long): Long? {
        return null // Needs implementation via Compose or Platform UI
    }

    override suspend fun showTimePicker(hour: Int, minute: Int): Pair<Int, Int>? {
        return null // Needs implementation via Compose or Platform UI
    }
}

actual fun getPlatformHelper(): PlatformHelper = IosPlatformHelper()
