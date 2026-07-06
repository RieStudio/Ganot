package com.ganot.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.ganot.app.MainActivity
import com.ganot.app.R
import com.ganot.app.data.DatabaseFactory
import com.ganot.app.data.Lecture
import com.ganot.app.data.getRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class AcademicAppWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SWAP_UPDATE || 
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || 
            intent.action == "com.ganot.app.ACTION_WIDGET_REFRESH") {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, AcademicAppWidgetProvider::class.java))
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val sharedPrefs = context.getSharedPreferences("academic_assistant_prefs", Context.MODE_PRIVATE)
        val lecturesJson = sharedPrefs.getString("widget_lectures_data", null)
        var lecturesList: List<Lecture>? = null

        if (lecturesJson != null) {
            try {
                val jsonArray = JSONArray(lecturesJson)
                val list = mutableListOf<Lecture>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Lecture(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            currentAbsenteeism = obj.getInt("currentAbsenteeism"),
                            maxAbsenteeism = obj.getInt("maxAbsenteeism")
                        )
                    )
                }
                lecturesList = list
            } catch (e: Exception) {
                Log.e("AcademicWidgetProvider", "Error parsing cached widget lectures", e)
            }
        }

        if (lecturesList != null) {
            // Instant, synchronous update for total resilience and zero lag
            appWidgetIds.forEach { widgetId ->
                val views = buildRemoteViews(context, lecturesList)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        } else {
            // DB fallback
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val db = getRoomDatabase(DatabaseFactory(context).createDatabase())
                    val listFromDb = db.lectureDao().getAllLectures().first()

                    launch(Dispatchers.Main) {
                        appWidgetIds.forEach { widgetId ->
                            val views = buildRemoteViews(context, listFromDb)
                            appWidgetManager.updateAppWidget(widgetId, views)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AcademicWidgetProvider", "Error updating widgets from DB fallback", e)
                }
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        lectures: List<Lecture>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.academic_widget_layout)

        // Light / Dark configuration detection
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkMode = uiMode == Configuration.UI_MODE_NIGHT_YES

        // Setup colors programmatically for high contrast and modern UI
        val textColorPrimary: Int
        val textColorSecondary: Int
        val bgResId: Int
        val slotBgRes: Int

        if (isDarkMode) {
            bgResId = R.drawable.academic_widget_bg_dark
            textColorPrimary = Color.WHITE
            textColorSecondary = Color.parseColor("#9CA3AF") // light grey
            slotBgRes = R.drawable.academic_widget_slot_bg_dark
        } else {
            bgResId = R.drawable.academic_widget_bg_light
            textColorPrimary = Color.parseColor("#1F2937") // dark slate
            textColorSecondary = Color.parseColor("#4B5563") // medium grey
            slotBgRes = R.drawable.academic_widget_slot_bg_light
        }

        // Apply general settings
        views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)
        views.setTextColor(R.id.widget_abs_title, textColorPrimary)

        // Open app when widget is clicked
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Process Absence Limits
        // Find lectures where maxAbsenteeism > 0 and remaining is <= 3
        val criticalLectures = lectures.filter {
            it.maxAbsenteeism > 0 && (it.maxAbsenteeism - it.currentAbsenteeism) <= 3
        }.sortedBy { it.maxAbsenteeism - it.currentAbsenteeism }

        if (criticalLectures.isEmpty()) {
            views.setViewVisibility(R.id.widget_abs_empty, View.VISIBLE)
            views.setViewVisibility(R.id.widget_abs_list, View.GONE)
            views.setViewVisibility(R.id.widget_abs_badge, View.GONE)
            views.setTextColor(R.id.widget_abs_empty, textColorSecondary)
        } else {
            views.setViewVisibility(R.id.widget_abs_empty, View.GONE)
            views.setViewVisibility(R.id.widget_abs_list, View.VISIBLE)
            
            // Show alarm badge (⚠️ icon) if any lecture is highly critical or limit exceeded
            val hasHighDanger = criticalLectures.any { (it.maxAbsenteeism - it.currentAbsenteeism) <= 1 }
            views.setViewVisibility(R.id.widget_abs_badge, if (hasHighDanger) View.VISIBLE else View.GONE)

            // Fill slots 1, 2, 3, 4
            val slots = listOf(
                Triple(R.id.abs_slot_1, R.id.abs_slot_1_name, R.id.abs_slot_1_info),
                Triple(R.id.abs_slot_2, R.id.abs_slot_2_name, R.id.abs_slot_2_info),
                Triple(R.id.abs_slot_3, R.id.abs_slot_3_name, R.id.abs_slot_3_info),
                Triple(R.id.abs_slot_4, R.id.abs_slot_4_name, R.id.abs_slot_4_info)
            )

            for (i in slots.indices) {
                val (slotContainerId, nameId, infoId) = slots[i]
                if (i < criticalLectures.size) {
                    val lecture = criticalLectures[i]
                    val remaining = lecture.maxAbsenteeism - lecture.currentAbsenteeism
                    
                    views.setViewVisibility(slotContainerId, View.VISIBLE)
                    views.setInt(slotContainerId, "setBackgroundResource", slotBgRes)
                    views.setTextViewText(nameId, lecture.name)
                    views.setTextColor(nameId, textColorPrimary)

                    val infoText: String
                    val infoColor: Int
                    if (remaining < 0) {
                        infoText = "Sınır Aşıldı (${lecture.currentAbsenteeism}/${lecture.maxAbsenteeism})"
                        infoColor = Color.parseColor("#EF4444") // bright red
                    } else if (remaining == 0) {
                        infoText = "Limit Doldu (${lecture.currentAbsenteeism}/${lecture.maxAbsenteeism})"
                        infoColor = Color.parseColor("#EF4444") // bright red
                    } else if (remaining == 1) {
                        infoText = "Son 1 Hak!"
                        infoColor = Color.parseColor("#F97316") // critical orange
                    } else {
                        infoText = "$remaining hak kaldı"
                        infoColor = if (isDarkMode) Color.parseColor("#FBBF24") else Color.parseColor("#D97706") // warm amber
                    }

                    views.setTextViewText(infoId, infoText)
                    views.setTextColor(infoId, infoColor)
                } else {
                    views.setViewVisibility(slotContainerId, View.GONE)
                }
            }
        }

        return views
    }

    companion object {
        const val ACTION_SWAP_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, AcademicAppWidgetProvider::class.java).apply {
                action = "com.ganot.app.ACTION_WIDGET_REFRESH"
            }
            context.sendBroadcast(intent)
        }
    }
}
