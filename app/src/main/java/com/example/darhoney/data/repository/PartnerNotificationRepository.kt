package com.example.darhoney.data.repository

import com.example.darhoney.data.model.PartnerNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PartnerNotificationRepository(
    private val coupleRepo: CoupleRepository = CoupleRepository()
) {
    private val db = FirebaseFirestore.getInstance()
    private val tag = "DarHoneyNotify"

    suspend fun notifyPartnerPasswordReset(
        coupleId: String,
        actorUid: String,
        actorName: String,
        actorEmail: String
    ) {
        val message =
            "$actorName solicitó restablecer su contraseña ($actorEmail). " +
                "Si no fuiste tú quien lo pidió, avísale a tu pareja."
        sendToPartner(
            coupleId = coupleId,
            actorUid = actorUid,
            actorName = actorName,
            type = PartnerNotification.TYPE_PASSWORD_RESET,
            message = message
        )
    }

    suspend fun notifyWishlistAdded(
        coupleId: String,
        actorUid: String,
        actorName: String,
        itemName: String
    ) {
        sendToPartner(
            coupleId = coupleId,
            actorUid = actorUid,
            actorName = actorName,
            type = PartnerNotification.TYPE_WISHLIST_ADDED,
            message = "$actorName agregó «$itemName» a la lista de deseos ✨"
        )
    }

    suspend fun notifyRestaurantAdded(
        coupleId: String,
        actorUid: String,
        actorName: String,
        restaurantName: String
    ) {
        sendToPartner(
            coupleId = coupleId,
            actorUid = actorUid,
            actorName = actorName,
            type = PartnerNotification.TYPE_RESTAURANT_ADDED,
            message = "$actorName registró el restaurante «$restaurantName» en su diario 🍽️"
        )
    }

    suspend fun notifyVisitAdded(
        coupleId: String,
        actorUid: String,
        actorName: String,
        restaurantName: String
    ) {
        sendToPartner(
            coupleId = coupleId,
            actorUid = actorUid,
            actorName = actorName,
            type = PartnerNotification.TYPE_VISIT_ADDED,
            message = "$actorName agregó una nueva visita a «$restaurantName» 💜"
        )
    }

    private suspend fun sendToPartner(
        coupleId: String,
        actorUid: String,
        actorName: String,
        type: String,
        message: String
    ) {
        if (coupleId.isBlank() || actorUid.isBlank()) return
        val couple = coupleRepo.getCouple(coupleId) ?: run {
            Log.w(tag, "Pareja no encontrada: $coupleId")
            return
        }
        val partnerId = couple.memberIds.firstOrNull { it != actorUid } ?: run {
            Log.w(tag, "Sin pareja vinculada (solo 1 miembro); no se envía aviso")
            return
        }
        db.collection("couples").document(coupleId)
            .collection("notifications")
            .add(
                mapOf(
                    "type" to type,
                    "targetUserId" to partnerId,
                    "fromUserId" to actorUid,
                    "fromDisplayName" to actorName,
                    "message" to message,
                    "coupleId" to coupleId,
                    "createdAt" to System.currentTimeMillis(),
                    "read" to false
                )
            ).await()
        Log.d(tag, "Aviso enviado a pareja ($type)")
    }

    /** Escucha avisos sin consulta compuesta (evita índice Firestore faltante). */
    fun incomingForUserFlow(coupleId: String, userId: String): Flow<List<PartnerNotification>> =
        callbackFlow {
            if (coupleId.isBlank() || userId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = db.collection("couples").document(coupleId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error escuchando avisos", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(PartnerNotification::class.java)?.copy(id = doc.id)
                    }?.filter { it.targetUserId == userId && !it.read }
                        ?: emptyList()
                    trySend(items)
                }
            awaitClose { listener.remove() }
        }

    @Deprecated("Usar incomingForUserFlow")
    fun unreadForUserFlow(coupleId: String, userId: String): Flow<List<PartnerNotification>> =
        incomingForUserFlow(coupleId, userId)

    suspend fun markAsRead(coupleId: String, notificationId: String) {
        if (coupleId.isBlank() || notificationId.isBlank()) return
        db.collection("couples").document(coupleId)
            .collection("notifications").document(notificationId)
            .update("read", true)
            .await()
    }
}
