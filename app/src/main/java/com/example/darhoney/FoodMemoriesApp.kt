package com.example.darhoney

import android.app.Application
import com.example.darhoney.notifications.NotificationHelper
import com.example.darhoney.utils.logSigningFingerprints
import com.google.firebase.FirebaseApp

class FoodMemoriesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannel(this)
        if (BuildConfig.DEBUG) {
            logSigningFingerprints(packageName, packageManager)
        }
    }
}
