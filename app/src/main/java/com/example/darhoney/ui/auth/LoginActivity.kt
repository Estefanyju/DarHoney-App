package com.example.darhoney.ui.auth

import android.util.Patterns
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.darhoney.R
import com.example.darhoney.BuildConfig
import com.example.darhoney.databinding.ActivityLoginBinding
import com.example.darhoney.ui.AuthViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.couple.CoupleSetupActivity
import com.example.darhoney.ui.main.MainActivity
import com.example.darhoney.notifications.NotificationHelper
import com.example.darhoney.utils.SessionPreferences
import com.example.darhoney.utils.SocialAuthHelper
import com.example.darhoney.utils.logSigningFingerprints
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels { ViewModelFactory.instance }
    private var isLoginMode = true
    private var facebookLogin: FacebookLoginHelper? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "No se pudo obtener el token de Google", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            viewModel.signInWithGoogle(idToken)
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                Toast.makeText(this, "Inicio con Google cancelado o fallido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.DEBUG) {
            logSigningFingerprints(packageName, packageManager)
        }

        setupFacebookLogin()
        setupSocialButtons()
        setupObservers()
        setupListeners()
        updateAuthModeUi()

        tryAutoLogin()

        viewModel.user.value?.let { navigateIfReady() }
    }

    private fun tryAutoLogin() {
        val coupleId = SessionPreferences.getCoupleId(this)
        if (FirebaseAuth.getInstance().currentUser != null &&
            SessionPreferences.isLoggedIn(this) &&
            !coupleId.isNullOrBlank()
        ) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_COUPLE_ID, coupleId)
            })
            finish()
        }
    }

    private fun setupFacebookLogin() {
        if (!SocialAuthHelper.isFacebookConfigured(this)) return
        facebookLogin = FacebookLoginHelper(
            activity = this,
            onToken = { viewModel.signInWithFacebook(it) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
        )
    }

    private fun setupSocialButtons() {
        binding.btnGoogle.setOnClickListener {
            if (!SocialAuthHelper.isGoogleConfigured(this)) {
                Toast.makeText(
                    this,
                    "Google: activa el proveedor en Firebase, añade SHA-1 (Logcat → DarHoneyAuth) " +
                        "y descarga google-services.json.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            googleSignInLauncher.launch(SocialAuthHelper.googleSignInClient(this).signInIntent)
        }

        binding.btnFacebook.setOnClickListener {
            if (!SocialAuthHelper.isFacebookConfigured(this)) {
                Toast.makeText(
                    this,
                    "Pon FACEBOOK_APP_ID y FACEBOOK_CLIENT_TOKEN en local.properties y Sync Gradle.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val helper = facebookLogin
            if (helper == null) {
                Toast.makeText(
                    this,
                    "Facebook no está listo. Sync Gradle, Rebuild e instala de nuevo la app.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            helper.logIn()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookLogin?.callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val name = binding.etName.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                binding.tilEmail.error = if (email.isBlank()) "Requerido" else null
                binding.tilPassword.error = if (password.isBlank()) "Requerido" else null
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            binding.tilPassword.error = null

            if (isLoginMode) {
                viewModel.signIn(email, password)
            } else {
                if (name.isBlank()) {
                    binding.tilName.error = "Ingresa tu nombre"
                    return@setOnClickListener
                }
                binding.tilName.error = null
                viewModel.signUp(email, password, name)
            }
        }

        binding.tvToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateAuthModeUi()
        }

        binding.tvForgotPassword.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun updateAuthModeUi() {
        binding.tilName.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.tvForgotPassword.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        binding.btnSubmit.text = if (isLoginMode) "Iniciar sesión" else "Crear cuenta"
        binding.tvToggleMode.text = if (isLoginMode) "¿No tienes cuenta? Regístrate"
        else "¿Ya tienes cuenta? Inicia sesión"
    }

    private fun showForgotPasswordDialog() {
        val padding = (24 * resources.displayMetrics.density).toInt()
        val emailInput = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.etEmail.text.toString().trim())
        }
        val emailLayout = TextInputLayout(this).apply {
            hint = "Correo electrónico"
            addView(emailInput)
            setPadding(padding, padding / 2, padding, 0)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.forgot_password_title)
            .setMessage(R.string.forgot_password_message)
            .setView(emailLayout)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.send_reset_link) { _, _ ->
                val email = emailInput.text?.toString()?.trim().orEmpty()
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                binding.etEmail.setText(email)
                viewModel.sendPasswordReset(email)
            }
            .show()
    }

    private fun setupObservers() {
        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading
            binding.btnGoogle.isEnabled = !loading
            binding.btnFacebook.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.passwordResetSent.observe(this) { sent ->
            if (sent) {
                Toast.makeText(this, R.string.password_reset_sent, Toast.LENGTH_LONG).show()
                SessionPreferences.getCoupleId(this)?.takeIf { it.isNotBlank() }?.let { coupleId ->
                    NotificationHelper.showPasswordResetSent(this, coupleId)
                }
                viewModel.clearPasswordResetSent()
            }
        }

        viewModel.authSuccess.observe(this) { success ->
            if (success) navigateIfReady()
        }

        viewModel.profile.observe(this) { profile ->
            if (profile != null && viewModel.user.value != null) navigateIfReady()
        }
    }

    private fun navigateIfReady() {
        val profile = viewModel.profile.value ?: return
        viewModel.persistSession(this)
        val intent = if (profile.coupleId.isBlank()) {
            Intent(this, CoupleSetupActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_COUPLE_ID, profile.coupleId)
            }
        }
        startActivity(intent)
        finish()
    }
}
