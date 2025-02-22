package com.example.healthmentor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.healthmentor.models.UserProfile

@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = "HealthMentor Icon",
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "HealthMentor",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Register an account",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(64.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Please fill out all fields."
                    } else if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                    } else {
                        val db = FirebaseFirestore.getInstance()
                        val usersCollection = db.collection("users")

                        usersCollection.whereEqualTo("username", username).get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    FirebaseAuth.getInstance()
                                        .createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val user = task.result?.user
                                                if (user != null) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                        createUserInFirestore(user.uid, email, username)
                                                        navController.navigate("home")
                                                    }, 1000)
                                                }
                                            } else {
                                                errorMessage = task.exception?.message ?: "Ismeretlen hiba történt"
                                                Log.e("RegisterScreen", "Hiba a regisztráció során", task.exception)
                                            }
                                        }
                                } else {
                                    errorMessage = "Username already exists."
                                }
                            }
                            .addOnFailureListener { e ->
                                errorMessage = e.message ?: "Unknown error"
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colors.error)
            }
        }
    }
}

private fun createUserInFirestore(userId: String, email: String, username: String) {
    val db = FirebaseFirestore.getInstance()
    val userProfile = mapOf(
        "userId" to userId,
        "email" to email,
        "username" to username,
        "displayName" to username,
        "friends" to emptyList<String>(),
        "friendRequests" to emptyList<String>()
    )

    db.collection("users")
        .document(userId)
        .set(userProfile)
        .addOnSuccessListener {
            Log.d("RegisterScreen", "Felhasználó sikeresen létrehozva: $userId")
        }
        .addOnFailureListener { e ->
            Log.e("RegisterScreen", "Hiba a felhasználó létrehozása közben", e)
            Log.e("RegisterScreen", "Hiba részletei: ${e.message}")
        }
}
