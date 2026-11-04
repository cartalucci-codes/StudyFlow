package com.studyflow.app.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.AuthError
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.data.repository.AuthResult
import com.studyflow.app.databinding.ActivityRegisterBinding
import com.studyflow.app.ui.home.HomeActivity
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.Validators
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch

/**
 * Registration screen. The password travels over HTTPS and is hashed with bcrypt on the server before
 * it is stored (api/routes/auth.js) - it is never stored or logged in plain text.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        authRepository = AuthRepository(
            RetrofitClient.api, AppDatabase.getInstance(this).userDao(), SessionManager(this)
        )
        binding.buttonRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val name = binding.inputName.text.toString()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString()
        val confirm = binding.inputConfirmPassword.text.toString()

        binding.layoutName.error = if (Validators.isValidName(name)) null else getString(R.string.error_required)
        binding.layoutEmail.error = if (Validators.isValidEmail(email)) null else getString(R.string.error_email_invalid)
        binding.layoutPassword.error = if (Validators.isStrongPassword(password)) null else getString(R.string.error_password_weak)
        binding.layoutConfirmPassword.error = if (password == confirm) null else getString(R.string.error_password_mismatch)
        if (listOf(binding.layoutName, binding.layoutEmail, binding.layoutPassword, binding.layoutConfirmPassword)
                .any { it.error != null }
        ) return

        setLoading(true)
        lifecycleScope.launch {
            when (val result = authRepository.register(name, email, password)) {
                is AuthResult.Success -> {
                    startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                    finishAffinity()
                }
                is AuthResult.Error -> {
                    setLoading(false)
                    val message = when (result.kind) {
                        AuthError.EMAIL_TAKEN -> R.string.msg_email_taken
                        AuthError.NETWORK -> R.string.msg_network_error
                        AuthError.INVALID_INPUT, AuthError.INVALID_CREDENTIALS, AuthError.GOOGLE -> R.string.msg_invalid_input
                        AuthError.SERVER -> R.string.msg_server_error
                    }
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressLoading.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        binding.buttonRegister.isEnabled = !loading
    }
}
