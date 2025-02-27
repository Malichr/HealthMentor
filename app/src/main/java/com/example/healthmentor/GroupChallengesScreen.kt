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
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.clickable

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun GroupChallengesScreen(navController: NavController) {
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var groupInvites by remember { mutableStateOf<List<GroupInvite>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
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
                .get()
                .addOnSuccessListener { document ->
                    val userProfile = document.toObject(UserProfile::class.java)
                    userProfile?.friends?.let { friendIds ->
                        if (friendIds.isNotEmpty()) {
                            db.collection("users")
                                .whereIn("userId", friendIds)
                                .get()
                                .addOnSuccessListener { documents ->
                                    friends = documents.mapNotNull { 
                                        it.toObject(UserProfile::class.java) 
                                    }
                                }
                        }
                    }
                }
        }
    }

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
            if (groupInvites.isNotEmpty()) {
                Text(
                    text = "Meghívók",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GroupInvitesList(groupInvites) { invite, accepted ->
                    handleGroupInviteResponse(invite, accepted)
                }
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }

            Text(
                text = "Csoportjaim",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (groups.isEmpty()) {
                Text(
                    text = "Még nem vagy tagja egy csoportnak sem.",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn {
                    items(groups) { group ->
                        GroupItem(
                            group = group,
                            onGroupSelected = { selectedGroup = it }
                        )
                    }
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

        selectedGroup?.let { group ->
            LaunchedEffect(group) {
                if (group.members.isNotEmpty()) {
                    db.collection("users")
                        .whereIn("userId", group.members)
                        .get()
                        .addOnSuccessListener { documents ->
                            selectedGroupMembers = documents.mapNotNull { 
                                it.toObject(UserProfile::class.java)
                            }

                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                            group.members.forEach { memberId ->
                                db.collection("stepCounts")
                                    .whereEqualTo("userId", memberId)
                                    .whereEqualTo("date", today)
                                    .get()
                                    .addOnSuccessListener { stepDocs ->
                                        val stepCount = stepDocs.documents.firstOrNull()
                                            ?.toObject(StepCount::class.java)
                                        val steps = stepCount?.steps ?: 0
                                        
                                        val member = selectedGroupMembers.find { it.userId == memberId }
                                        if (member != null) {
                                            memberSteps = memberSteps + (member to steps)
                                        }
                                    }
                            }
                        }
                } else {
                    selectedGroupMembers = emptyList()
                }
            }

            GroupDetailsDialog(
                group = group,
                friends = friends,
                currentUserId = currentUser?.uid ?: "",
                members = selectedGroupMembers,
                memberSteps = memberSteps,
                onDismiss = { 
                    selectedGroup = null
                    selectedGroupMembers = emptyList()
                    memberSteps = emptyList()
                },
                onInvite = { friendId ->
                    if (currentUser?.uid == group.ownerId) {
                        inviteFriendToGroup(group.id, friendId)
                    }
                },
                onLeave = {
                    currentUser?.uid?.let { userId ->
                        leaveGroup(group.id, userId)
                        selectedGroup = null
                        selectedGroupMembers = emptyList()
                    }
                },
                onDelete = {
                    if (currentUser?.uid == group.ownerId) {
                        deleteGroup(group.id)
                        selectedGroup = null
                        selectedGroupMembers = emptyList()
                    }
                }
            )
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
    
    db.collection("users")
        .document(friendId)
        .get()
        .addOnSuccessListener { document ->
            val friendProfile = document.toObject(UserProfile::class.java)
            if (friendProfile != null) {
                val groupInvite = GroupInvite(
                    groupId = groupId,
                    groupName = "",
                    fromUserId = currentUser.uid,
                    fromUserEmail = currentUser.email ?: "",
                    toUserId = friendId,
                    toUserEmail = friendProfile.email
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
fun GroupInvitesList(
    invites: List<GroupInvite>,
    onResponse: (GroupInvite, Boolean) -> Unit
) {
    invites.forEach { invite ->
        GroupInviteItem(invite)
        Button(
            onClick = { onResponse(invite, true) }
        ) {
            Text("Elfogadás")
        }
        Button(
            onClick = { onResponse(invite, false) }
        ) {
            Text("Elutasítás")
        }
    }
}

@Composable
fun GroupInviteItem(invite: GroupInvite) {
    Card(
            modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = invite.groupName,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "Küldő: ${invite.fromUserEmail}",
                style = MaterialTheme.typography.body1
            )
            Text(
                text = "Címzett: ${invite.toUserEmail}",
                style = MaterialTheme.typography.body2
            )
        }
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