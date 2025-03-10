package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.example.healthmentor.components.CommonBottomBar
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.util.concurrent.TimeUnit

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ActivityScreen(navController: NavController) {
    var fitnessData by remember { mutableStateOf(FitnessDataState()) }
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("ActivityScreen", "Received fitness data broadcast")
                if (intent.action == "fitness_data_updated") {
                    fitnessData = fitnessData.copy(
                        dailySteps = intent.getIntExtra("dailySteps", 0),
                        dailyDistance = intent.getFloatExtra("distance", 0f),
                        dailyCalories = intent.getIntExtra("caloriesBurned", 0),
                        dailyActiveCalories = intent.getIntExtra("activeCalories", 0),
                        speed = intent.getFloatExtra("speed", 0f),

                        totalSteps = intent.getIntExtra("totalSteps", 0),
                        totalDistance = intent.getFloatExtra("totalDistance", 0f),
                        totalCalories = intent.getIntExtra("totalCalories", 0),
                        totalActiveCalories = intent.getIntExtra("totalActiveCalories", 0),
                        avgStepsPerDay = intent.getIntExtra("avgStepsPerDay", 0),
                        avgCaloriesPerDay = intent.getIntExtra("avgCaloriesPerDay", 0),
                        activeDays = intent.getIntExtra("activeDays", 0),
                        longestStreak = intent.getIntExtra("longestStreak", 0),
                        currentStreak = intent.getIntExtra("currentStreak", 0),

                        totalStartTime = intent.getLongExtra("totalStartTime", 0L),
                        totalEndTime = intent.getLongExtra("totalEndTime", 0L)
                    )
                    Log.d("ActivityScreen", "Updated fitness data: $fitnessData")
                }
            }
        }

        val filter = IntentFilter("fitness_data_updated").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        val account = GoogleSignIn.getLastSignedInAccount(context)
        account?.let {
            requestFitnessData(context, it)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        bottomBar = {
            CommonBottomBar(navController = navController, currentRoute = "activity")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            DashboardHeader(title = "Mai Aktivitás")
            Spacer(modifier = Modifier.height(16.dp))
            
            DashboardSection(title = "Mai Lépések és Távolság") {
                MetricCard(
                    title = "Napi lépésszám",
                    value = "${fitnessData.dailySteps}",
                    unit = "lépés"
                )
                MetricCard(
                    title = "Mai távolság",
                    value = String.format("%.2f", fitnessData.dailyDistance),
                    unit = "m"
                )
                MetricCard(
                    title = "Átlagsebesség",
                    value = String.format("%.1f", fitnessData.speed * 3.6),
                    unit = "km/h"
                )
            }

            DashboardSection(title = "Mai Kalória") {
                MetricCard(
                    title = "Elégetett kalória",
                    value = "${fitnessData.dailyCalories}",
                    unit = "kcal"
                )
                MetricCard(
                    title = "Aktív kalória",
                    value = "${fitnessData.dailyActiveCalories}",
                    unit = "kcal"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            StatisticsHeader(fitnessData = fitnessData)
            Spacer(modifier = Modifier.height(16.dp))

            DashboardSection(title = "Napi Átlagok") {
                MetricCard(
                    title = "Átlag lépésszám",
                    value = "${fitnessData.avgStepsPerDay}",
                    unit = "lépés/nap"
                )
                MetricCard(
                    title = "Átlag kalória",
                    value = "${fitnessData.avgCaloriesPerDay}",
                    unit = "kcal/nap"
                )
                MetricCard(
                    title = "Átlag távolság",
                    value = String.format("%.1f", 
                        if (fitnessData.activeDays > 0) 
                            fitnessData.totalDistance / fitnessData.activeDays 
                        else 0f),
                    unit = "km/nap"
                )
            }

            DashboardSection(title = "Összes Aktivitás") {
                MetricCard(
                    title = "Összes lépés",
                    value = "${fitnessData.totalSteps}",
                    unit = "lépés"
                )
                MetricCard(
                    title = "Összes távolság",
                    value = String.format("%.1f", fitnessData.totalDistance / 1000),
                    unit = "km"
                )
                MetricCard(
                    title = "Összes kalória",
                    value = "${fitnessData.totalCalories}",
                    unit = "kcal"
                )
            }

            DashboardSection(title = "Aktivitás Statisztika") {
                MetricCard(
                    title = "Aktív napok",
                    value = "${fitnessData.activeDays}",
                    unit = "nap"
                )
                MetricCard(
                    title = "Aktív napok aránya",
                    value = String.format("%.1f", 
                        if (fitnessData.totalEndTime > fitnessData.totalStartTime)
                            fitnessData.activeDays * 100f / 
                            ((fitnessData.totalEndTime - fitnessData.totalStartTime) / 
                            TimeUnit.DAYS.toMillis(1))
                        else 0f),
                    unit = "%"
                )
                MetricCard(
                    title = "Leghosszabb sorozat",
                    value = "${fitnessData.longestStreak}",
                    unit = "nap"
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun DashboardHeader(title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StatisticsHeader(fitnessData: FitnessDataState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Összesített Statisztika",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.primary
        )
        if (fitnessData.totalStartTime > 0) {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val startDate = Date(fitnessData.totalStartTime)
            val endDate = Date(fitnessData.totalEndTime)
            Text(
                text = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DashboardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%d:%02d", hours, mins)
}

data class FitnessDataState(
    val dailySteps: Int = 0,
    val dailyDistance: Float = 0f,
    val dailyCalories: Int = 0,
    val dailyActiveCalories: Int = 0,
    val speed: Float = 0f,

    val totalSteps: Int = 0,
    val totalDistance: Float = 0f,
    val totalCalories: Int = 0,
    val totalActiveCalories: Int = 0,
    val avgStepsPerDay: Int = 0,
    val avgCaloriesPerDay: Int = 0,
    val activeDays: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,

    val totalStartTime: Long = 0L,
    val totalEndTime: Long = 0L
)

public fun requestFitnessData(context: Context, account: GoogleSignInAccount) {
    val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    FitnessDataService.enqueueWork(context, account)
}
