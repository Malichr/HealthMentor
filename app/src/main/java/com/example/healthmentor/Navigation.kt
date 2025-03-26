package com.example.healthmentor

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("activity") { ActivityScreen(navController) }
        composable("ai_advice") { AIAdviceScreen(navController) }
        composable("challenges") { GroupChallengesScreen(navController) }
        composable("friends") { FriendsScreen(navController) }
        composable("group_details/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            GroupChallengesScreen(
                navController = navController,
                initialGroupId = groupId
            )
        }
        composable("group_member_details/{memberId}/{memberName}") { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: ""
            val memberName = backStackEntry.arguments?.getString("memberName") ?: ""
            GroupMemberDetailsScreen(
                navController = navController,
                memberId = memberId,
                memberName = memberName
            )
        }
    }
}
