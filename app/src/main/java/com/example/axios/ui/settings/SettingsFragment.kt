package com.example.axios.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.axios.LoginActivity
import com.example.axios.data.DataRepository
import com.example.axios.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: ""
        val rollNo = if (email.endsWith("@iiitl.ac.in")) email.substringBefore("@").uppercase() else "N/A"
        val role = DataRepository.members.filter { it.rollNo.equals(rollNo, ignoreCase = true) }
            .map { it.role }.distinct().joinToString(", ").ifEmpty { "Student" }

        binding.name.text = currentUser?.displayName ?: "Guest User"
        binding.rollNo.text = rollNo
        binding.studentRole.text = role

        val sharedPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isDark = sharedPrefs.getBoolean("is_dark_mode", false)
        binding.switch1.isChecked = isDark
        binding.switch1.text = if (isDark) "Switch to light mode" else "Switch to dark mode"

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("is_dark_mode", isChecked).apply()
            binding.switch1.text = if (isChecked) "Switch to light mode" else "Switch to dark mode"
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
