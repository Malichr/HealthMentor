package com.example.healthmentor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CommonBottomBar(navController: NavController, currentRoute: String) {
    var pendingFriendRequests by remember { mutableStateOf(0) }
    var pendingGroupInvites by remember { mutableStateOf(0) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("friendRequests")
                .whereEqualTo("toUserId", currentUser.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    pendingFriendRequests = snapshot?.documents?.size ?: 0
                }

            db.collection("groupInvites")
                .whereEqualTo("toUserId", currentUser.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }
                    pendingGroupInvites = snapshot?.documents?.size ?: 0
                }
        }
    }

    BottomNavigation(
        modifier = Modifier.height(56.dp)
    ) {
        BottomNavigationItem(
            icon = { 
                Icon(
                    Icons.Default.DirectionsRun, 
                    contentDescription = "Aktivitás"
                ) 
            },
            label = { 
                Text(
                    "Aktivitás",
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            selected = currentRoute == "activity",
            onClick = { 
                if (currentRoute != "activity") {
                    navController.navigate("activity")
                }
            },
            alwaysShowLabel = true,
            modifier = Modifier.padding(0.dp)
        )
        
        BottomNavigationItem(
            icon = { 
                Icon(
                    Icons.Default.Info, 
                    contentDescription = "AI Tanács"
                ) 
            },
            label = { 
                Text(
                    "AI Tanács",
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            selected = currentRoute == "ai_advice",
            onClick = { 
                if (currentRoute != "ai_advice") {
                    navController.navigate("ai_advice")
                }
            },
            alwaysShowLabel = true,
            modifier = Modifier.padding(0.dp)
        )
        
        BottomNavigationItem(
            icon = {
                Box {
                    Icon(
                        Icons.Default.Group, 
                        contentDescription = "Kihívások"
                    )
                    if (pendingGroupInvites > 0) {
                        Badge(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = pendingGroupInvites.toString(),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(0.dp)
                            )
                        }
                    }
                }
            },
            label = { 
                Text(
                    "Kihívások",
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            selected = currentRoute == "challenges",
            onClick = { 
                if (currentRoute != "challenges") {
                    navController.navigate("challenges")
                }
            },
            modifier = Modifier.padding(0.dp)
        )
        
        BottomNavigationItem(
            icon = {
                Box {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = "Barátok"
                    )
                    if (pendingFriendRequests > 0) {
                        Badge(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = pendingFriendRequests.toString(),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(0.dp)
                            )
                        }
                    }
                }
            },
            label = { 
                Text(
                    "Barátok",
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) 
            },
            selected = currentRoute == "friends",
            onClick = { 
                if (currentRoute != "friends") {
                    navController.navigate("friends")
                }
            },
            modifier = Modifier.padding(0.dp)
        )
        
        BottomNavigationItem(
            icon = { 
                Icon(
                    Icons.Default.Logout, 
                    contentDescription = "Kijelentkezés"
                ) 
            },
            label = { 
                Text(
                    "Kijelentkezés",
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                ) 
            },
            selected = false,
            onClick = { 
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            },
            alwaysShowLabel = true,
            modifier = Modifier.padding(0.dp)
        )
    }
} 