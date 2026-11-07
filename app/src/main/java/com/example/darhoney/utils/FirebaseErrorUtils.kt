package com.example.darhoney.utils

import com.google.firebase.auth.FirebaseAuthException

fun Throwable.toUserMessage(): String {
    val message = message.orEmpty()
    if (message.contains("Object does not exist at location", ignoreCase = true)) {
        return "Firebase Storage no está activo. Ve a Firebase Console → Storage → Empezar y publica las reglas."
    }
    if (message.contains("storage", ignoreCase = true) && message.contains("403", ignoreCase = true)) {
        return "Sin permiso en Storage. Publica las reglas en Firebase Console → Storage → Reglas."
    }
    if (message.contains("PERMISSION_DENIED", ignoreCase = true)) {
        return "Sin permiso en Firestore. Ve a Firebase Console → Firestore → Reglas y publícalas."
    }
    if (message.contains("NOT_FOUND", ignoreCase = true) && message.contains("database", ignoreCase = true)) {
        return "Firestore no está creado. Ve a Firebase Console → Firestore Database → Crear base de datos."
    }
    if (message.contains("UNAVAILABLE", ignoreCase = true) || message.contains("offline", ignoreCase = true)) {
        return "Sin conexión a Firebase. Revisa tu internet."
    }
    if (message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
        return "Authentication no está activado. En Firebase Console → Authentication → Método de acceso, activa el proveedor (Google o Facebook)."
    }
    if (message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true)) {
        return "Este método de inicio de sesión no está activado en Firebase Console → Authentication."
    }
    if (message.contains("not supported via this method", ignoreCase = true) ||
        message.contains("Facebook TOS", ignoreCase = true)
    ) {
        return "Facebook en Android debe usar el SDK de Meta. Configura FACEBOOK_APP_ID en local.properties y el Key Hash en Meta for Developers."
    }
    if (message.contains("certificate hash", ignoreCase = true) ||
        message.contains("package certificate", ignoreCase = true)
    ) {
        return "Falta registrar la huella del certificado: Firebase (SHA-1) y Meta (Key Hash). Mira Logcat con filtro DarHoneyAuth."
    }
    if (message.contains("API key not valid", ignoreCase = true)) {
        return "Firebase no está configurado. Verifica que google-services.json esté en app/."
    }
    if (this is FirebaseAuthException) {
        return when (errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado."
            "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
            "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Correo o contraseña incorrectos."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Ya existe una cuenta con ese correo usando otro método de inicio de sesión."
            "ERROR_USER_NOT_FOUND" -> "No existe una cuenta con este correo."
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, cierra sesión, vuelve a iniciar sesión e intenta eliminar la cuenta de nuevo."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Revisa tu internet."
            else -> message.ifBlank { "No se pudo completar la operación." }
        }
    }
    return message.ifBlank { "Ocurrió un error inesperado." }
}
