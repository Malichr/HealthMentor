package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AIAdviceScreen(navController: NavController) {
    Log.d("AIAdviceScreen", "AIAdviceScreen Composable started")

    var steps by remember { mutableStateOf(0) }
    var caloriesBurned by remember { mutableStateOf(0) }
    var distance by remember { mutableStateOf(0f) }
    var advice by remember { mutableStateOf("Loading AI advice...") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val broadcastReceiver = remember {
        HomeScreenBroadcastReceiver { updatedSteps, updatedCaloriesBurned, updatedDistance ->
            Log.d("AIAdviceScreen", "Broadcast received: steps=$updatedSteps, calories=$updatedCaloriesBurned, distance=$updatedDistance")
            steps = updatedSteps
            caloriesBurned = updatedCaloriesBurned
            distance = updatedDistance

            val prompt = "I have taken $steps steps, burned $caloriesBurned calories, and walked $distance kilometers today. Can you give me some advice on how to improve my health and fitness?"
            coroutineScope.launch {
                try {
                    val aiAdvice = AIAdviceService.getAIAdvice(prompt)
                    advice = aiAdvice
                } catch (e: Exception) {
                    advice = "Hiba történt: ${e.message}"
                }
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
        try {
            val aiAdvice = AIAdviceService.getAIAdvice(testPrompt)
            advice = aiAdvice
        } catch (e: Exception) {
            advice = "Hiba történt: ${e.message}"
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
                    Text(
                        text = "Mai fitnesz adatok",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("Lépések: $steps")
                    Text("Elégetett kalóriák: $caloriesBurned")
                    Text("Megtett távolság: $distance km")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI Tanácsadó",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = advice,
                        style = MaterialTheme.typography.body1
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}
