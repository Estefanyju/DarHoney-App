package com.example.darhoney.utils

import android.content.Context

object SessionPreferences {
    private const val PREFS = "darhoney_session"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_UID = "uid"
    private const val KEY_COUPLE_ID = "couple_id"
    private const val KEY_ROLE = "role"

    fun save(context: Context, uid: String, coupleId: String, role: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_UID, uid)
            .putString(KEY_COUPLE_ID, coupleId)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOGGED_IN, false)

    fun getCoupleId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COUPLE_ID, null)

    fun getRole(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROLE, null)
}
