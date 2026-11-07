package com.example.darhoney.ui.couple

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.darhoney.databinding.ActivityCoupleSetupBinding
import com.example.darhoney.ui.AuthViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.main.MainActivity

class CoupleSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoupleSetupBinding
    private val viewModel: AuthViewModel by viewModels { ViewModelFactory.instance }
    private var pendingCoupleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoupleSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreate.setOnClickListener {
            val name = binding.etCoupleName.text.toString().trim()
                .ifBlank { "Nuestra aventura gastronómica" }
            viewModel.createCouple(name)
        }

        binding.btnJoin.setOnClickListener {
            val code = binding.etInviteCode.text.toString().trim().uppercase()
            if (code.isBlank()) {
                Toast.makeText(this, "Ingresa el código de invitación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.length != 6) {
                Toast.makeText(this, "El código debe tener 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.joinCouple(code)
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvInviteCode.text.toString()
            if (code.isNotBlank() && code != "—") {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", code))
                Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnContinue.setOnClickListener {
            pendingCoupleId?.let { goToMain(it) }
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnCreate.isEnabled = !loading
            binding.btnJoin.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.coupleReady.observe(this) { couple ->
            couple ?: return@observe
            showInviteCode(couple)
            viewModel.clearCoupleReady()
        }
    }

    private fun showInviteCode(couple: com.example.darhoney.data.model.Couple) {
        binding.cardInvite.visibility = View.VISIBLE
        binding.tvInviteCode.text = couple.inviteCode
        pendingCoupleId = couple.id

        if (couple.memberIds.size >= 2) {
            goToMain(couple.id)
        } else {
            binding.btnContinue.visibility = View.VISIBLE
            Toast.makeText(
                this,
                "Código listo: ${couple.inviteCode}. Compártelo con tu pareja.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun goToMain(coupleId: String) {
        viewModel.persistSession(this, coupleId)
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_COUPLE_ID, coupleId)
        })
        finish()
    }
}
