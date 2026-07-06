package com.ganot.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganot.app.data.AcademicRepository
import com.ganot.app.data.CalendarEvent
import com.ganot.app.data.FocusSession
import com.ganot.app.data.Lecture
import com.ganot.app.data.GradeInfo
import com.ganot.app.data.getCoefficientsForSystem
import com.ganot.app.getSettings
import com.ganot.app.getPlatformHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*

class AcademicViewModel(private val repository: AcademicRepository) : ViewModel() {
    private val settings = getSettings()
    private val platformHelper = getPlatformHelper()

    val lectures: StateFlow<List<Lecture>>
    val events: StateFlow<List<CalendarEvent>>
    val sessions: StateFlow<List<FocusSession>>

    private val _userName = MutableStateFlow(settings.getString("user_name", ""))
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _completedTutorial = MutableStateFlow(settings.getBoolean("completed_tutorial", false))
    val completedTutorial: StateFlow<Boolean> = _completedTutorial.asStateFlow()

    private val _forceShowTutorial = MutableStateFlow(false)
    val forceShowTutorial: StateFlow<Boolean> = _forceShowTutorial.asStateFlow()

    private val _gradingSystemIndex = MutableStateFlow(settings.getInt("grading_system_index", 0))
    val gradingSystemIndex: StateFlow<Int> = _gradingSystemIndex.asStateFlow()

    private val _weightType = MutableStateFlow(settings.getString("weight_type", "KREDİ"))
    val weightType: StateFlow<String> = _weightType.asStateFlow()

    private val _isBellCurve = MutableStateFlow(settings.getBoolean("is_bell_curve", false))
    val isBellCurve: StateFlow<Boolean> = _isBellCurve.asStateFlow()

    val activeCoefficients: StateFlow<Map<String, GradeInfo>> = combine(
        _gradingSystemIndex
    ) { index ->
        getCoefficientsForSystem(index[0])
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = getCoefficientsForSystem(settings.getInt("grading_system_index", 0))
    )

    fun setGradingSystemIndex(index: Int) {
        settings.putInt("grading_system_index", index)
        _gradingSystemIndex.value = index
    }

    fun setWeightType(type: String) {
        settings.putString("weight_type", type)
        _weightType.value = type
    }

    fun setIsBellCurve(enabled: Boolean) {
        settings.putBoolean("is_bell_curve", enabled)
        _isBellCurve.value = enabled
    }

    private val _reminderDays = MutableStateFlow(settings.getInt("reminder_days", 3))
    val reminderDays: StateFlow<Int> = _reminderDays.asStateFlow()

    private val _reminderTime = MutableStateFlow(settings.getString("reminder_time", "20:00"))
    val reminderTime: StateFlow<String> = _reminderTime.asStateFlow()

    private val _isAbsenceNotificationEnabled = MutableStateFlow(settings.getBoolean("absence_notification_enabled", true))
    val isAbsenceNotificationEnabled: StateFlow<Boolean> = _isAbsenceNotificationEnabled.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(settings.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerMode = MutableStateFlow("POMODORO")
    val timerMode: StateFlow<String> = _timerMode.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _selectedLectureForTimer = MutableStateFlow<Int?>(null)
    val selectedLectureForTimer: StateFlow<Int?> = _selectedLectureForTimer.asStateFlow()

    private var timerJob: Job? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    init {
        lectures = repository.allLectures.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        events = repository.allEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        sessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            combine(events, _reminderTime) { eventList, rTime ->
                eventList to rTime
            }.collect { (eventList, rTime) ->
                platformHelper.checkAndTriggerNotifications(eventList, _reminderDays.value, rTime)
                platformHelper.triggerWidgetUpdate()
            }
        }

        viewModelScope.launch {
            combine(lectures, _isAbsenceNotificationEnabled) { lectureList, isEnabled ->
                lectureList to isEnabled
            }.collect { (lectureList, isEnabled) ->
                saveLecturesToPrefsSynchronously(lectureList)
                platformHelper.checkAndTriggerAbsenceNotifications(lectureList, isEnabled)
                platformHelper.triggerWidgetUpdate()
            }
        }
    }

