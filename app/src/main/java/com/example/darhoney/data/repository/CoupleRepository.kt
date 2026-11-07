package com.example.darhoney.data.repository

import com.example.darhoney.data.model.Couple
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CoupleRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun generateInviteCode(): String =
        UUID.randomUUID().toString().substring(0, 6).uppercase()

    suspend fun createCouple(userId: String, coupleName: String): Result<Couple> = runCatching {
        val inviteCode = generateInviteCode()
        val coupleRef = db.collection("couples").document()
        val couple = Couple(
            id = coupleRef.id,
            name = coupleName,
            memberIds = listOf(userId),
            inviteCode = inviteCode
        )
        coupleRef.set(couple).await()
        db.collection("inviteCodes").document(inviteCode).set(mapOf("coupleId" to couple.id)).await()
        couple
    }

    suspend fun joinCouple(userId: String, inviteCode: String): Result<Couple> = runCatching {
        val code = inviteCode.trim().uppercase()
        if (code.length != 6) {
            throw Exception("El código debe tener 6 caracteres")
        }

        val codeDoc = db.collection("inviteCodes").document(code).get().await()
        if (!codeDoc.exists()) {
            throw Exception("Código de invitación inválido")
        }

        val coupleId = codeDoc.getString("coupleId")?.takeIf { it.isNotBlank() }
            ?: throw Exception("Código de invitación inválido")

        val coupleRef = db.collection("couples").document(coupleId)
        val coupleDoc = coupleRef.get().await()
        val couple = coupleDoc.toObject(Couple::class.java)
            ?: throw Exception("Pareja no encontrada")

        if (couple.memberIds.size >= 2) {
            throw Exception("Esta pareja ya tiene dos miembros")
        }
        if (couple.memberIds.contains(userId)) {
            throw Exception("Ya eres miembro de esta pareja")
        }

        val updatedMembers = couple.memberIds + userId
        coupleRef.update("memberIds", updatedMembers).await()
        couple.copy(memberIds = updatedMembers)
    }

    fun coupleFlow(coupleId: String): Flow<Couple?> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("couples").document(coupleId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(Couple::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun getCouple(coupleId: String): Couple? {
        if (coupleId.isBlank()) return null
        val doc = db.collection("couples").document(coupleId).get().await()
        return doc.toObject(Couple::class.java)
    }
}
