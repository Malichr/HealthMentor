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

        val account = intent.getParcelableExtra<GoogleSignInAccount>(EXTRA_ACCOUNT)
        if (account == null) {
            Log.e(TAG, "No account provided to FitnessDataService")
            return
        }

        Log.d(TAG, "Account email: ${account.email}")

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        Log.d(TAG, "Requesting fitness data from ${Date(startTime)} to ${Date(endTime)}")

        val datasetsRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
            .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        try {
            Fitness.getHistoryClient(this, account)
                .readData(datasetsRequest)
                .addOnSuccessListener { response ->
                    Log.d(TAG, "Successfully read fitness data")
                    var totalSteps = 0
                    var totalCalories = 0.0f
                    var totalDistance = 0.0f

                    for (bucket in response.buckets) {
                        for (dataset in bucket.dataSets) {
                            Log.d(TAG, "Dataset type: ${dataset.dataType.name}")
                            for (dataPoint in dataset.dataPoints) {
                                when (dataset.dataType.name) {
                                    "com.google.step_count.delta" -> {
                                        val steps = dataPoint.getValue(Field.FIELD_STEPS).asInt()
                                        totalSteps += steps
                                        Log.d(TAG, "Field steps: $steps")
                                    }
                                    "com.google.calories.expended" -> {
                                        val calories = dataPoint.getValue(Field.FIELD_CALORIES).asFloat()
                                        totalCalories += calories
                                        Log.d(TAG, "Field calories: $calories")
                                    }
                                    "com.google.distance.delta" -> {
                                        val distance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat()
                                        totalDistance += distance
                                        Log.d(TAG, "Field distance: $distance")
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Total steps: $totalSteps")
                    Log.d(TAG, "Total calories: $totalCalories")
                    Log.d(TAG, "Total distance: $totalDistance")

                    sendBroadcastIntent(totalSteps, totalCalories.toInt(), totalDistance)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to read fitness data", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while reading fitness data", e)
        }
    }

    private fun sendBroadcastIntent(steps: Int, caloriesBurned: Int, distance: Float) {
        val intent = Intent("fitness_data_updated").apply {
            `package` = packageName
            putExtra("steps", steps)
            putExtra("caloriesBurned", caloriesBurned)
            putExtra("distance", distance)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        
        Handler(Looper.getMainLooper()).post {
            Log.d(TAG, "Sending broadcast with steps: $steps, calories: $caloriesBurned, distance: $distance")
            applicationContext.sendBroadcast(intent)
        }
    }
}
