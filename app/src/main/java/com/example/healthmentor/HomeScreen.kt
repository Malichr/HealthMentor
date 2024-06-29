package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HomeScreen(navController: NavController) {
    var steps by remember { mutableStateOf(0) }
    var caloriesBurned by remember { mutableStateOf(0) }
    var distance by remember { mutableStateOf(0f) }

    val broadcastReceiver = remember {
        HomeScreenBroadcastReceiver { updatedSteps, updatedCaloriesBurned, updatedDistance ->
            steps = updatedSteps
            caloriesBurned = updatedCaloriesBurned
            distance = updatedDistance
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intentFilter = IntentFilter("fitness_data_updated")
        LocalBroadcastManager.getInstance(context).registerReceiver(
            broadcastReceiver,
            intentFilter
        )

        val account = GoogleSignIn.getLastSignedInAccount(context)
        account?.let {
            requestFitnessData(context, it)
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
                    selected = false,
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
            FitnessDataCard(steps, caloriesBurned, distance)
        }
    }
}

@Composable
fun FitnessDataCard(steps: Int, caloriesBurned: Int, distance: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Fitness Data")
            Text("Steps: $steps")
            Text("Calories Burned: $caloriesBurned")
            Text("Distance: ${distance} km")
        }
    }
}

public fun requestFitnessData(context: Context, account: GoogleSignInAccount) {
    val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    FitnessDataService.enqueueWork(context, account)
}
