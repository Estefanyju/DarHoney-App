package com.example.darhoney.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.darhoney.R
import com.example.darhoney.databinding.ActivityMainBinding
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.auth.LoginActivity
import com.example.darhoney.ui.fragment.HomeFragment
import com.example.darhoney.ui.fragment.MapFragment
import com.example.darhoney.ui.fragment.RankingsFragment
import com.example.darhoney.ui.fragment.StatsFragment
import com.example.darhoney.ui.fragment.TimelineFragment
import com.example.darhoney.ui.fragment.WishlistFragment
import com.example.darhoney.notifications.PartnerNotificationWatcher
import com.example.darhoney.utils.applyNavigationBarPadding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { ViewModelFactory.instance }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            intent.getStringExtra(EXTRA_COUPLE_ID)?.let { PartnerNotificationWatcher.start(this, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomNav.applyNavigationBarPadding()

        val coupleId = intent.getStringExtra(EXTRA_COUPLE_ID)
        if (coupleId.isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewModel.init(coupleId)
        PartnerNotificationWatcher.start(this, coupleId)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(HomeFragment.newInstance(coupleId))
                R.id.nav_map -> showFragment(MapFragment.newInstance(coupleId))
                R.id.nav_timeline -> showFragment(TimelineFragment.newInstance(coupleId))
                R.id.nav_stats -> showFragment(StatsFragment.newInstance(coupleId))
                R.id.nav_wishlist -> showFragment(WishlistFragment.newInstance(coupleId))
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            PartnerNotificationWatcher.stop()
        }
        super.onDestroy()
    }

    private fun showFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    fun getSharedViewModel(): MainViewModel = viewModel

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val EXTRA_COUPLE_ID = "couple_id"
    }
}
