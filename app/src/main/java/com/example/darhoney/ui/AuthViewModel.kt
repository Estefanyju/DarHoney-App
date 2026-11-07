package com.example.darhoney.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.darhoney.data.model.Couple
import com.example.darhoney.data.model.UserProfile
import com.example.darhoney.data.repository.AuthRepository
import com.example.darhoney.data.repository.CoupleRepository
import com.example.darhoney.utils.SessionPreferences
import com.example.darhoney.utils.toUserMessage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepo: AuthRepository,
    private val coupleRepo: CoupleRepository
) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _profile = MutableLiveData<UserProfile?>()
    val profile: LiveData<UserProfile?> = _profile

    private val _couple = MutableLiveData<Couple?>()
    val couple: LiveData<Couple?> = _couple

    private val _coupleReady = MutableLiveData<Couple?>()
    val coupleReady: LiveData<Couple?> = _coupleReady

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _authSuccess = MutableLiveData(false)
    val authSuccess: LiveData<Boolean> = _authSuccess

    private val _accountDeleted = MutableLiveData(false)
    val accountDeleted: LiveData<Boolean> = _accountDeleted

    private val _passwordResetSent = MutableLiveData(false)
    val passwordResetSent: LiveData<Boolean> = _passwordResetSent

    init {
        viewModelScope.launch {
            authRepo.authStateFlow().collectLatest { firebaseUser ->
                _user.value = firebaseUser
                if (firebaseUser != null) {
                    observeProfile(firebaseUser.uid)
                } else {
                    _profile.value = null
                    _couple.value = null
                }
            }
        }
    }

    private fun observeProfile(uid: String) {
        viewModelScope.launch {
            authRepo.userProfileFlow(uid).collectLatest { profile ->
                _profile.value = profile
                profile?.coupleId?.takeIf { it.isNotBlank() }?.let { coupleId ->
                    coupleRepo.coupleFlow(coupleId).collectLatest { couple ->
                        if (couple != null) _couple.value = couple
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                authRepo.signIn(email, password)
                    .onSuccess { _authSuccess.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                authRepo.signUp(email, password, displayName)
                    .onSuccess { _authSuccess.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                authRepo.signInWithGoogle(idToken)
                    .onSuccess { _authSuccess.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun signInWithFacebook(accessToken: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                authRepo.signInWithFacebook(accessToken)
                    .onSuccess { _authSuccess.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun createCouple(name: String) {
        val uid = authRepo.currentUser?.uid
        if (uid == null) {
            _error.value = "Sesión no válida. Vuelve a iniciar sesión."
            return
        }
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val couple = coupleRepo.createCouple(uid, name).getOrElse { error ->
                    _error.value = error.toUserMessage()
                    return@launch
                }
                authRepo.updateUserCoupleId(uid, couple.id)
                _couple.value = couple
                _coupleReady.value = couple
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    fun joinCouple(code: String) {
        val uid = authRepo.currentUser?.uid
        if (uid == null) {
            _error.value = "Sesión no válida. Vuelve a iniciar sesión."
            return
        }
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val couple = coupleRepo.joinCouple(uid, code).getOrElse { error ->
                    _error.value = error.toUserMessage()
                    return@launch
                }
                authRepo.updateUserCoupleId(uid, couple.id)
                _couple.value = couple
                _coupleReady.value = couple
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearCoupleReady() { _coupleReady.value = null }

    fun persistSession(context: Context, coupleId: String? = null) {
        val user = authRepo.currentUser ?: return
        val cid = coupleId ?: _profile.value?.coupleId ?: return
        if (cid.isBlank()) return
        val role = _profile.value?.role ?: com.example.darhoney.data.model.UserRole.STANDARD.name
        SessionPreferences.save(context, user.uid, cid, role)
    }

    fun signOut() = authRepo.signOut()

    fun sendPasswordReset(email: String) {
        _loading.value = true
        _error.value = null
        _passwordResetSent.value = false
        viewModelScope.launch {
            try {
                authRepo.sendPasswordResetEmail(email)
                    .onSuccess { _passwordResetSent.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearPasswordResetSent() {
        _passwordResetSent.value = false
    }

    fun deleteAccount() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                authRepo.deleteAccount()
                    .onSuccess { _accountDeleted.value = true }
                    .onFailure { _error.value = it.toUserMessage() }
            } finally {
                _loading.value = false
            }
        }
    }
}
