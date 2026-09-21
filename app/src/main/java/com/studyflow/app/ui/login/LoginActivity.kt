package com.studyflow.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.data.repository.AuthResult
import com.studyflow.app.databinding.ActivityLoginBinding
import com.studyflow.app.ui.home.HomeActivity
import com.studyflow.app.ui.register.RegisterActivity
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var session: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient

    // Handles the result of the Google sign-in screen launched by buttonGoogleSignIn.
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                signInWithGoogleToken(idToken)
            } else {
                Toast.makeText(this, "No ID token returned by Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            // Status code 10 (DEVELOPER_ERROR) almost always means the SHA-1 fingerprint or
            // package name registered in the Google Cloud Console OAuth client doesn't match
            // this build - see the SSO setup steps in the README.
            Toast.makeText(this, "Google sign-in failed (code ${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        val db = AppDatabase.getInstance(this)
        authRepository = AuthRepository(RetrofitClient.api, db.userDao(), session)

        // requestIdToken takes the WEB client ID (not the Android one) - this is what lets
        // the backend verify the token via google-auth-library. See res/values/strings.xml
        // for default_web_client_id, which you set after creating the OAuth clients.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Already logged in from a previous session -> skip straight to Home
        if (session.isLoggedIn) {
            goToHome()
            return
        }

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.textRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.buttonGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun signInWithGoogleToken(idToken: String) {
        binding.buttonGoogleSignIn.isEnabled = false
        lifecycleScope.launch {
            when (val result = authRepository.loginWithGoogle(idToken)) {
                is AuthResult.Success -> goToHome()
                is AuthResult.Error -> {
                    binding.buttonGoogleSignIn.isEnabled = true
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter your email and password", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonLogin.isEnabled = false
        lifecycleScope.launch {
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> goToHome()
                is AuthResult.Error -> {
                    binding.buttonLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
