package com.ganot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class GradeCriterion(
    val name: String,
    val weight: Double,
    val grade: Double?
)

@Entity(tableName = "lectures")
@Serializable
data class Lecture(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val credit: Int = 3,
    val maxAbsenteeism: Int = 12,
    val currentAbsenteeism: Int = 0,
    val midtermGrade: Double? = null,
    val midtermWeight: Double = 40.0,
    val finalGrade: Double? = null,
    val finalWeight: Double = 60.0,
    val homeworkGrade: Double? = null,
    val homeworkWeight: Double = 0.0,
    val projectGrade: Double? = null,
    val projectWeight: Double = 0.0,
    val customCriteriaJson: String? = null,
    val manualLetterGrade: String? = null
) {
    fun getCriteriaList(): List<GradeCriterion> {
        if (customCriteriaJson.isNullOrBlank()) {
            val list = mutableListOf<GradeCriterion>()
            list.add(GradeCriterion("Vize", midtermWeight, midtermGrade))
            list.add(GradeCriterion("Final", finalWeight, finalGrade))
            if (homeworkWeight > 0.0 || homeworkGrade != null) {
                list.add(GradeCriterion("Ödev", homeworkWeight, homeworkGrade))
            }
            if (projectWeight > 0.0 || projectGrade != null) {
                list.add(GradeCriterion("Proje", projectWeight, projectGrade))
            }
            return list
        }

        return try {
            customCriteriaJson.split(",").mapNotNull { item ->
                val parts = item.split(":")
                if (parts.size >= 2) {
                    val criterionName = parts[0]
                    val criterionWeight = parts[1].toDoubleOrNull() ?: 0.0
                    val criterionGrade = parts.getOrNull(2)?.toDoubleOrNull()
                    GradeCriterion(criterionName, criterionWeight, criterionGrade)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun calculateCurrentGrade(): Double? {
        val criteria = getCriteriaList()
        var weightedSum = 0.0
        var weightSum = 0.0

        criteria.forEach { criterion ->
            criterion.grade?.let {
                weightedSum += it * criterion.weight
                weightSum += criterion.weight
            }
        }

        if (weightSum == 0.0) return null
        return weightedSum / weightSum
    }

    fun getLetterGrade(coefficients: Map<String, GradeInfo>? = null): String {
        if (!manualLetterGrade.isNullOrBlank()) return manualLetterGrade
        val grade = calculateCurrentGrade() ?: return "N/A"
        
        val targetMap = coefficients ?: getCoefficientsForSystem(0)
        
        val matchingGrades = targetMap.entries
            .filter { grade >= it.value.minScore }
            .sortedWith(
                compareByDescending<Map.Entry<String, GradeInfo>> { it.value.minScore }
                    .thenByDescending { it.value.value }
                    .thenBy { if (it.key == "FZ") 1 else 0 }
            )
            
        return matchingGrades.firstOrNull()?.key ?: "FF"
    }

    fun getGradePoints(coefficients: Map<String, GradeInfo>? = null): Double {
        val lGrade = getLetterGrade(coefficients)
        val targetMap = coefficients ?: getCoefficientsForSystem(0)
        return targetMap[lGrade]?.value ?: 0.0
    }
}

@Entity(tableName = "calendar_events")
@Serializable
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lectureId: Int? = null,
    val title: String,
    val type: String, // "SINAV", "ÖDEV", "PROJE", "DERS"
    val dueDate: Long,
    val description: String = "",
    val isCompleted: Boolean = false,
    val classroom: String = ""
)

@Entity(tableName = "focus_sessions")
@Serializable
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lectureId: Int? = null,
    val durationMs: Long,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class GradeInfo(
    val value: Double,
    val minScore: Int
)

data class University(
    val id: String,
    val name: String,
    val weightType: String,
    val hasBellCurve: Boolean,
    val gradingSystem: String,
    val coefficients: Map<String, GradeInfo>
)

fun getCoefficientsForSystem(index: Int): Map<String, GradeInfo> {
    return when (index) {
        1 -> mapOf(
            "AA" to GradeInfo(4.0, 90),
            "AB" to GradeInfo(3.7, 85),
            "BA" to GradeInfo(3.3, 80),
            "BB" to GradeInfo(3.0, 75),
            "BC" to GradeInfo(2.7, 70),
            "CB" to GradeInfo(2.3, 65),
            "CC" to GradeInfo(2.0, 60),
            "CD" to GradeInfo(1.7, 55),
            "DC" to GradeInfo(1.3, 50),
            "DD" to GradeInfo(1.0, 45),
            "FF" to GradeInfo(0.0, 0)
        )
        2 -> mapOf(
            "A" to GradeInfo(4.0, 90),
            "A-" to GradeInfo(3.7, 85),
            "B+" to GradeInfo(3.3, 80),
            "B" to GradeInfo(3.0, 75),
            "B-" to GradeInfo(2.7, 70),
            "C+" to GradeInfo(2.3, 65),
            "C" to GradeInfo(2.0, 60),
            "C-" to GradeInfo(1.7, 55),
            "D+" to GradeInfo(1.3, 50),
            "D" to GradeInfo(1.0, 45),
            "D-" to GradeInfo(0.7, 40),
            "F" to GradeInfo(0.0, 0)
        )
        3 -> mapOf(
            "A1" to GradeInfo(4.0, 90),
            "A2" to GradeInfo(3.75, 85),
            "A3" to GradeInfo(3.5, 80),
            "B1" to GradeInfo(3.25, 75),
            "B2" to GradeInfo(3.0, 70),
            "B3" to GradeInfo(2.75, 65),
            "C1" to GradeInfo(2.5, 60),
            "C2" to GradeInfo(2.25, 55),
            "C3" to GradeInfo(2.0, 50),
            "D" to GradeInfo(1.5, 40),
            "F" to GradeInfo(0.0, 0)
        )
        else -> mapOf(
            "AA" to GradeInfo(4.0, 90),
            "BA" to GradeInfo(3.5, 85),
            "BB" to GradeInfo(3.0, 75),
            "CB" to GradeInfo(2.5, 70),
            "CC" to GradeInfo(2.0, 60),
            "DC" to GradeInfo(1.5, 55),
            "DD" to GradeInfo(1.0, 45),
            "FD" to GradeInfo(0.5, 40),
            "FF" to GradeInfo(0.0, 0)
        )
    }
}
