package com.example.darhoney.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DarHoneyMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "DarHoney"
        val body = message.notification?.body ?: message.data["body"] ?: return
        val coupleId = message.data["coupleId"].orEmpty()
        if (coupleId.isNotBlank()) {
            NotificationHelper.showTaskSaved(applicationContext, coupleId, title, body)
        }
    }

    override fun onNewToken(token: String) {
        // Listo para registrar el token FCM en Firestore si lo necesitas en producción.
    }
}