    fun saveUserName(name: String) {
        settings.putString("user_name", name)
        _userName.value = name
    }

    fun completeTutorial() {
        settings.putBoolean("completed_tutorial", true)
        _completedTutorial.value = true
    }

    fun triggerTutorialReplay() {
        _forceShowTutorial.value = true
    }

    fun completeTutorialReplay() {
        _forceShowTutorial.value = false
    }

    fun updateReminderDays(days: Int) {
        settings.putInt("reminder_days", days)
        _reminderDays.value = days
        platformHelper.checkAndTriggerNotifications(events.value, days, _reminderTime.value)
    }

    fun updateReminderTime(timeStr: String) {
        settings.putString("reminder_time", timeStr)
        _reminderTime.value = timeStr
        platformHelper.checkAndTriggerNotifications(events.value, _reminderDays.value, timeStr)
    }

    fun updateAbsenceNotificationEnabled(enabled: Boolean) {
        settings.putBoolean("absence_notification_enabled", enabled)
        _isAbsenceNotificationEnabled.value = enabled
        platformHelper.checkAndTriggerAbsenceNotifications(lectures.value, enabled)
    }

    fun updateDarkTheme(enabled: Boolean) {
        settings.putBoolean("is_dark_theme", enabled)
        _isDarkTheme.value = enabled
    }

    fun clearAllData() {
        viewModelScope.launch {
            lectures.value.forEach { repository.deleteLecture(it) }
            events.value.forEach { repository.deleteEvent(it) }
            sessions.value.forEach { repository.deleteSession(it) }
            
            val currentTheme = _isDarkTheme.value
            settings.clear()
            settings.putBoolean("is_dark_theme", currentTheme)
            _userName.value = ""
            _completedTutorial.value = false
            resetTimer()
            _selectedLectureForTimer.value = null
        }
    }

    fun saveLecturesToPrefsSynchronously(lecturesList: List<Lecture>) {
        try {
            val json = Json.encodeToString(JsonArray.serializer(), JsonArray(lecturesList.map { lecture ->
                buildJsonObject {
                    put("id", lecture.id)
                    put("name", lecture.name)
                    put("currentAbsenteeism", lecture.currentAbsenteeism)
                    put("maxAbsenteeism", lecture.maxAbsenteeism)
                }
            }))
            settings.putString("widget_lectures_data", json)
        } catch (e: Exception) {
            // Log error platform-independently if possible, or just ignore for now
        }
    }

    fun addLecture(lecture: Lecture) {
        viewModelScope.launch {
            repository.insertLecture(lecture)
        }
    }

    fun updateLecture(lecture: Lecture) {
        viewModelScope.launch {
            repository.updateLecture(lecture)
        }
    }

    fun deleteLecture(lecture: Lecture) {
        viewModelScope.launch {
            repository.deleteLecture(lecture)
        }
    }

