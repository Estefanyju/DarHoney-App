package com.example.darhoney.utils

import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

private const val TAG = "DarHoneyAuth"

/**
 * Imprime en Logcat las huellas para Firebase (SHA-1) y Meta/Facebook (Key Hash).
 * Android Studio → Logcat → filtra por "DarHoneyAuth".
 */
fun logSigningFingerprints(packageName: String, packageManager: PackageManager) {
    try {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                .signatures ?: emptyArray()
        }

        for (signature in signatures) {
            val bytes = signature.toByteArray()

            val sha1 = MessageDigest.getInstance("SHA-1").digest(bytes)
            val sha1Hex = sha1.joinToString(":") { b -> "%02X".format(b) }
            Log.i(TAG, "SHA-1 (Firebase Console → app Android): $sha1Hex")

            @Suppress("DEPRECATION")
            val fbHash = Base64.encodeToString(
                MessageDigest.getInstance("SHA").digest(bytes),
                Base64.NO_WRAP
            )
            Log.i(TAG, "Key Hash (Meta → app Android → Hashes): $fbHash")
        }
    } catch (e: Exception) {
        Log.w(TAG, "No se pudieron leer las huellas del certificado: ${e.message}")
    }
}
