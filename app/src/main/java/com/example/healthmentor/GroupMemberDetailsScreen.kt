package com.example.healthmentor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthmentor.components.CommonBottomBar
import com.example.healthmentor.components.StepsChart
import com.example.healthmentor.components.SleepChart
import com.example.healthmentor.components.SimpleHeader
import com.example.healthmentor.components.DashboardSection
import com.example.healthmentor.components.MetricCard
import com.example.healthmentor.models.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupMemberDetailsScreen(
    navController: NavController,
    memberId: String,
    memberName: String
) {
    val db = FirebaseFirestore.getInstance()
    
    var userData by remember { mutableStateOf<UserProfile?>(null) }
    var stepData by remember { mutableStateOf<List<MemberStepCount>>(emptyList()) }
    var sleepData by remember { mutableStateOf<List<SleepData>>(emptyList()) }
    var caloriesData by remember { mutableStateOf<List<CaloriesData>>(emptyList()) }
    var distanceData by remember { mutableStateOf<List<DistanceData>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

    LaunchedEffect(memberId) {
        db.collection("users").document(memberId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userData = document.toObject(UserProfile::class.java)
                } else {
                    error = "Felhasználói adatok nem találhatók"
                }
            }
            .addOnFailureListener { e ->
                error = "Hiba a felhasználói adatok lekérdezésekor: ${e.message}"
                Log.e("GroupMemberDetails", "Error fetching user data", e)
            }
    }

    LaunchedEffect(memberId) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time

        db.collection("users").document(memberId)
            .collection("steps")
            .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val steps = documents.mapNotNull { it.toObject(MemberStepCount::class.java) }
                    stepData = steps
                    loading = false
                } else {
                    val sevenDaysAgo = Calendar.getInstance()
                    sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7)
                    
                    db.collection("stepCounts")
                        .whereEqualTo("userId", memberId)
                        .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo.timeInMillis)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { stepsSnapshot ->
                            val memberSteps = stepsSnapshot.documents.mapNotNull { doc ->
                                val stepCount = doc.toObject(StepCount::class.java)
                                stepCount?.let {
                                    val date = Calendar.getInstance()
                                    date.timeInMillis = it.timestamp
                                    MemberStepCount(
                                        userId = it.userId,
                                        date = Timestamp(Date(it.timestamp)),
                                        count = it.steps
                                    )
                                }
                            }
                            stepData = memberSteps
                            loading = false
                        }
                        .addOnFailureListener { e ->
                            error = "Hiba a lépések lekérdezésekor: ${e.message}"
                            loading = false
                            Log.e("GroupMemberDetails", "Error fetching steps from stepCounts", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                error = "Hiba a lépések lekérdezésekor: ${e.message}"
                loading = false
                Log.e("GroupMemberDetails", "Error fetching steps", e)
            }
    }

    LaunchedEffect(memberId) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time
        
        db.collection("users").document(memberId)
            .collection("sleep")
            .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val sleep = documents.mapNotNull { it.toObject(SleepData::class.java) }
                sleepData = sleep
            }
            .addOnFailureListener { e ->
                Log.e("GroupMemberDetails", "Error fetching sleep data", e)
            }
    }

    LaunchedEffect(memberId) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time
        
        db.collection("users").document(memberId)
            .collection("calories")
            .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val calories = documents.mapNotNull { it.toObject(CaloriesData::class.java) }
                caloriesData = calories
            }
            .addOnFailureListener { e ->
                Log.e("GroupMemberDetails", "Error fetching calories data", e)
            }
    }

    LaunchedEffect(memberId) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = calendar.time
        
        db.collection("users").document(memberId)
            .collection("distance")
            .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val distance = documents.mapNotNull { it.toObject(DistanceData::class.java) }
                distanceData = distance
            }
            .addOnFailureListener { e ->
                Log.e("GroupMemberDetails", "Error fetching distance data", e)
            }
    }

    val latestStepData = stepData.firstOrNull()
    val latestCaloriesData = caloriesData.firstOrNull()
    val latestDistanceData = distanceData.firstOrNull()
    val latestSleepData = sleepData.firstOrNull()

    val totalSteps = stepData.sumOf { it.count }
    val avgSteps = if (stepData.isNotEmpty()) totalSteps / stepData.size else 0
    val totalCalories = caloriesData.sumOf { it.calories }
    val avgCalories = if (caloriesData.isNotEmpty()) totalCalories / caloriesData.size else 0
    val totalDistance = distanceData.sumOf { it.distance }
    val avgDistance = if (distanceData.isNotEmpty()) totalDistance / distanceData.size else 0.0
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$memberName adatai") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Vissza")
                    }
                },
            )
        },
        bottomBar = {
            CommonBottomBar(navController, "activity")
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                Text(
                    text = error ?: "Ismeretlen hiba történt",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SimpleHeader(title = "${memberName} aktivitása")
                    Spacer(modifier = Modifier.height(16.dp))

                    DashboardSection(title = "Napi Lépések és Távolság") {
                        MetricCard(
                            title = "Legutóbbi lépésszám",
                            value = "${latestStepData?.count ?: 0}",
                            unit = "lépés"
                        )
                        MetricCard(
                            title = "Legutóbbi távolság",
                            value = String.format("%.2f", latestDistanceData?.distance ?: 0.0),
                            unit = "km"
                        )
                    }

                    if (stepData.isNotEmpty()) {
                        Text(
                            text = "Lépésszám változása",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        StepsChart(steps = stepData)
                    }

                    DashboardSection(title = "Kalória") {
                        MetricCard(
                            title = "Legutóbbi elégetett kalória",
                            value = "${latestCaloriesData?.calories ?: 0}",
                            unit = "kcal"
                        )
                    }

                    if (sleepData.isNotEmpty()) {
                        DashboardSection(title = "Alvás") {
                            MetricCard(
                                title = "Legutóbbi alvásidő",
                                value = "${latestSleepData?.durationMinutes?.div(60) ?: 0}",
                                unit = "óra"
                            )
                        }
                        
                        if (sleepData.size > 1) {
                            Text(
                                text = "Alvási idő változása",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            SleepChart(sleepData = sleepData)
                        }
                    }

                    DashboardSection(title = "Napi Átlagok") {
                        MetricCard(
                            title = "Átlag lépésszám",
                            value = "$avgSteps",
                            unit = "lépés/nap"
                        )
                        MetricCard(
                            title = "Átlag kalória",
                            value = "$avgCalories",
                            unit = "kcal/nap"
                        )
                        MetricCard(
                            title = "Átlag távolság",
                            value = String.format("%.1f", avgDistance),
                            unit = "km/nap"
                        )
                    }

                    DashboardSection(title = "Összes Aktivitás") {
                        MetricCard(
                            title = "Összes lépés",
                            value = "$totalSteps",
                            unit = "lépés"
                        )
                        MetricCard(
                            title = "Összes távolság",
                            value = String.format("%.1f", totalDistance),
                            unit = "km"
                        )
                        MetricCard(
                            title = "Összes kalória",
                            value = "$totalCalories",
                            unit = "kcal"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
} 