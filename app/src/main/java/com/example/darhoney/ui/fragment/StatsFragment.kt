package com.example.darhoney.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import android.util.Patterns
import com.example.darhoney.R
import com.example.darhoney.databinding.FragmentStatsBinding
import com.example.darhoney.ui.AuthViewModel
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.auth.LoginActivity
import com.example.darhoney.notifications.NotificationHelper
import com.example.darhoney.utils.FormatUtils
import com.example.darhoney.utils.SessionPreferences
import com.example.darhoney.utils.SocialAuthHelper
import com.example.darhoney.utils.StatsCalculator
import com.example.darhoney.utils.applyStatusBarPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }
    private val authViewModel: AuthViewModel by activityViewModels { ViewModelFactory.instance }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contentLayout.applyStatusBarPadding()

        viewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            val stats = StatsCalculator.calculate(restaurants)
            binding.tvTotalRestaurants.text = stats.totalRestaurants.toString()
            binding.tvTotalVisits.text = stats.totalVisits.toString()
            binding.tvTopCity.text = stats.topCity
            binding.tvFavoriteFood.text = stats.favoriteFoodType
            binding.tvBestRated.text = stats.bestRated?.name ?: "—"
            binding.tvMostExpensive.text = stats.mostExpensive?.name ?: "—"
            binding.tvCheapest.text = stats.cheapest?.name ?: "—"
            binding.tvAverageSpend.text = FormatUtils.formatCurrency(stats.averageSpend)
            binding.tvTotalSpent.text = FormatUtils.formatCurrency(stats.totalSpent)
            binding.tvRomanticDates.text = stats.romanticDates.toString()
            binding.tvMostVisited.text = stats.mostVisited?.name ?: "—"
        }

        binding.btnSignOut.setOnClickListener { signOut() }

        binding.btnManagePassword.setOnClickListener { showManagePasswordDialog() }

        binding.btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.darhoney.R.string.delete_account_title)
                .setMessage(com.example.darhoney.R.string.delete_account_message)
                .setNegativeButton(com.example.darhoney.R.string.cancel, null)
                .setPositiveButton(com.example.darhoney.R.string.delete_account_confirm) { _, _ ->
                    authViewModel.deleteAccount()
                }
                .show()
        }

        authViewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.btnSignOut.isEnabled = !loading
            binding.btnManagePassword.isEnabled = !loading
            binding.btnDeleteAccount.isEnabled = !loading
        }

        authViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                authViewModel.clearError()
            }
        }

        authViewModel.passwordResetSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                Toast.makeText(requireContext(), R.string.password_reset_sent, Toast.LENGTH_LONG).show()
                SessionPreferences.getCoupleId(requireContext())?.takeIf { it.isNotBlank() }?.let { coupleId ->
                    NotificationHelper.showPasswordResetSent(requireContext(), coupleId)
                }
                authViewModel.clearPasswordResetSent()
            }
        }

        authViewModel.accountDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) navigateToLogin()
        }
    }

    private fun showManagePasswordDialog() {
        val user = FirebaseAuth.getInstance().currentUser
        val hasEmailPassword = user?.providerData?.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
        } == true
        if (!hasEmailPassword) {
            Toast.makeText(requireContext(), R.string.password_only_email, Toast.LENGTH_LONG).show()
            return
        }
        val email = user?.email?.trim().orEmpty()
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), R.string.invalid_email, Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.manage_password)
            .setMessage(getString(R.string.manage_password_message) + "\n\n$email")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.send_reset_link) { _, _ ->
                authViewModel.sendPasswordReset(email)
            }
            .show()
    }

    private fun signOut() {
        SocialAuthHelper.signOutProviders(requireContext())
        authViewModel.signOut()
        SessionPreferences.clear(requireContext())
        Glide.get(requireContext()).clearMemory()
        lifecycleScope.launch(Dispatchers.IO) {
            Glide.get(requireContext()).clearDiskCache()
        }
        navigateToLogin()
    }

    private fun navigateToLogin() {
        SessionPreferences.clear(requireContext())
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(@Suppress("UNUSED_PARAMETER") coupleId: String) = StatsFragment()
    }
}
