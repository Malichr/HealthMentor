package com.example.healthmentor

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.example.healthmentor.components.*
import com.example.healthmentor.models.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupChallengesScreen(
    navController: NavController,
    initialGroupId: String? = null
) {
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var groupInvites by remember { mutableStateOf<List<GroupInvite>>(emptyList()) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedGroupMembers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var memberSteps by remember { mutableStateOf<List<Pair<UserProfile, Int>>>(emptyList()) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("groups")
                .whereArrayContains("members", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GroupChallenges", "Hiba a csoportok figyelése közben", e)
                        return@addSnapshotListener
                    }
                    groups = snapshot?.documents?.mapNotNull { 
                        it.toObject(Group::class.java)?.copy(id = it.id) 
                    } ?: emptyList()
                    
                    selectedGroupMembers = emptyList()
                    memberSteps = emptyList()

                    groups.forEach { group ->
                        if (group.members.contains(currentUser.uid)) {
                            group.members.forEach { memberId ->
                                db.collection("users")
                                    .document(memberId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        val userProfile = document.toObject(UserProfile::class.java)
                                        if (userProfile != null) {
                                            selectedGroupMembers = selectedGroupMembers + userProfile
                                            memberSteps = memberSteps.filter { it.first.userId != memberId } + (userProfile to 0)
                                        }
                                    }
                            }
                        }
                    }
                }

            db.collection("groupInvites")
                .whereEqualTo("toUserId", currentUser.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GroupChallenges", "Hiba a meghívók figyelése közben", e)
                        return@addSnapshotListener
                    }
                    groupInvites = snapshot?.documents?.mapNotNull {
                        it.toObject(GroupInvite::class.java)?.copy(id = it.id)
                    } ?: emptyList()
                }

            db.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GroupChallenges", "Hiba a felhasználó adatainak figyelése közben", e)
                        return@addSnapshotListener
                    }
                    
                    val userProfile = snapshot?.toObject(UserProfile::class.java)
                    userProfile?.friends?.let { friendIds ->
                        if (friendIds.isNotEmpty()) {
                            db.collection("users")
                                .whereIn("userId", friendIds)
                                .addSnapshotListener { friendsSnapshot, friendsError ->
                                    if (friendsError != null) {
                                        Log.e("GroupChallenges", "Hiba a barátok figyelése közben", friendsError)
                                        return@addSnapshotListener
                                    }
                                    friends = friendsSnapshot?.documents?.mapNotNull { 
                                        it.toObject(UserProfile::class.java) 
                                    } ?: emptyList()
                                }
                        } else {
                            friends = emptyList()
                        }
                    }
                }
        }
    }

    LaunchedEffect(groups) {
        groups.forEach { group ->
            if (group.members.contains(currentUser?.uid)) {
                db.collection("users")
                    .whereIn("userId", group.members)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        selectedGroupMembers = querySnapshot.documents.mapNotNull { document ->
                            document.toObject(UserProfile::class.java)
                        }

                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        group.members.forEach { memberId ->
                            db.collection("stepCounts")
                                .whereEqualTo("userId", memberId)
                                .whereEqualTo("date", today)
                                .get()
                                .addOnSuccessListener { stepSnapshot ->
                                    val stepCount = stepSnapshot.documents.firstOrNull()
                                        ?.toObject(StepCount::class.java)
                                    val steps = stepCount?.steps ?: 0
                                    
                                    val member = selectedGroupMembers.find { it.userId == memberId }
                                    if (member != null) {
                                        memberSteps = memberSteps.filter { it.first.userId != memberId } + (member to steps)
                                    }
                                }
                        }
                    }
            }
        }
    }

    if (initialGroupId != null) {
        val group = groups.find { it.id == initialGroupId }
        if (group != null) {
            GroupDetailsScreen(
                navController = navController,
                group = group,
                friends = friends,
                currentUserId = currentUser?.uid ?: "",
                members = selectedGroupMembers,
                onInvite = { friendId -> 
                    inviteFriendToGroup(group.id, friendId)
                },
                onLeave = { 
                    leaveGroup(group.id, currentUser?.uid ?: "")
                    navController.navigateUp()
                },
                onDelete = {
                    deleteGroup(group.id)
                    navController.navigateUp()
                },
                onRemoveMember = { memberId ->
                    removeMemberFromGroup(group.id, memberId)
                }
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                CommonBottomBar(navController = navController, currentRoute = "challenges")
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    backgroundColor = MaterialTheme.colors.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Új csoport létrehozása",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Csoportok",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (groupInvites.isNotEmpty()) {
                    GroupInvitesList(groupInvites) { invite, accepted ->
                        handleGroupInviteResponse(invite, accepted)
                    }
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                LazyColumn {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = {
                                navController.navigate("group_details/${group.id}")
                            }
                        )
                    }
                }
            }

            if (showCreateGroupDialog) {
                CreateGroupDialog(
                    groupName = newGroupName,
                    onGroupNameChange = { newGroupName = it },
                    onDismiss = {
                        showCreateGroupDialog = false
                        newGroupName = ""
                    },
                    onCreate = {
                        createNewGroup(newGroupName)
                        showCreateGroupDialog = false
                        newGroupName = ""
                    }
                )
            }
        }
    }
}

