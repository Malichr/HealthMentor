package com.example.healthmentor.models

import com.google.firebase.Timestamp
import java.util.*

data class MemberStepCount(
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val count: Int = 0
) {
    fun getFormattedDate(): Date {
        return date.toDate()
    }
}

data class SleepData(
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val durationMinutes: Int = 0
) {
    fun getFormattedDate(): Date {
        return date.toDate()
    }
}

data class CaloriesData(
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val calories: Int = 0
) {
    fun getFormattedDate(): Date {
        return date.toDate()
    }
}

data class DistanceData(
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val distance: Double = 0.0
) {
    fun getFormattedDate(): Date {
        return date.toDate()
    }
} 