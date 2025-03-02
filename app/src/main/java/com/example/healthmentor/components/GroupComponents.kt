package com.example.healthmentor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthmentor.models.Group
import com.example.healthmentor.models.GroupInvite
import com.example.healthmentor.models.UserProfile

@Composable
fun CreateGroupDialog(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új csoport létrehozása") },
        text = {
            TextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Csoport neve") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = groupName.isNotBlank()
            ) {
                Text("Létrehozás")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )
}

@Composable
fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "Tagok: ${group.members.size}",
                style = MaterialTheme.typography.body1
            )
            if (group.pendingInvites.isNotEmpty()) {
                Text(
                    text = "Függő meghívások: ${group.pendingInvites.size}",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Composable
fun GroupDetailsDialog(
    group: Group,
    friends: List<UserProfile>,
    currentUserId: String,
    members: List<UserProfile>,
    memberSteps: List<Pair<UserProfile, Int>>,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.name) },
        text = {
            Column {
                Text(
                    "Mai rangsor:",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val sortedMembers = memberSteps.sortedByDescending { it.second }
                val maxSteps = sortedMembers.firstOrNull()?.second ?: 0

                sortedMembers.forEachIndexed { index, (member, steps) -> 
                    val progress = if (maxSteps > 0) steps.toFloat() / maxSteps else 0f
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .background(MaterialTheme.colors.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(24.dp)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFFFFD700)
                                            1 -> Color(0xFFC0C0C0)
                                            2 -> Color(0xFFCD7F32)
                                            else -> MaterialTheme.colors.primary.copy(alpha = 0.7f)
                                        }
                                    )
                            )
                            Text(
                                text = "$steps lépés",
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp),
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                        
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = member.username,
                                    style = MaterialTheme.typography.subtitle1
                                )
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.caption
                                )
                            }
                            if (member.userId == group.ownerId) {
                                Text(
                                    text = "(Admin)",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    
                    if (index < sortedMembers.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (currentUserId == group.ownerId) {
                    Text("Barát meghívása:", style = MaterialTheme.typography.h6)
                    friends.forEach { friend ->
                        if (!group.members.contains(friend.userId) && 
                            !group.pendingInvites.contains(friend.userId)) {
                            Button(
                                onClick = { onInvite(friend.userId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(friend.username)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentUserId == group.ownerId) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Csoport törlése")
                    }
                } else {
                    Button(
                        onClick = onLeave,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kilépés a csoportból")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Bezárás")
            }
        }
    )
}

@Composable
fun GroupInvitesSection(
    invites: List<GroupInvite>,
    onResponse: (GroupInvite, Boolean) -> Unit
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
                text = "Csoport meghívások",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            invites.forEach { invite ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(invite.groupName)
                        Text(
                            "Meghívó: ${invite.fromUserEmail}",
                            style = MaterialTheme.typography.caption
                        )
                    }
                    Row {
                        Button(
                            onClick = { onResponse(invite, true) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Text("Elfogad")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onResponse(invite, false) },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.error
                            )
                        ) {
                            Text("Elutasít")
                        }
                    }
                }
                if (invites.last() != invite) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
} 