package com.studyflow.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.studyflow.app.data.repository.AuthError
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.data.repository.AuthResult
import com.studyflow.app.databinding.ActivityLoginBinding
import com.studyflow.app.ui.home.HomeActivity
import com.studyflow.app.ui.register.RegisterActivity
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.Validators
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var session: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient

    // Receives the result of the Google account picker launched by the "Sign in with Google" button.
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                signInWithGoogleToken(idToken)
            } else {
                Toast.makeText(this, R.string.msg_google_no_token, Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            // Status 10 (DEVELOPER_ERROR) means the SHA-1 / package name registered in Google Cloud
            // Console doesn't match this build; 12501 means the student cancelled.
            Log.w(TAG, "Google sign-in failed, status ${e.statusCode}")
            if (e.statusCode != 12501) {
                Toast.makeText(this, getString(R.string.msg_google_failed, e.statusCode), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        session = SessionManager(this)
        authRepository = AuthRepository(RetrofitClient.api, AppDatabase.getInstance(this).userDao(), session)

        // Already logged in from an earlier visit -> straight to Home
        if (session.isLoggedIn) {
            goToHome()
            return
        }

        // requestIdToken takes the WEB client ID so the API can verify the token (see README, "Google SSO")
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, options)

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.textRegisterLink.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.buttonGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut() // always show the account picker
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun attemptLogin() {
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()

        // Validate before touching the network, and show the problem next to the field.
        binding.layoutEmail.error = if (Validators.isValidEmail(email)) null else getString(R.string.error_email_invalid)
        binding.layoutPassword.error = if (password.isNotEmpty()) null else getString(R.string.error_required)
        if (binding.layoutEmail.error != null || binding.layoutPassword.error != null) return

        setLoading(true)
        lifecycleScope.launch {
            handleResult(authRepository.login(email, password))
        }
    }

    private fun signInWithGoogleToken(idToken: String) {
        setLoading(true)
        lifecycleScope.launch {
            handleResult(authRepository.loginWithGoogle(idToken))
        }
    }

    private fun handleResult(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> goToHome()
            is AuthResult.Error -> {
                setLoading(false)
                Toast.makeText(this, messageFor(result.kind), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun messageFor(kind: AuthError): String = getString(
        when (kind) {
            AuthError.INVALID_CREDENTIALS -> R.string.msg_invalid_credentials
            AuthError.NETWORK -> R.string.msg_network_error
            AuthError.GOOGLE -> R.string.msg_google_server_failed
            AuthError.EMAIL_TAKEN, AuthError.INVALID_INPUT -> R.string.msg_invalid_input
            AuthError.SERVER -> R.string.msg_server_error
        }
    )

    private fun setLoading(loading: Boolean) {
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.buttonLogin.isEnabled = !loading
        binding.buttonGoogleSignIn.isEnabled = !loading
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
