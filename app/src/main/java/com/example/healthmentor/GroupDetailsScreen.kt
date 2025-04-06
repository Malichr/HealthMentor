package com.example.healthmentor

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.healthmentor.components.CommonBottomBar
import com.example.healthmentor.components.ExpandableSection
import com.example.healthmentor.components.InviteFriendsDialog
import com.example.healthmentor.models.Group
import com.example.healthmentor.models.UserProfile

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun GroupDetailsScreen(
    navController: NavController,
    group: Group,
    friends: List<UserProfile>,
    currentUserId: String,
    members: List<UserProfile>,
    pendingInviteFriends: List<UserProfile>,
    onInvite: (String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onCancelInvite: (String) -> Unit
) {
    var showInviteFriendsDialog by remember { mutableStateOf(false) }
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
        },
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 6.dp, end = 2.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate("group_statistics/${group.id}")
                        },
                        backgroundColor = MaterialTheme.colors.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Csoport statisztikák",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                    
                    if (currentUserId == group.ownerId) {
                        FloatingActionButton(
                            onClick = {
                                onDelete()
                                navController.navigateUp()
                            },
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Csoport törlése",
                                tint = MaterialTheme.colors.onError
                            )
                        }
                    } else {
                        FloatingActionButton(
                            onClick = {
                                onLeave()
                                navController.navigateUp()
                            },
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Kilépés a csoportból",
                                tint = MaterialTheme.colors.onError
                            )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
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
                if (members.isNotEmpty()) {
                    Column {
                        members.forEach { member ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        navController.navigate("group_member_details/${member.userId}/${member.username}")
                                    },
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
                } else {
                    Text(
                        text = "Nincsenek tagok a csoportban",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (group.pendingInvites.isNotEmpty() && currentUserId == group.ownerId) {
                Spacer(modifier = Modifier.height(16.dp))
                ExpandableSection(
                    title = "Függőben lévő meghívások",
                    badge = group.pendingInvites.size
                ) {
                    if (pendingInviteFriends.isNotEmpty()) {
                        Column {
                            pendingInviteFriends.forEach { friend ->
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
                                            onClick = { onCancelInvite(friend.userId) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Meghívás visszavonása",
                                                tint = MaterialTheme.colors.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Nincsenek megjeleníthető meghívások",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            if (currentUserId == group.ownerId) {
                val invitableFriends = friends.filter { friend ->
                    !group.members.contains(friend.userId) && 
                    !group.pendingInvites.contains(friend.userId)
                }
                
                if (invitableFriends.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpandableSection(
                        title = "Barát meghívása",
                        badge = invitableFriends.size
                    ) {
                        if (invitableFriends.isNotEmpty()) {
                            Column {
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
                        } else {
                            Text(
                                text = "Nincs meghívható barát",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (currentUserId != group.ownerId) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showInviteFriendsDialog) {
        InviteFriendsDialog(
            friends = friends,
            members = members,
            pendingInvites = group.pendingInvites,
            onInvite = { friendId ->
                onInvite(friendId)
                showInviteFriendsDialog = false
            },
            onDismiss = { showInviteFriendsDialog = false }
        )
    }
} 