package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AIAdviceScreen(navController: NavController) {
    Log.d("AIAdviceScreen", "AIAdviceScreen Composable started")

    var steps by remember { mutableStateOf(0) }
    var caloriesBurned by remember { mutableStateOf(0) }
    var distance by remember { mutableStateOf(0f) }
    var advice by remember { mutableStateOf("Loading AI advice...") }

    val context = LocalContext.current

    val broadcastReceiver = remember {
        HomeScreenBroadcastReceiver { updatedSteps, updatedCaloriesBurned, updatedDistance ->
            Log.d("AIAdviceScreen", "Broadcast received: steps=$updatedSteps, calories=$updatedCaloriesBurned, distance=$updatedDistance")
            steps = updatedSteps
            caloriesBurned = updatedCaloriesBurned
            distance = updatedDistance

            val prompt = "I have taken $steps steps, burned $caloriesBurned calories, and walked $distance kilometers today. Can you give me some advice on how to improve my health and fitness?"
            AIAdviceService.getAIAdvice(prompt) { aiAdvice ->
                Log.d("AIAdviceScreen", "Received AI advice: $aiAdvice")
                advice = aiAdvice
            }
        }
    }

    LaunchedEffect(Unit) {
        val intentFilter = IntentFilter("fitness_data_updated")
        LocalBroadcastManager.getInstance(context).registerReceiver(
            broadcastReceiver,
            intentFilter
        )
        Log.d("AIAdviceScreen", "BroadcastReceiver registered")

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            Log.d("AIAdviceScreen", "Google account found, requesting fitness data")
            requestFitnessData(context, account)
        } else {
            Log.d("AIAdviceScreen", "No Google account found")
        }

        val testPrompt = "Give me advice on how to improve my health and fitness."
        AIAdviceService.getAIAdvice(testPrompt) { aiAdvice ->
            Log.d("AIAdviceScreen", "Test AI advice: $aiAdvice")
            advice = aiAdvice
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigation {
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = false,
                    onClick = { navController.navigate("home") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("AI advice") },
                    selected = true,
                    onClick = { navController.navigate("ai_advice") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Challenges") },
                    selected = false,
                    onClick = { navController.navigate("challenges") }
                )
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("AI Advice Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Text(advice)
        }
    }
}