@Composable
fun GroupItem(group: Group, onGroupSelected: (Group) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onGroupSelected(group) },
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

private fun createNewGroup(name: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()
    val group = Group(
        name = name,
        ownerId = currentUser.uid,
        members = listOf(currentUser.uid),
        pendingInvites = emptyList(),
        createdAt = System.currentTimeMillis()
    )
    
    db.collection("groups")
        .add(group)
        .addOnSuccessListener { documentRef ->
            Log.d("GroupChallenges", "Csoport sikeresen létrehozva: ${documentRef.id}")
        }
        .addOnFailureListener { e ->
            Log.e("GroupChallenges", "Hiba a csoport létrehozása közben", e)
        }
}

private fun inviteFriendToGroup(groupId: String, friendId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()
    
    db.collection("groups")
        .document(groupId)
        .get()
        .addOnSuccessListener { groupDoc ->
            val group = groupDoc.toObject(Group::class.java)
            if (group != null) {
                db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener { document ->
                        val friendProfile = document.toObject(UserProfile::class.java)
                        if (friendProfile != null) {
                            val groupInvite = GroupInvite(
                                groupId = groupId,
                                groupName = group.name,
                                fromUserId = currentUser.uid,
                                fromUserEmail = currentUser.email ?: "",
                                toUserId = friendId,
                                toUserEmail = friendProfile.email,
                                status = "pending"
                            )
                            
                            db.collection("groupInvites")
                                .add(groupInvite)
                                .addOnSuccessListener {
                                    db.collection("groups")
                                        .document(groupId)
                                        .update("pendingInvites", FieldValue.arrayUnion(friendId))
                                }
                        }
                    }
            }
        }
}

