package com.example.healthmentor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.healthmentor.ui.theme.HealthMentorTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.tasks.Task
import org.json.JSONObject
import java.io.InputStream

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var fitnessOptions: FitnessOptions
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthMentorTheme {
                val navController = rememberNavController()
                Surface(color = MaterialTheme.colors.background) {
                    Navigation(navController)
                }
            }
        }

        initializeFitnessOptions()
        initializeGoogleSignInClient()
    }

    private fun initializeFitnessOptions() {
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }

    private fun initializeGoogleSignInClient() {
        val clientId = getClientIdFromJson()
        Log.d(TAG, "Client ID: $clientId")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(clientId)
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        signInToGoogle()
    }

    private fun getClientIdFromJson(): String {
        return try {
            val inputStream: InputStream = resources.openRawResource(R.raw.client_secret_898233716746_a0gvo12io55j8c17p0eb78ekqsqoq9e8_apps_googleusercontent_com)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            jsonObject.getJSONObject("installed").getString("client_id")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading client ID from JSON", e)
            ""
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "SignInLauncher result: $result")
        if (result.resultCode == RESULT_OK) {
            val task: Task<GoogleSignInAccount>? = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.e(TAG, "Google Sign-In failed with resultCode: ${result.resultCode}. Result: $result")
            Log.d(TAG, "Intent data: ${result.data}")
            result.data?.extras?.let { extras ->
                val status = extras.get("googleSignInStatus")
                Log.d(TAG, "Intent extras: googleSignInStatus = $status")
            }
        }
    }


    private fun signInToGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                account?.let {
                    startFitnessDataService(it)
                }
            } else {
                Log.e(TAG, "Google Fit permissions denied")
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>?) {
        try {
            val account = completedTask?.getResult(ApiException::class.java)
            Log.d(TAG, "signInResult:success account=${account?.email}")
            account?.let {
                requestGoogleFitPermissions(it)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Log.w(TAG, "signInResult:failed message=" + e.message)
        }
    }

    private fun requestGoogleFitPermissions(account: GoogleSignInAccount) {
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            startFitnessDataService(account)
        }
    }

    private fun startFitnessDataService(account: GoogleSignInAccount) {
        FitnessDataService.enqueueWork(this, account)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
