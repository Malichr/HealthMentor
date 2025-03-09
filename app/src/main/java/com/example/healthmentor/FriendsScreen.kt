package com.example.healthmentor

import com.example.healthmentor.models.FriendRequest
import com.example.healthmentor.models.UserProfile
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.healthmentor.components.CommonBottomBar
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background

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
                                .size(24.dp)
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
                                    fontSize = 12.sp,
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
        AnimatedVisibility(visible = expanded) {
            content()
        }
        if (expanded) {
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun FriendsScreen(navController: NavController) {
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var friendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentFriendRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var newFriendEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(UserProfile::class.java)
                    userName = user?.email ?: currentUser.email ?: ""
                }
                .addOnFailureListener { e ->
                    Log.e("FriendsScreen", "Hiba a felhasználó adatainak lekérdezésénél", e)
                }

            db.collection("friendRequests")
                .whereEqualTo("toUserId", currentUser.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FriendsScreen", "Hiba a barátkérelmek lekérdezésénél", e)
                        return@addSnapshotListener
                    }
                    
                    friendRequests = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    
                    Log.d("FriendsScreen", "Beérkező kérelmek: ${friendRequests.size}")
                }

            db.collection("friendRequests")
                .whereEqualTo("fromUserId", currentUser.uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FriendsScreen", "Hiba az elküldött kérelmek lekérdezésénél", e)
                        return@addSnapshotListener
                    }
                    
                    sentFriendRequests = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    
                    Log.d("FriendsScreen", "Elküldött kérelmek: ${sentFriendRequests.size}")
                }

            db.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FriendsScreen", "Hiba a felhasználó adatainak lekérdezésénél", e)
                        return@addSnapshotListener
                    }

                    val user = snapshot?.toObject(UserProfile::class.java)
                    val friendIds = user?.friends ?: emptyList()

                    if (friendIds.isNotEmpty()) {
                        db.collection("users")
                            .whereIn("userId", friendIds)
                            .get()
                            .addOnSuccessListener { documents ->
                                friends = documents.mapNotNull { it.toObject(UserProfile::class.java) }
                                Log.d("FriendsScreen", "Barátok száma: ${friends.size}")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FriendsScreen", "Hiba a barátok lekérdezésénél", exception)
                            }
                    } else {
                        friends = emptyList()
                    }
                }
        }
    }

    Scaffold(
        bottomBar = {
            CommonBottomBar(navController = navController, currentRoute = "friends")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendDialog = true },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Barát hozzáadása",
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Felhasználó",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.h6
                    )
                }
            }

            if (friendRequests.isNotEmpty()) {
                ExpandableSection(
                    title = "Barátkérelmek",
                    badge = friendRequests.size
                ) {
                    FriendRequestsList(friendRequests) { request, accepted ->
                        handleFriendRequestResponse(request, accepted)
                    }
                }
            }

            if (sentFriendRequests.isNotEmpty()) {
                ExpandableSection(
                    title = "Elküldött kérelmek",
                    badge = sentFriendRequests.size
                ) {
                    SentFriendRequestsList(
                        requests = sentFriendRequests,
                        onCancel = { request ->
                            cancelFriendRequest(request.id)
                        }
                    )
                }
            }

            ExpandableSection(
                title = "Barátaim",
                badge = friends.size
            ) {
                if (friends.isEmpty()) {
                    Text(
                        text = "Még nincsenek barátaid.",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn {
                        items(friends) { friend ->
                            FriendItem(
                                friend = friend,
                                onRemove = {
                                    currentUser?.uid?.let { userId ->
                                        removeFriend(userId, friend.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAddFriendDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showAddFriendDialog = false 
                    newFriendEmail = ""
                    errorMessage = null
                },
                title = { Text("Barát hozzáadása") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newFriendEmail,
                            onValueChange = { newFriendEmail = it },
                            label = { Text("Email cím") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFriendEmail.isNotEmpty()) {
                                sendFriendRequest(newFriendEmail) { success, message ->
                                    if (success) {
                                        showAddFriendDialog = false
                                        newFriendEmail = ""
                                        errorMessage = null
                                    } else {
                                        errorMessage = message
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Hozzáadás")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showAddFriendDialog = false
                            newFriendEmail = ""
                            errorMessage = null
                        }
                    ) {
                        Text("Mégse")
                    }
                }
            )
        }
    }
}

@Composable
fun FriendRequestsList(
    requests: List<FriendRequest>,
    onResponse: (FriendRequest, Boolean) -> Unit
) {
    Column {
        requests.forEach { request ->
            FriendRequestItem(
                request = request,
                onResponse = { accepted -> onResponse(request, accepted) }
            )
        }
    }
}

@Composable
fun FriendRequestItem(request: FriendRequest, onResponse: (Boolean) -> Unit) {
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
                    text = request.fromUserEmail,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = "Barát kérelmet küldött",
                    style = MaterialTheme.typography.caption
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onResponse(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Elfogadás",
                        tint = androidx.compose.ui.graphics.Color.Green
                    )
                }
                IconButton(
                    onClick = { onResponse(false) }
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

@Composable
fun FriendItem(friend: UserProfile, onRemove: () -> Unit) {
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
            Text(friend.email)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Barát törlése")
            }
        }
    }
}

private fun sendFriendRequest(toEmail: String, onError: (Boolean, String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    db.collection("users")
        .whereEqualTo("email", toEmail)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                onError(false, "Nem található felhasználó ezzel az email címmel")
                return@addOnSuccessListener
            }

            val toUser = documents.documents.first()
            val toUserId = toUser.getString("userId") ?: toUser.id

            if (toUserId == currentUser?.uid) {
                onError(false, "Nem küldhetsz magadnak barát kérelmet")
                return@addOnSuccessListener
            }

            val friendRequest = FriendRequest(
                fromUserId = currentUser?.uid ?: "",
                fromUserEmail = currentUser?.email ?: "",
                toUserId = toUserId,
                toUserEmail = toEmail,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            db.collection("friendRequests")
                .whereEqualTo("fromUserId", currentUser?.uid)
                .whereEqualTo("toUserEmail", toEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { existingRequests ->
                    if (!existingRequests.isEmpty) {
                        onError(false, "Már van függőben lévő kérelem ehhez a felhasználóhoz")
                        return@addOnSuccessListener
                    }

                    db.collection("friendRequests")
                        .add(friendRequest)
                        .addOnSuccessListener {
                            onError(true, null)
                        }
                        .addOnFailureListener { e ->
                            onError(false, "Hiba történt a kérelem küldése közben: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    onError(false, "Hiba történt a felhasználó keresése közben: ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            onError(false, "Hiba történt a felhasználó keresése közben: ${e.message}")
        }
}

private fun handleFriendRequestResponse(request: FriendRequest, accepted: Boolean) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return

    if (accepted) {
        db.collection("friendRequests").document(request.id)
            .update("status", "accepted")
            .addOnSuccessListener {
                db.collection("users").document(currentUser.uid)
                    .update("friends", FieldValue.arrayUnion(request.fromUserId))
                
                db.collection("users").document(request.fromUserId)
                    .update("friends", FieldValue.arrayUnion(currentUser.uid))
                
                Log.d("FriendsScreen", "Barátkérelem elfogadva")
            }
            .addOnFailureListener { e ->
                Log.e("FriendsScreen", "Hiba a barátkérelem elfogadásakor", e)
            }
    } else {
        db.collection("friendRequests").document(request.id)
            .delete()
            .addOnSuccessListener {
                Log.d("FriendsScreen", "Barátkérelem elutasítva")
            }
            .addOnFailureListener { e ->
                Log.e("FriendsScreen", "Hiba a barátkérelem elutasításakor", e)
            }
    }
}

private fun removeFriend(currentUserId: String?, friendId: String) {
    if (currentUserId == null) return
    
    val db = FirebaseFirestore.getInstance()

    db.collection("users").document(currentUserId).update(
        "friends", FieldValue.arrayRemove(friendId)
    )
    db.collection("users").document(friendId).update(
        "friends", FieldValue.arrayRemove(currentUserId)
    )
}

@Composable
fun SentFriendRequestsList(
    requests: List<FriendRequest>,
    onCancel: (FriendRequest) -> Unit
) {
    Column {
        requests.forEach { request ->
            SentFriendRequestItem(
                request = request,
                onCancel = { onCancel(request) }
            )
        }
    }
}

@Composable
fun SentFriendRequestItem(request: FriendRequest, onCancel: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.toUserEmail,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = "Függőben",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kérelem visszavonása",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

private fun cancelFriendRequest(requestId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("friendRequests").document(requestId)
        .delete()
        .addOnSuccessListener {
            Log.d("FriendsScreen", "Barátkérelem sikeresen törölve: $requestId")
        }
        .addOnFailureListener { e ->
            Log.e("FriendsScreen", "Hiba a barátkérelem törlése közben", e)
        }
}