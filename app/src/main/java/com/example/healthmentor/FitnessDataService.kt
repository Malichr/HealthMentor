package com.example.healthmentor

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.concurrent.TimeUnit

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
        Log.d(TAG, "FitnessDataService is handling intent")

        val account = intent.getParcelableExtra<GoogleSignInAccount>(EXTRA_ACCOUNT)
        account?.let { googleAccount ->
            val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .build()

            val now = System.currentTimeMillis()

            // Request data for the past day
            val endTime = now
            val startTime = endTime - TimeUnit.DAYS.toMillis(1)

            val request = DataReadRequest.Builder()
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .read(DataType.TYPE_CALORIES_EXPENDED)
                .read(DataType.TYPE_DISTANCE_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            Fitness.getHistoryClient(this, googleAccount)
                .readData(request)
                .addOnSuccessListener { response ->
                    Log.d(TAG, "Fitness data read successfully")

                    var steps = 0
                    var caloriesBurned = 0f
                    var distance = 0f

                    for (dataSet in response.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            for (field in dataPoint.dataType.fields) {
                                when (field.name) {
                                    Field.FIELD_STEPS.name -> steps += dataPoint.getValue(field).asInt()
                                    Field.FIELD_CALORIES.name -> caloriesBurned += dataPoint.getValue(field).asFloat()
                                    Field.FIELD_DISTANCE.name -> distance += dataPoint.getValue(field).asFloat()
                                }
                            }
                        }
                    }

                    sendBroadcastIntent(steps, caloriesBurned.toInt(), distance)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to read fitness data", e)
                }
        } ?: run {
            Log.e(TAG, "GoogleSignInAccount not found in intent extras")
        }
    }

    private fun sendBroadcastIntent(steps: Int, caloriesBurned: Int, distance: Float) {
        val intent = Intent("fitness_data_updated")
        intent.putExtra("steps", steps)
        intent.putExtra("caloriesBurned", caloriesBurned)
        intent.putExtra("distance", distance)
        sendBroadcast(intent)
    }
}
