package com.example.darhoney.ui.auth

import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class FacebookLoginHelper(
    private val activity: AppCompatActivity,
    private val onToken: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    val callbackManager: CallbackManager = CallbackManager.Factory.create()

    init {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val token = result.accessToken.token
                    if (token.isBlank()) {
                        onError("No se recibió token de Facebook")
                    } else {
                        onToken(token)
                    }
                }

                override fun onCancel() {
                    // Usuario canceló; no mostrar error
                }

                override fun onError(error: FacebookException) {
                    onError(translateFacebookError(error.message ?: error.toString()))
                }
            }
        )
    }

    fun logIn() {
        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            callbackManager,
            listOf("email", "public_profile")
        )
    }

    private fun translateFacebookError(message: String): String = when {
        message.contains("certificate hash", ignoreCase = true) ||
            message.contains("package certificate", ignoreCase = true) ->
            "Añade el Key Hash en Meta (Logcat → DarHoneyAuth) y package com.example.darhoney."
        else -> message
    }
}
