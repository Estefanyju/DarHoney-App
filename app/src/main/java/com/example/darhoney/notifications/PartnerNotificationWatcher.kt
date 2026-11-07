package com.example.darhoney.notifications

import android.content.Context
import android.util.Log
import com.example.darhoney.R
import com.example.darhoney.data.model.PartnerNotification
import com.example.darhoney.data.repository.PartnerNotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object PartnerNotificationWatcher {
    private const val TAG = "DarHoneyNotify"
    private val repo = PartnerNotificationRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null
    private var activeCoupleId: String? = null
    private val shownIds = mutableSetOf<String>()

    fun start(context: Context, coupleId: String) {
        if (coupleId.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (coupleId == activeCoupleId && watchJob?.isActive == true) return
        stop()
        activeCoupleId = coupleId
        val appContext = context.applicationContext
        watchJob = scope.launch {
            repo.incomingForUserFlow(coupleId, uid).collectLatest { alerts ->
                alerts.forEach { alert ->
                    if (alert.id.isBlank() || alert.id in shownIds) return@forEach
                    shownIds.add(alert.id)
                    Log.d(TAG, "Mostrando aviso pareja: ${alert.type}")
                    NotificationHelper.showPartnerAlert(
                        appContext,
                        coupleId,
                        titleFor(appContext, alert.type),
                        alert.message
                    )
                    launch(Dispatchers.IO) {
                        runCatching { repo.markAsRead(coupleId, alert.id) }
                            .onFailure { Log.w(TAG, "No se pudo marcar leído", it) }
                    }
                }
            }
        }
    }

    fun stop() {
        watchJob?.cancel()
        watchJob = null
        activeCoupleId = null
    }

    private fun titleFor(context: Context, type: String): String = when (type) {
        PartnerNotification.TYPE_WISHLIST_ADDED -> context.getString(R.string.notify_wishlist_title)
        PartnerNotification.TYPE_RESTAURANT_ADDED -> context.getString(R.string.notify_restaurant_title)
        PartnerNotification.TYPE_VISIT_ADDED -> context.getString(R.string.notify_visit_title)
        PartnerNotification.TYPE_PASSWORD_RESET -> context.getString(R.string.partner_password_reset_title)
        else -> context.getString(R.string.app_name)
    }
}
