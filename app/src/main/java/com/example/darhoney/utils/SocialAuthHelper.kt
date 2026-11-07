package com.example.darhoney.utils

import android.content.Context
import com.example.darhoney.R
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

object SocialAuthHelper {

    private fun isValidWebClientId(id: String): Boolean =
        id.isNotBlank() &&
            id.endsWith(".apps.googleusercontent.com") &&
            !id.contains("xxxx", ignoreCase = true)

    fun isGoogleConfigured(context: Context): Boolean {
        val fromProps = context.getString(R.string.google_web_client_id)
        if (isValidWebClientId(fromProps)) return true
        return isValidWebClientId(webClientIdFromGeneratedResources(context))
    }

    private fun webClientIdFromGeneratedResources(context: Context): String {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) return ""
        return context.getString(id)
    }

    fun googleWebClientId(context: Context): String {
        val fromProps = context.getString(R.string.google_web_client_id)
        if (isValidWebClientId(fromProps)) return fromProps
        val generated = webClientIdFromGeneratedResources(context)
        if (isValidWebClientId(generated)) return generated
        return ""
    }

    fun googleSignInOptions(context: Context): GoogleSignInOptions {
        val webClientId = googleWebClientId(context)
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    fun googleSignInClient(context: Context) =
        GoogleSignIn.getClient(context, googleSignInOptions(context))

    fun isFacebookConfigured(context: Context): Boolean =
        context.getString(R.string.facebook_app_id).isNotBlank()

    fun signOutProviders(context: Context) {
        if (isGoogleConfigured(context)) {
            googleSignInClient(context).signOut()
        }
        if (isFacebookConfigured(context)) {
            try {
                LoginManager.getInstance().logOut()
            } catch (_: Exception) {
            }
        }
    }
}
