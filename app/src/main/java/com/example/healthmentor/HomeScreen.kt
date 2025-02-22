package com.example.healthmentor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.auth.FirebaseAuth
import com.example.healthmentor.components.CommonBottomBar

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HomeScreen(navController: NavController) {
    var steps by remember { mutableStateOf(0) }
    var calories by remember { mutableStateOf(0) }
    var distance by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("HomeScreen", "Received fitness data broadcast")
                if (intent.action == "fitness_data_updated") {
                    val newSteps = intent.getIntExtra("steps", 0)
                    val newCalories = intent.getIntExtra("caloriesBurned", 0)
                    val newDistance = intent.getFloatExtra("distance", 0f)
                    
                    Log.d("HomeScreen", "Received values - Steps: $newSteps, Calories: $newCalories, Distance: $newDistance")
                    
                    // Főszálon frissítjük az értékeket
                    Handler(Looper.getMainLooper()).post {
                        steps = newSteps
                        calories = newCalories
                        distance = newDistance
                        Log.d("HomeScreen", "Updated UI values - Steps: $steps, Calories: $calories, Distance: $distance")
                    }
                }
            }
        }

        val filter = IntentFilter("fitness_data_updated").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        
        Log.d("HomeScreen", "Registering broadcast receiver")
        context.registerReceiver(
            receiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        // Indítsuk el a fitness adatok lekérését
        val account = GoogleSignIn.getLastSignedInAccount(context)
        account?.let {
            Log.d("HomeScreen", "Starting fitness data request")
            requestFitnessData(context, it)
        }

        onDispose {
            Log.d("HomeScreen", "Unregistering broadcast receiver")
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        bottomBar = {
            CommonBottomBar(navController = navController, currentRoute = "home")
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            FitnessDataCard(steps, calories, distance)
        }
    }
}

@Composable
fun FitnessDataCard(steps: Int, calories: Int, distance: Float) {
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
            Text("Calories Burned: $calories")
            Text("Distance: ${String.format("%.2f", distance)} m")
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
