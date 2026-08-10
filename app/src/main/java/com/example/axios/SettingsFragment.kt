package com.example.axios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.axios.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Fetch user profile information from Google / Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = currentUser?.displayName ?: "Guest User"
        val email = currentUser?.email ?: ""
        
        val rollNo = if (email.endsWith("@iiitl.ac.in")) {
            email.substringBefore("@").uppercase()
        } else {
            "N/A"
        }

        val course = when {
            rollNo.startsWith("LIT") -> "Information Technology"
            rollNo.startsWith("LCS") -> "Computer Science"
            rollNo.startsWith("LDS") -> "Data Science"
            rollNo.startsWith("LBA") -> "Business Administration"
            else -> "IIIT Lucknow"
        }

        // Display user info in My Profile card
        binding.name.text = name
        binding.rollNo.text = rollNo
        binding.course.text = course

        // 2. Setup theme toggle switch with persistence
        val sharedPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        
        binding.switch1.isChecked = isDarkMode
        binding.switch1.text = if (isDarkMode) "Switch to light mode" else "Switch to dark mode"

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("is_dark_mode", isChecked).apply()
            binding.switch1.text = if (isChecked) "Switch to light mode" else "Switch to dark mode"
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 3. Logout action
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
