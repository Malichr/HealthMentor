package com.example.healthmentor

import android.annotation.SuppressLint
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
import com.example.healthmentor.models.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupStatisticsScreen(
    navController: NavController,
    groupId: String
) {
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    var group by remember { mutableStateOf<Group?>(null) }
    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var memberStepData by remember { mutableStateOf<Map<String, List<MemberStepCount>>>(emptyMap()) }
    var memberDistanceData by remember { mutableStateOf<Map<String, List<DistanceData>>>(emptyMap()) }
    var memberCaloriesData by remember { mutableStateOf<Map<String, List<CaloriesData>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        db.collection("groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    group = document.toObject(Group::class.java)

                    if (group != null && group!!.members.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("userId", group!!.members)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                members = querySnapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }

                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DAY_OF_YEAR, -7)
                                val weekAgo = calendar.time

                                val tempStepData = mutableMapOf<String, List<MemberStepCount>>()
                                val tempDistanceData = mutableMapOf<String, List<DistanceData>>()
                                val tempCaloriesData = mutableMapOf<String, List<CaloriesData>>()
                                
                                var completedQueries = 0
                                val totalQueries = members.size * 3
                                
                                members.forEach { member ->
                                    db.collection("users").document(member.userId)
                                        .collection("steps")
                                        .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
                                        .orderBy("date", Query.Direction.ASCENDING)
                                        .get()
                                        .addOnSuccessListener { stepSnapshot ->
                                            val steps = stepSnapshot.documents.mapNotNull { 
                                                it.toObject(MemberStepCount::class.java) 
                                            }
                                            tempStepData[member.userId] = steps
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                                memberStepData = tempStepData
                                                memberDistanceData = tempDistanceData
                                                memberCaloriesData = tempCaloriesData
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("GroupStatistics", "Error fetching step data for ${member.username}", e)
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                            }
                                        }

                                    db.collection("users").document(member.userId)
                                        .collection("distance")
                                        .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
                                        .orderBy("date", Query.Direction.ASCENDING)
                                        .get()
                                        .addOnSuccessListener { distanceSnapshot ->
                                            val distances = distanceSnapshot.documents.mapNotNull { 
                                                it.toObject(DistanceData::class.java) 
                                            }
                                            tempDistanceData[member.userId] = distances
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                                memberStepData = tempStepData
                                                memberDistanceData = tempDistanceData
                                                memberCaloriesData = tempCaloriesData
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("GroupStatistics", "Error fetching distance data for ${member.username}", e)
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                            }
                                        }

                                    db.collection("users").document(member.userId)
                                        .collection("calories")
                                        .whereGreaterThanOrEqualTo("date", Timestamp(weekAgo))
                                        .orderBy("date", Query.Direction.ASCENDING)
                                        .get()
                                        .addOnSuccessListener { caloriesSnapshot ->
                                            val calories = caloriesSnapshot.documents.mapNotNull { 
                                                it.toObject(CaloriesData::class.java) 
                                            }
                                            tempCaloriesData[member.userId] = calories
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                                memberStepData = tempStepData
                                                memberDistanceData = tempDistanceData
                                                memberCaloriesData = tempCaloriesData
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("GroupStatistics", "Error fetching calories data for ${member.username}", e)
                                            completedQueries++
                                            if (completedQueries == totalQueries) {
                                                loading = false
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                error = "Nem sikerült lekérni a csoport tagjait: ${e.message}"
                                loading = false
                            }
                    } else {
                        error = "A csoportnak nincsenek tagjai"
                        loading = false
                    }
                } else {
                    error = "A csoport nem található"
                    loading = false
                }
            }
            .addOnFailureListener { e ->
                error = "Nem sikerült lekérni a csoportot: ${e.message}"
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Csoport statisztikák") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Vissza")
                    }
                }
            )
        },
        bottomBar = {
            CommonBottomBar(navController = navController, currentRoute = "challenges")
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
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "${group?.name ?: "Csoport"} statisztikák",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    ChartSection(
                        title = "Napi lépésszám",
                        memberData = memberStepData,
                        members = members,
                        valueSelector = { it.count.toFloat() },
                        unit = "lépés"
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    ChartSection(
                        title = "Napi távolság",
                        memberData = memberDistanceData,
                        members = members,
                        valueSelector = { it.distance.toFloat() },
                        unit = "km"
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    ChartSection(
                        title = "Napi elégetett kalória",
                        memberData = memberCaloriesData,
                        members = members,
                        valueSelector = { it.calories.toFloat() },
                        unit = "kcal"
                    )
                    
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun <T: Any> ChartSection(
    title: String,
    memberData: Map<String, List<T>>,
    members: List<UserProfile>,
    valueSelector: (T) -> Float,
    unit: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    if (memberData.isEmpty() || members.isEmpty()) {
        Text(
            text = "Nincs elegendő adat a grafikon megjelenítéséhez",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(vertical = 8.dp)
    ) {
        val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())

        val allDates = mutableSetOf<Long>()
        memberData.values.forEach { dataList ->
            dataList.forEach { data ->
                when (data) {
                    is MemberStepCount -> allDates.add(data.date.seconds)
                    is DistanceData -> allDates.add(data.date.seconds)
                    is CaloriesData -> allDates.add(data.date.seconds)
                }
            }
        }

        val sortedDates = allDates.sorted()

        val allEntries = members.mapNotNull { member ->
            val data = memberData[member.userId] ?: return@mapNotNull null

            val entries = data.map { item ->
                val date = when (item) {
                    is MemberStepCount -> item.date.seconds
                    is DistanceData -> item.date.seconds
                    is CaloriesData -> item.date.seconds
                    else -> 0L
                }
                val index = sortedDates.indexOf(date).toFloat()
                val value = valueSelector(item)
                entryOf(index, value)
            }
            
            if (entries.isEmpty()) null else entries
        }
        
        if (allEntries.isEmpty()) {
            Text(
                text = "Nincs elegendő adat a grafikon megjelenítéséhez",
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return
        }
        
        val entryModel = entryModelOf(*allEntries.toTypedArray())
        
        val axisValueFormatter = remember {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                val index = value.toInt()
                if (index >= 0 && index < sortedDates.size) {
                    val date = Date(sortedDates[index] * 1000)
                    dateFormat.format(date)
                } else ""
            }
        }
        
        val verticalAxisFormatter = remember {
            AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                "${value.toInt()} $unit"
            }
        }

        val lines = members.mapIndexed { index, member ->
            val colors = listOf(
                MaterialTheme.colors.primary,
                MaterialTheme.colors.secondary,
                Color(0xFF388E3C),
                Color(0xFFF57C00),
                Color(0xFF7B1FA2),
                Color(0xFF1976D2),
                Color(0xFFD32F2F)
            )
            val color = colors[index % colors.size]
            
            lineSpec(
                lineColor = color,
                lineThickness = 2.dp
            )
        }
        
        Chart(
            chart = lineChart(lines = lines),
            model = entryModel,
            startAxis = startAxis(valueFormatter = verticalAxisFormatter),
            bottomAxis = bottomAxis(valueFormatter = axisValueFormatter),
            modifier = Modifier.fillMaxSize()
        )
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Jelmagyarázat:",
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        members.forEachIndexed { index, member ->
            val colors = listOf(
                MaterialTheme.colors.primary,
                MaterialTheme.colors.secondary,
                Color(0xFF388E3C),
                Color(0xFFF57C00),
                Color(0xFF7B1FA2),
                Color(0xFF1976D2),
                Color(0xFFD32F2F)
            )
            val color = colors[index % colors.size]
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Divider(
                    color = color,
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = member.username,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
} 