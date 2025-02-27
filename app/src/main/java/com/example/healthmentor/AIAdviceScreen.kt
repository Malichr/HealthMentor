package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.healthmentor.components.CommonBottomBar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AIAdviceScreen(navController: NavController) {
    var advice by remember { mutableStateOf("AI tanácsok betöltése...") }
    var fitnessData by remember { mutableStateOf(FitnessDataState()) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
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
                        avgStepsPerDay = intent.getIntExtra("avgStepsPerDay", 0),
                        avgCaloriesPerDay = intent.getIntExtra("avgCaloriesPerDay", 0),
                        activeDays = intent.getIntExtra("activeDays", 0),
                        longestStreak = intent.getIntExtra("longestStreak", 0),
                        currentStreak = intent.getIntExtra("currentStreak", 0)
                    )

                    coroutineScope.launch {
                        advice = AIAdviceService.getAIAdvice(fitnessData)
                    }
                }
            }
        }

        val filter = IntentFilter("fitness_data_updated")
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val aiAdvice = AIAdviceService.getAIAdvice(fitnessData)
                advice = aiAdvice
            } catch (e: Exception) {
                advice = "Hiba történt a tanácsok lekérése közben: ${e.message}"
            }
        }
    }

    @Composable
    fun parseMarkdownText(markdown: String, primaryColor: androidx.compose.ui.graphics.Color): AnnotatedString {
        return buildAnnotatedString {
            val lines = markdown.lines()
            var isInBold = false
            
            lines.forEach { line ->
                val text = line.trim()
                when {
                    text.isEmpty() -> {
                        append("\n")
                    }
                    text.contains("**") -> {
                        val parts = text.split("**")
                        parts.forEachIndexed { index, part ->
                            if (index % 2 == 1) {
                                withStyle(SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )) {
                                    append(part)
                                }
                            } else {
                                append(part)
                            }
                        }
                        append("\n")
                    }
                    text.startsWith("# ") -> {
                        withStyle(SpanStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )) {
                            append(text.substring(2))
                        }
                        append("\n\n")
                    }
                    text.startsWith("## ") -> {
                        withStyle(SpanStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )) {
                            append(text.substring(3))
                        }
                        append("\n\n")
                    }
                    text.startsWith("- ") -> {
                        append("• ${text.substring(2)}\n")
                    }
                    else -> {
                        append("$text\n")
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            CommonBottomBar(navController = navController, currentRoute = "ai_advice")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    val primaryColor = MaterialTheme.colors.primary
                    
                    Text(
                        text = "AI Tanács",
                        style = MaterialTheme.typography.h6.copy(
                            color = primaryColor
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val formattedText = parseMarkdownText(advice, primaryColor)
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}
