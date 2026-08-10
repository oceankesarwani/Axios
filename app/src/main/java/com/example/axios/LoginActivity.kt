package com.example.axios

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatDelegate
import com.example.axios.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in and has the allowed email domain
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.email?.endsWith("@iiitl.ac.in") == true) {
            navigateToMain()
        }

        binding.loginBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        // Disable sign-in button during the process to avoid multiple prompts
        binding.loginBtn.isEnabled = false

        val credentialManager = CredentialManager.create(this)

        // Use the generated web client ID from google-services.json, with a hardcoded fallback
        val webClientId = getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.e("LoginActivity", "Unexpected credential type returned: ${credential.type}")
                    Toast.makeText(this@LoginActivity, "Unsupported login method selected", Toast.LENGTH_SHORT).show()
                    binding.loginBtn.isEnabled = true
                }
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Credential Manager failed: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Google Sign-In failed or cancelled", Toast.LENGTH_SHORT).show()
                binding.loginBtn.isEnabled = true
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("LoginActivity", "Token parsing failed: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Failed to parse Google account token", Toast.LENGTH_SHORT).show()
                binding.loginBtn.isEnabled = true
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val email = user?.email

                    if (email != null && email.endsWith("@iiitl.ac.in")) {
                        // Authorized: proceed to MainActivity
                        Toast.makeText(this, "Success: Signed in as $email", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        // Unauthorized: log out immediately and show warning
                        Log.w("LoginActivity", "Rejected unauthorized email domain: $email")
                        Toast.makeText(this, "Access restricted to @iiitl.ac.in students only.", Toast.LENGTH_LONG).show()
                        auth.signOut()
                        binding.loginBtn.isEnabled = true
                    }
                } else {
                    Log.e("LoginActivity", "Firebase Auth with Google credential failed", task.exception)
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    binding.loginBtn.isEnabled = true
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
