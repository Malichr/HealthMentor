package com.example.healthmentor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit
import java.util.Date

class FitnessDataService : JobIntentService() {

    companion object {
        private const val TAG = "FitnessDataService"
        private const val JOB_ID = 1001
        private const val EXTRA_ACCOUNT = "extra_account"

        fun enqueueWork(context: Context, account: GoogleSignInAccount) {
            val intent = Intent(context, FitnessDataService::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account)
            }
            enqueueWork(context, FitnessDataService::class.java, JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "Starting FitnessDataService")
        val account = intent.getParcelableExtra<GoogleSignInAccount>(EXTRA_ACCOUNT) ?: return

        val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val totalEndTime = endTime
        val totalStartTime = totalEndTime - TimeUnit.DAYS.toMillis(365)

        val dailyRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
            .aggregate(DataType.TYPE_SPEED, DataType.AGGREGATE_SPEED_SUMMARY)
            .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
            .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

        val totalRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(totalStartTime, totalEndTime, TimeUnit.MILLISECONDS)
            .build()

        try {
            val historyClient = Fitness.getHistoryClient(this, account)
            
            historyClient.readData(dailyRequest)
                .addOnSuccessListener { dailyResponse ->
                    var totalCalories = 0f
                    var activeCalories = 0f
                    var totalSteps = 0
                    var dailySteps = 0
                    var totalDistance = 0f
                    var avgSpeed = 0f
                    var activityType = "Unknown"

                    for (bucket in dailyResponse.buckets) {
                        for (dataset in bucket.dataSets) {
                            for (dataPoint in dataset.dataPoints) {
                                when (dataset.dataType.name) {
                                    "com.google.step_count.delta" -> {
                                        val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                        totalSteps += steps
                                        dailySteps += steps
                                        Log.d(TAG, "Field steps: $steps")
                                    }
                                    "com.google.calories.expended" -> {
                                        val calories = dataPoint.getValue(Field.FIELD_CALORIES).asFloat()
                                        totalCalories += calories
                                        activeCalories += calories
                                        Log.d(TAG, "Field calories: $calories")
                                    }
                                    "com.google.distance.delta" -> {
                                        val distance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat()
                                        totalDistance += distance
                                        Log.d(TAG, "Field distance: $distance")
                                    }
                                    "com.google.speed.summary" -> {
                                        avgSpeed = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat()
                                        Log.d(TAG, "Field average speed: $avgSpeed")
                                    }
                                    "com.google.activity.segment" -> {
                                        activityType = when (dataPoint.getValue(Field.FIELD_ACTIVITY).asInt()) {
                                            7 -> "Walking"
                                            8 -> "Running"
                                            1 -> "Biking"
                                            else -> "Other"
                                        }
                                        Log.d(TAG, "Field activity type: $activityType")
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Total steps: $totalSteps")
                    Log.d(TAG, "Daily steps: $dailySteps")
                    Log.d(TAG, "Total calories: $totalCalories")
                    Log.d(TAG, "Active calories: $activeCalories")
                    Log.d(TAG, "Total distance: $totalDistance")
                    Log.d(TAG, "Average speed: $avgSpeed")
                    Log.d(TAG, "Activity type: $activityType")

                    historyClient.readData(totalRequest)
                        .addOnSuccessListener { totalResponse ->
                            var totalCalories = 0f
                            var activeDays = 0
                            var currentStreak = 0
                            var longestStreak = 0
                            var streakCount = 0

                            for (bucket in totalResponse.buckets) {
                                var hasActivity = false
                                for (dataset in bucket.dataSets) {
                                    for (dataPoint in dataset.dataPoints) {
                                        when (dataset.dataType.name) {
                                            "com.google.step_count.delta" -> {
                                                val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                                if (steps > 0) hasActivity = true
                                                totalSteps += steps
                                            }
                                            "com.google.calories.expended" -> {
                                                val calories = dataPoint.getValue(Field.FIELD_CALORIES).asFloat()
                                                if (calories > 0) hasActivity = true
                                                totalCalories += calories
                                            }
                                            "com.google.distance.delta" -> {
                                                val distance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat()
                                                totalDistance += distance
                                            }
                                            "com.google.speed.summary" -> {
                                                avgSpeed = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat()
                                            }
                                            "com.google.activity.segment" -> {
                                                activityType = when (dataPoint.getValue(Field.FIELD_ACTIVITY).asInt()) {
                                                    7 -> "Walking"
                                                    8 -> "Running"
                                                    1 -> "Biking"
                                                    else -> "Other"
                                                }
                                            }
                                        }
                                    }
                                }
                                if (hasActivity) {
                                    activeDays++
                                    streakCount++
                                    longestStreak = maxOf(longestStreak, streakCount)
                                } else {
                                    streakCount = 0
                                }
                            }

                            currentStreak = streakCount

                            val avgCaloriesPerDay = if (activeDays > 0) totalCalories / activeDays else 0f

                            val broadcastIntent = Intent("fitness_data_updated").apply {
                                `package` = packageName
                                putExtra("dailySteps", dailySteps)
                                putExtra("caloriesBurned", totalCalories.toInt())
                                putExtra("activeCalories", activeCalories.toInt())
                                putExtra("distance", totalDistance)
                                putExtra("speed", avgSpeed)
                                putExtra("activityType", activityType)

                                putExtra("totalSteps", totalSteps)
                                putExtra("totalDistance", totalDistance)
                                putExtra("totalCalories", totalCalories.toInt())
                                putExtra("activeDays", activeDays)
                                putExtra("avgStepsPerDay", if (activeDays > 0) totalSteps / activeDays else 0)
                                putExtra("avgCaloriesPerDay", avgCaloriesPerDay.toInt())

                                putExtra("totalStartTime", totalStartTime)
                                putExtra("totalEndTime", totalEndTime)
                                putExtra("longestStreak", longestStreak)
                                putExtra("currentStreak", currentStreak)
                                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                            }

                            Handler(Looper.getMainLooper()).post {
                                applicationContext.sendBroadcast(broadcastIntent)
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to read fitness data", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while reading fitness data", e)
        }
    }
}
