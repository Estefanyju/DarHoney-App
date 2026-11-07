package com.example.darhoney.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.darhoney.R
import com.example.darhoney.ui.main.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "darhoney_tasks"
    private const val CHANNEL_NAME = "DarHoney actividades"
    private const val TAG = "DarHoneyNotify"
    private var nextId = 1000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de tu pareja: deseos, restaurantes y visitas"
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showTaskSaved(context: Context, coupleId: String, title: String, message: String) {
        showNotification(context, coupleId, title, message)
    }

    fun showPartnerAlert(context: Context, coupleId: String, title: String, message: String) {
        showNotification(context, coupleId, title, message)
    }

    fun showPasswordResetSent(context: Context, coupleId: String) {
        showNotification(
            context,
            coupleId,
            context.getString(R.string.password_reset_email_title),
            context.getString(R.string.password_reset_email_body)
        )
    }

    private fun showNotification(context: Context, coupleId: String, title: String, message: String) {
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Sin permiso POST_NOTIFICATIONS; no se muestra: $title")
            return
        }
        createChannel(context)
        val notificationId = nextId++
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_COUPLE_ID, coupleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "No se pudo publicar notificación", e)
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
