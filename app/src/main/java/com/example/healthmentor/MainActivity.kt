package com.example.healthmentor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.navigation.compose.rememberNavController
import com.example.healthmentor.ui.theme.HealthMentorTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var fitnessOptions: FitnessOptions
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 2001
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Initializing Fitness options and Google Sign In")
        
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Fitness.SCOPE_ACTIVITY_READ.toString()))
            .requestScopes(Scope(Fitness.SCOPE_BODY_READ.toString()))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            Log.d(TAG, "Found existing Google account: ${account.email}")
            checkGoogleFitPermissions(account)
        } else {
            Log.d(TAG, "No Google account found, initiating sign in")
            signInToGoogle()
        }

        setContent {
            HealthMentorTheme {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colors.background) {
                    Navigation(navController)
                }
            }
        }
    }

    private fun signInToGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun checkGoogleFitPermissions(account: GoogleSignInAccount) {
        Log.d(TAG, "Checking Google Fit permissions")
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            Log.d(TAG, "Requesting Google Fit permissions")
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            Log.d(TAG, "Already have Google Fit permissions")
            accessGoogleFit(account)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        when (requestCode) {
            RC_SIGN_IN -> {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "Google Sign in success: ${account.email}")
                    checkGoogleFitPermissions(account)
                } catch (e: ApiException) {
                    Log.e(TAG, "Google Sign in failed: ${e.statusCode}", e)
                }
            }
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    val account = GoogleSignIn.getLastSignedInAccount(this)
                    account?.let { 
                        Log.d(TAG, "Google Fit permissions granted")
                        accessGoogleFit(it) 
                    }
                } else {
                    Log.e(TAG, "Google Fit permissions denied")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val account = GoogleSignIn.getLastSignedInAccount(this)
                    account?.let { startFitnessDataService(it) }
                }
            }
        }
    }

    private fun startFitnessDataService(account: GoogleSignInAccount) {
        FitnessDataService.enqueueWork(this, account)
    }

    private fun accessGoogleFit(account: GoogleSignInAccount) {
        Log.d(TAG, "Accessing Google Fit")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Requesting Activity Recognition permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                PERMISSION_REQUEST_ACTIVITY_RECOGNITION
            )
        } else {
            Log.d(TAG, "Starting FitnessDataService")
            startFitnessDataService(account)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

