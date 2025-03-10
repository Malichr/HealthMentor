package com.example.healthmentor

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthmentor.components.CommonBottomBar
import com.example.healthmentor.models.Group
import com.example.healthmentor.models.UserProfile

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    group: Group,
    friends: List<UserProfile>,
    currentUserId: String,
    members: List<UserProfile>,
    onInvite: (String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onRemoveMember: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            ExpandableSection(
                title = "Tagok",
                badge = members.size
            ) {
                members.forEach { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = member.username,
                                    style = MaterialTheme.typography.subtitle1
                                )
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.caption
                                )
                                if (member.userId == group.ownerId) {
                                    Text(
                                        text = "(Admin)",
                                        style = MaterialTheme.typography.caption,
                                        color = MaterialTheme.colors.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            
                            if (currentUserId == group.ownerId && member.userId != currentUserId) {
                                IconButton(
                                    onClick = { onRemoveMember(member.userId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Tag törlése",
                                        tint = MaterialTheme.colors.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (currentUserId == group.ownerId) {
                val invitableFriends = friends.filter { friend ->
                    !group.members.contains(friend.userId) && 
                    !group.pendingInvites.contains(friend.userId)
                }
                
                if (invitableFriends.isNotEmpty()) {
                    ExpandableSection(
                        title = "Barát meghívása",
                        badge = invitableFriends.size
                    ) {
                        invitableFriends.forEach { friend ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = friend.username,
                                            style = MaterialTheme.typography.subtitle1
                                        )
                                        Text(
                                            text = friend.email,
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                    IconButton(
                                        onClick = { onInvite(friend.userId) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Barát meghívása",
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDelete()
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Csoport törlése", color = MaterialTheme.colors.onError)
                }
            } else {
                Button(
                    onClick = {
                        onLeave()
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kilépés a csoportból", color = MaterialTheme.colors.onError)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
} 