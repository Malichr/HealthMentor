package com.example.healthmentor.models

data class User(
    val id: String,
    val email: String,
    val username: String,
    val friends: List<String> = emptyList(),
    val groups: List<String> = emptyList()
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserEmail: String = "",
    val toUserId: String = "",
    val toUserEmail: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)

data class Group(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val pendingInvites: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class FitnessData(
    val userId: String,
    val steps: Int,
    val calories: Int,
    val distance: Float,
    val timestamp: Long
)

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val friends: List<String> = emptyList(),
    val friendRequests: List<String> = emptyList()
)

data class GroupInvite(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val fromUserId: String = "",
    val fromUserEmail: String = "",
    val toUserId: String = "",
    val toUserEmail: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)

data class StepCount(
    val userId: String = "",
    val userEmail: String = "",
    val steps: Int = 0,
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
