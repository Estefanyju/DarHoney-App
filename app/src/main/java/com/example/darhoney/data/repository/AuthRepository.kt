package com.example.darhoney.data.repository

import com.example.darhoney.data.model.UserProfile
import com.example.darhoney.data.model.UserRole
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val partnerNotifications = PartnerNotificationRepository()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = currentUser != null

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<FirebaseUser> =
        runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Error al crear cuenta")
            val role = UserRole.fromEmail(email).name
            val profile = UserProfile(
                uid = user.uid,
                email = email,
                displayName = displayName,
                role = role
            )
            db.collection("users").document(user.uid).set(profile).await()
            user
        }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await().user
                ?: throw Exception("Error al iniciar sesión")
        }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        val trimmed = email.trim()
        if (trimmed.isBlank()) throw Exception("Ingresa tu correo electrónico")
        auth.sendPasswordResetEmail(trimmed).await()
        runCatching {
            notifyPartnerAfterPasswordReset(trimmed)
        }
    }

    private suspend fun notifyPartnerAfterPasswordReset(email: String) {
        val trimmed = email.trim()
        val user = auth.currentUser
        if (user != null) {
            val profile = getUserProfile(user.uid)
            val coupleId = profile?.coupleId.orEmpty()
            val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
                ?: trimmed.substringBefore("@")
            if (coupleId.isNotBlank()) {
                partnerNotifications.notifyPartnerPasswordReset(
                    coupleId = coupleId,
                    actorUid = user.uid,
                    actorName = displayName,
                    actorEmail = trimmed
                )
            }
            return
        }
        val snapshot = db.collection("users")
            .whereEqualTo("email", trimmed)
            .limit(1)
            .get()
            .await()
        val profile = snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java) ?: return
        if (profile.coupleId.isBlank() || profile.uid.isBlank()) return
        partnerNotifications.notifyPartnerPasswordReset(
            coupleId = profile.coupleId,
            actorUid = profile.uid,
            actorName = profile.displayName.ifBlank { trimmed.substringBefore("@") },
            actorEmail = trimmed
        )
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> =
        signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))

    suspend fun signInWithFacebook(accessToken: String): Result<FirebaseUser> =
        signInWithCredential(FacebookAuthProvider.getCredential(accessToken))

    private suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> =
        runCatching {
            val user = auth.signInWithCredential(credential).await().user
                ?: throw Exception("Error al iniciar sesión")
            ensureUserProfile(user)
            user
        }

    private suspend fun ensureUserProfile(user: FirebaseUser) {
        val existing = getUserProfile(user.uid)
        if (existing != null) return
        val profile = UserProfile(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Usuario",
            role = UserRole.fromEmail(user.email.orEmpty()).name
        )
        db.collection("users").document(user.uid).set(profile).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    fun userProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserCoupleId(uid: String, coupleId: String) {
        db.collection("users").document(uid)
            .set(mapOf("coupleId" to coupleId), SetOptions.merge())
            .await()
    }

    fun signOut() = auth.signOut()

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw Exception("No hay sesión activa")
        val uid = user.uid
        db.collection("users").document(uid).delete().await()
        user.delete().await()
    }
}
