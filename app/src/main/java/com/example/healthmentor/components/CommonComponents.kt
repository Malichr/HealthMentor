package com.example.healthmentor.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthmentor.models.UserProfile

@Composable
fun ExpandableSection(
    title: String,
    badge: Int? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
                badge?.let {
                    if (it > 0) {
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colors.primary,
                            shape = CircleShape
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = it.toString(),
                                    color = MaterialTheme.colors.onPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Összecsukás" else "Kinyitás",
                modifier = Modifier.rotate(rotationState)
            )
        }
        if (expanded) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun InviteFriendsDialog(
    friends: List<UserProfile>,
    members: List<UserProfile>,
    pendingInvites: List<String>,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val invitableFriends = friends.filter { friend ->
        !members.any { it.userId == friend.userId } && 
        !pendingInvites.contains(friend.userId)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barát meghívása") },
        text = {
            Column {
                if (invitableFriends.isEmpty()) {
                    Text("Nincs meghívható barát")
                } else {
                    Text("Válassz barátot a meghíváshoz:", modifier = Modifier.padding(bottom = 8.dp))
                    Column {
                        invitableFriends.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onInvite(friend.userId) }
                                    .padding(vertical = 8.dp),
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
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Meghívás",
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                            if (friend != invitableFriends.last()) {
                                Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Bezárás")
            }
        }
    )
} 