private fun handleGroupInviteResponse(invite: GroupInvite, accepted: Boolean) {
    val db = FirebaseFirestore.getInstance()
    
    if (accepted) {
        db.runTransaction { transaction ->
            val groupRef = db.collection("groups").document(invite.groupId)
            val inviteRef = db.collection("groupInvites").document(invite.id)
            
            val groupDoc = transaction.get(groupRef)
            val group = groupDoc.toObject(Group::class.java)
            
            if (group != null) {
                val updatedMembers = group.members + invite.toUserId
                val updatedPendingInvites = group.pendingInvites - invite.toUserId
                
                transaction.update(groupRef, mapOf(
                    "members" to updatedMembers,
                    "pendingInvites" to updatedPendingInvites
                ))
                
                transaction.update(inviteRef, "status", "accepted")
            }
        }
    } else {
        db.collection("groupInvites")
            .document(invite.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                db.collection("groups")
                    .document(invite.groupId)
                    .update("pendingInvites", FieldValue.arrayRemove(invite.toUserId))
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
    var showInviteFriendsDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.name) },
        text = {
            Column {
                Text(
                    "Tagok:",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                memberSteps.forEach { (member, steps) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(member.email)
                        Text("$steps lépés")
                    }
                }

                if (group.pendingInvites.isNotEmpty() && currentUserId == group.ownerId) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Függő meghívások:",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    friends.filter { friend ->
                        group.pendingInvites.contains(friend.userId)
                    }.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(friend.email)
                            IconButton(
                                onClick = { cancelGroupInvite(group.id, friend.userId) }
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
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentUserId == group.ownerId) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.error
                        )
                    ) {
                        Text("Csoport törlése")
                    }
                    FloatingActionButton(
                        onClick = { showInviteFriendsDialog = true },
                        modifier = Modifier.size(40.dp),
                        backgroundColor = MaterialTheme.colors.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Barátok meghívása",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }
                } else {
                    TextButton(
                        onClick = onLeave,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.error
                        )
                    ) {
                        Text("Kilépés")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Bezárás")
                }
            }
        }
    )

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

@Composable
fun InviteFriendsDialog(
    friends: List<UserProfile>,
    members: List<UserProfile>,
    pendingInvites: List<String>,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barátok meghívása") },
        text = {
            LazyColumn {
                items(
                    friends.filter { friend ->
                        !members.any { it.userId == friend.userId } &&
                        !pendingInvites.contains(friend.userId)
                    }
                ) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(friend.email)
                        IconButton(
                            onClick = { onInvite(friend.userId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Meghívás",
                                tint = MaterialTheme.colors.primary
                            )
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

@Composable
fun GroupInvitesList(
    invites: List<GroupInvite>,
    onResponse: (GroupInvite, Boolean) -> Unit
) {
    LazyColumn {
        items(invites) { invite ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = invite.groupName,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = "Meghívó: ${invite.fromUserEmail}",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onResponse(invite, true) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Elfogadás",
                                tint = androidx.compose.ui.graphics.Color.Green
                            )
                        }
                        IconButton(
                            onClick = { onResponse(invite, false) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Elutasítás",
                                tint = MaterialTheme.colors.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun cancelGroupInvite(groupId: String, friendId: String) {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("groupInvites")
        .whereEqualTo("groupId", groupId)
        .whereEqualTo("toUserId", friendId)
        .whereEqualTo("status", "pending")
        .get()
        .addOnSuccessListener { documents ->
            documents.forEach { document ->
                document.reference.delete()
            }
            
            db.collection("groups")
                .document(groupId)
                .update("pendingInvites", FieldValue.arrayRemove(friendId))
        }
}

private fun deleteGroup(groupId: String) {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("groups").document(groupId)
        .delete()
        .addOnSuccessListener {
            Log.d("GroupChallenges", "Csoport sikeresen törölve: $groupId")
        }
        .addOnFailureListener { e ->
            Log.e("GroupChallenges", "Hiba a csoport törlése közben", e)
        }
}

private fun leaveGroup(groupId: String, userId: String) {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("groups").document(groupId)
        .update("members", FieldValue.arrayRemove(userId))
        .addOnSuccessListener {
            Log.d("GroupChallenges", "Sikeresen kilépett a csoportból: $userId")
        }
        .addOnFailureListener { e ->
            Log.e("GroupChallenges", "Hiba a csoportból való kilépés közben", e)
        }
}

private fun removeMemberFromGroup(groupId: String, memberId: String) {
    val db = FirebaseFirestore.getInstance()
    
    db.collection("groups")
        .document(groupId)
        .update("members", FieldValue.arrayRemove(memberId))
        .addOnSuccessListener {
            Log.d("GroupChallenges", "Tag sikeresen eltávolítva: $memberId")
        }
        .addOnFailureListener { e ->
            Log.e("GroupChallenges", "Hiba a tag eltávolítása közben", e)
        }
}