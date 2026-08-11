package com.example.axios

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.axios.data.DataRepository
import com.example.axios.ui.home.HomeFragment
import com.example.axios.ui.members.MembersWingFragment
import com.example.axios.ui.resources.ResourcesWingFragment
import com.example.axios.ui.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme early
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)

        // Check authentication and domain authorization before loading layout
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email?.endsWith("@iiitl.ac.in") != true) {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // One-time migration: clear old hardcoded defaults (runs only on first launch after update)
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (!prefs.getBoolean("defaults_cleared_v1", false)) {
            DataRepository.clearCache(this)
            prefs.edit().putBoolean("defaults_cleared_v1", true).apply()
        }

        // Initialize DataRepository (loads local cache then starts Firestore sync)
        DataRepository.init(this) {
            // Callback when local data loaded/firestore synced.
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load HomeFragment by default on first launch
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HomeFragment())
                .commit()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav)
        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_resources -> ResourcesWingFragment()
                R.id.nav_members -> MembersWingFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }
            fragment?.let {
                // Clear the backstack when changing main sections
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, it)
                    .commit()
                true
            } ?: false
        }
    }
}