package com.example.darhoney.utils

import com.example.darhoney.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

object PartnerNotifyHelper {
    private val authRepo = AuthRepository()

    suspend fun currentActor(): Pair<String, String>? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val profile = authRepo.getUserProfile(user.uid)
        val name = profile?.displayName?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "Tu pareja"
        return user.uid to name
    }
}
