package com.example.axios

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
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
        val isDarkMode = getSharedPreferences("app_settings", MODE_PRIVATE).getBoolean("is_dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser?.email?.endsWith("@iiitl.ac.in") == true) navigateToMain()

        binding.loginBtn.setOnClickListener { signInWithGoogle() }
    }

    private fun signInWithGoogle() {
        binding.loginBtn.isEnabled = false

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val credential = CredentialManager.create(this@LoginActivity)
                    .getCredential(this@LoginActivity, request).credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    firebaseAuthWithGoogle(GoogleIdTokenCredential.createFrom(credential.data).idToken)
                } else {
                    Log.e("LoginActivity", "Unexpected credential type: ${credential.type}")
                    toast("Unsupported login method selected")
                    binding.loginBtn.isEnabled = true
                }
            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Credential Manager failed: ${e.message}", e)
                toast("Google Sign-In failed or cancelled")
                binding.loginBtn.isEnabled = true
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("LoginActivity", "Token parsing failed: ${e.message}", e)
                toast("Failed to parse Google account token")
                binding.loginBtn.isEnabled = true
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val email = auth.currentUser?.email
                    if (email?.endsWith("@iiitl.ac.in") == true) {
                        toast("Success: Signed in as $email")
                        navigateToMain()
                    } else {
                        Log.w("LoginActivity", "Rejected unauthorized email: $email")
                        toast("Access restricted to @iiitl.ac.in students only.", Toast.LENGTH_LONG)
                        auth.signOut()
                        binding.loginBtn.isEnabled = true
                    }
                } else {
                    Log.e("LoginActivity", "Firebase Auth failed", task.exception)
                    toast("Authentication failed. Please try again.")
                    binding.loginBtn.isEnabled = true
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(this, msg, length).show()
}