    fun updateAbsenteeism(lectureId: Int, delta: Int) {
        viewModelScope.launch {
            lectures.value.find { it.id == lectureId }?.let { lecture ->
                val newAbsenteeism = (lecture.currentAbsenteeism + delta).coerceAtLeast(0)
                repository.updateLecture(lecture.copy(currentAbsenteeism = newAbsenteeism))
            }
        }
    }

    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.insertEvent(event)
        }
    }

    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun toggleEventCompleted(event: CalendarEvent) {
        viewModelScope.launch {
            repository.updateEvent(event.copy(isCompleted = !event.isCompleted))
        }
    }

    fun setTimerMode(mode: String) {
        _timerMode.value = mode
        resetTimer()
    }

    fun selectLectureForTimer(lectureId: Int?) {
        _selectedLectureForTimer.value = lectureId
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true

        val mode = _timerMode.value
        val startTime = Clock.System.now().toEpochMilliseconds() - _elapsedMs.value

        timerJob = viewModelScope.launch {
            if (mode == "POMODORO") {
                val maxSeconds = 25 * 60
                while (_isTimerRunning.value) {
                    delay(1000)
                    _elapsedSeconds.value += 1
                    _elapsedMs.value = _elapsedSeconds.value * 1000L
                    if (_elapsedSeconds.value >= maxSeconds) {
                        completeTimerAndSave()
                        break
                    }
                }
            } else {
                while (_isTimerRunning.value) {
                    delay(30)
                    val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                    _elapsedMs.value = elapsed
                    _elapsedSeconds.value = (elapsed / 1000).toInt()
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _elapsedSeconds.value = 0
        _elapsedMs.value = 0L
    }

    fun completeTimerAndSave() {
        val duration = _elapsedMs.value
        val lectureId = _selectedLectureForTimer.value

        pauseTimer()
        _elapsedSeconds.value = 0
        _elapsedMs.value = 0L

        if (duration > 5000L) {
            viewModelScope.launch {
                repository.insertSession(
                    FocusSession(
                        lectureId = lectureId,
                        durationMs = duration,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun getCPAGradingTotal(): Pair<Double, Int> {
        val lectureList = lectures.value
        var weightedPointsSum = 0.0
        var totalCredits = 0

        val coefficients = getCoefficientsForSystem(gradingSystemIndex.value)

        lectureList.forEach { lecture ->
            val lGrade = lecture.getLetterGrade(coefficients)
            if (lGrade != "N/A" && lGrade.isNotBlank()) {
                val points = coefficients[lGrade]?.value ?: 0.0
                weightedPointsSum += points * lecture.credit
                totalCredits += lecture.credit
            }
        }

        val gpa = if (totalCredits > 0) weightedPointsSum / totalCredits else 0.0
        return Pair(gpa, totalCredits)
    }

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun resetShareState() {
        _shareState.value = ShareState.Idle
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    companion object {
        const val MAX_DAILY_SHARES = 50
    }

    fun canShareToday(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        // Simple day check (not as precise as SimpleDateFormat but works for KMP)
        val todayStart = (now / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000)
        val lastDate = settings.getLong("share_count_date_ms", 0)
        val currentCount = if (lastDate >= todayStart) {
            settings.getInt("share_count_today", 0)
        } else {
            0
        }
        return currentCount < MAX_DAILY_SHARES
    }

    private fun incrementShareCount() {
        val now = Clock.System.now().toEpochMilliseconds()
        val todayStart = (now / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000)
        val lastDate = settings.getLong("share_count_date_ms", 0)
        val currentCount = if (lastDate >= todayStart) {
            settings.getInt("share_count_today", 0)
        } else {
            0
        }
        settings.putLong("share_count_date_ms", now)
        settings.putInt("share_count_today", currentCount + 1)
    }

    fun serializeLecturesForSharing(lecturesList: List<Lecture>): String {
        return buildJsonObject {
            put("spec_version", 2)
            put("gradingSystemIndex", gradingSystemIndex.value)
            put("weightType", weightType.value)
            put("isBellCurve", isBellCurve.value)
            put("lectures", Json.encodeToJsonElement(lecturesList.map { lecture ->
                buildJsonObject {
                    put("name", lecture.name)
                    put("credit", lecture.credit)
                    put("maxAbsenteeism", lecture.maxAbsenteeism)
                    put("midtermWeight", lecture.midtermWeight)
                    put("finalWeight", lecture.finalWeight)
                    put("homeworkWeight", lecture.homeworkWeight)
                    put("projectWeight", lecture.projectWeight)
                    
                    val customCleaned = lecture.customCriteriaJson?.let { customJson ->
                        if (customJson.isNotBlank()) {
                            customJson.split(",").mapNotNull { item ->
                                val parts = item.split(":")
                                if (parts.size >= 2) {
                                    val cName = parts[0]
                                    val cWeight = parts[1].toDoubleOrNull() ?: 0.0
                                    "$cName:$cWeight:"
                                } else null
                            }.joinToString(",")
                        } else null
                    }
                    put("customCriteriaJson", customCleaned ?: "")
                }
            }))
        }.toString()
    }

    fun deserializeLecturesForSharing(jsonStr: String): List<Lecture> {
        return try {
            val json = Json.parseToJsonElement(jsonStr)
            val lecturesArray = if (json is JsonObject && json.containsKey("lectures")) {
                json["lectures"]!!.jsonArray
            } else if (json is JsonArray) {
                json
            } else return emptyList()

            lecturesArray.map { element ->
                val obj = element.jsonObject
                Lecture(
                    name = obj["name"]!!.jsonPrimitive.content,
                    credit = obj["credit"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3,
                    maxAbsenteeism = obj["maxAbsenteeism"]?.jsonPrimitive?.content?.toIntOrNull() ?: 12,
                    currentAbsenteeism = 0,
                    midtermGrade = null,
                    midtermWeight = obj["midtermWeight"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 40.0,
                    finalGrade = null,
                    finalWeight = obj["finalWeight"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 60.0,
                    homeworkGrade = null,
                    homeworkWeight = obj["homeworkWeight"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    projectGrade = null,
                    projectWeight = obj["projectWeight"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                    customCriteriaJson = obj["customCriteriaJson"]?.jsonPrimitive?.content?.let { if (it.isBlank()) null else it }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun shareLectures() {
        val currentLectures = lectures.value
        if (currentLectures.isEmpty()) {
            _shareState.value = ShareState.Error("Paylaşılacak hiç ders bulunmuyor! Önce ders ekleyin.")
            return
        }

        if (!canShareToday()) {
            _shareState.value = ShareState.Error("Günlük paylaşım sınırına ulaştınız. Yarın yeni kodlar oluşturmaya devam edebilirsiniz.")
            return
        }

        _shareState.value = ShareState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val jsonPayload = serializeLecturesForSharing(currentLectures)
            
            var resultingShortCode: String? = null
            val errorSteps = mutableListOf<String>()

            // RESTFUL-API strategy
            try {
                val response = client.post("https://api.restful-api.dev/objects") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("name", "AcademicLectureShared_${Clock.System.now().toEpochMilliseconds()}")
                        put("data", buildJsonObject {
                            put("payload", jsonPayload)
                        })
                    })
                }
                if (response.status.isSuccess()) {
                    val responseBody = response.bodyAsText()
                    val responseObj = Json.parseToJsonElement(responseBody).jsonObject
                    val objectId = responseObj["id"]?.jsonPrimitive?.content
                    if (!objectId.isNullOrBlank()) {
                        val longUrl = "https://api.restful-api.dev/objects/$objectId"
                        resultingShortCode = shortenUrlWithTinyUrl(longUrl) ?: objectId
                    }
                } else {
                    errorSteps.add("[GCloud Server Error: Code ${response.status.value}]")
                }
            } catch (e: Exception) {
                errorSteps.add("GCloud error: ${e.message}")
            }

            // Fallback strategies (ExtendsClass, Paste.c-net) could be implemented similarly with client.post
            if (resultingShortCode == null) {
                try {
                    val response = client.post("https://api.extendsclass.com/bin") {
                        contentType(ContentType.Application.Json)
                        header("Security-key", "academic_key_share")
                        setBody(jsonPayload)
                    }
                    if (response.status.isSuccess()) {
                        val responseBody = response.bodyAsText()
                        val responseObj = Json.parseToJsonElement(responseBody).jsonObject
                        val binId = responseObj["id"]?.jsonPrimitive?.content
                        if (!binId.isNullOrBlank()) {
                            val longUrl = "https://api.extendsclass.com/bin/$binId"
                            resultingShortCode = shortenUrlWithTinyUrl(longUrl) ?: binId
                        }
                    }
                } catch (e: Exception) {
                    errorSteps.add("ExtendsClass error: ${e.message}")
                }
            }

            if (resultingShortCode != null) {
                incrementShareCount()
                _shareState.value = ShareState.Success(resultingShortCode!!)
            } else {
                val detailedMsg = if (errorSteps.isNotEmpty()) {
                    "\n\nBağlantı Sorunu Detayları:\n" + errorSteps.joinToString("\n")
                } else ""
                _shareState.value = ShareState.Error("Hata: Üzgünüz, paylaşım kodu oluşturulamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.$detailedMsg")
            }
        }
    }

    private suspend fun shortenUrlWithTinyUrl(longUrl: String): String? {
        return try {
            val response = client.get("https://tinyurl.com/api-create.php") {
                parameter("url", longUrl)
            }
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText().trim()
                if (responseBody.startsWith("https://tinyurl.com/")) {
                    responseBody.substringAfterLast("/")
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPayloadFromResponseBody(body: String): String? {
        return try {
            val json = Json.parseToJsonElement(body)
            if (json is JsonArray) return body
            if (json is JsonObject) {
                if (json.containsKey("data")) {
                    val data = json["data"]?.jsonObject
                    if (data?.containsKey("payload") == true) {
                        return data["payload"]?.jsonPrimitive?.content
                    }
                }
                if (json.containsKey("payload")) {
                    return json["payload"]?.jsonPrimitive?.content
                }
                if (json.containsKey("lectures") || json.containsKey("spec_version")) {
                    return body
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchLecturesForImport(code: String) {
        var cleanCode = code.trim()
        if (cleanCode.contains("/")) cleanCode = cleanCode.substringAfterLast("/").trim()
        if (cleanCode.contains("?")) cleanCode = cleanCode.substringBefore("?").trim()
        
        if (cleanCode.isBlank()) {
            _importState.value = ImportState.Error("Geçersiz kod!")
            return
        }

        _importState.value = ImportState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            var rawJsonPayload: String? = null
            
            val strategies = listOf(
                "https://tinyurl.com/$cleanCode",
                "https://api.restful-api.dev/objects/$cleanCode",
                "https://api.extendsclass.com/bin/$cleanCode",
                "https://paste.c-net.org/$cleanCode"
            )

            for (url in strategies) {
                try {
                    val response = client.get(url)
                    if (response.status.isSuccess()) {
                        val body = response.bodyAsText()
                        rawJsonPayload = extractPayloadFromResponseBody(body)
                        if (rawJsonPayload != null) break
                    }
                } catch (e: Exception) {}
            }

            if (rawJsonPayload != null) {
                try {
                    val json = Json.parseToJsonElement(rawJsonPayload!!)
                    var gradingSystemIndexVal = 0
                    var weightTypeVal = "KREDİ"
                    var isBellCurveVal = false
                    
                    val parsedLectures = if (json is JsonObject) {
                        gradingSystemIndexVal = json["gradingSystemIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        weightTypeVal = json["weightType"]?.jsonPrimitive?.content ?: "KREDİ"
                        isBellCurveVal = json["isBellCurve"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                        deserializeLecturesForSharing(rawJsonPayload!!)
                    } else {
                        deserializeLecturesForSharing(rawJsonPayload!!)
                    }

                    if (parsedLectures.isEmpty()) {
                        _importState.value = ImportState.Error("Paylaşılan kodda geçerli bir ders ayarı bulunamadı.")
                    } else {
                        _importState.value = ImportState.Confirm(cleanCode, parsedLectures, gradingSystemIndexVal, weightTypeVal, isBellCurveVal)
                    }
                } catch (e: Exception) {
                    _importState.value = ImportState.Error("Veri ayrıştırılamadı.")
                }
            } else {
                _importState.value = ImportState.Error("Kod bulunamadı veya süresi dolmuş.")
            }
        }
    }

    fun applyImportedLectures(importedLectures: List<Lecture>, importGradingSystemIndex: Int, importWeightType: String, importIsBellCurve: Boolean) {
        viewModelScope.launch {
            lectures.value.forEach { repository.deleteLecture(it) }
            events.value.forEach { repository.deleteEvent(it) }
            sessions.value.forEach { repository.deleteSession(it) }
            importedLectures.forEach { repository.insertLecture(it) }
            setGradingSystemIndex(importGradingSystemIndex)
            setWeightType(importWeightType)
            setIsBellCurve(importIsBellCurve)
            _importState.value = ImportState.Success
        }
    }
}

sealed class ShareState {
    object Idle : ShareState()
    object Loading : ShareState()
    data class Success(val code: String) : ShareState()
    data class Error(val message: String) : ShareState()
}

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Confirm(val code: String, val fetchedLectures: List<Lecture>, val gradingSystemIndex: Int, val weightType: String, val isBellCurve: Boolean) : ImportState()
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
}
