package com.studyflow.app.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.data.repository.AuthResult
import com.studyflow.app.databinding.ActivityRegisterBinding
import com.studyflow.app.ui.home.HomeActivity
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Registration screen. The password is sent to the API over HTTPS and hashed
 * server-side with bcrypt before it is stored (see studyflow-api/routes/auth.js) -
 * it is never written to our own database or Room in plain text.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)
        val db = AppDatabase.getInstance(this)
        authRepository = AuthRepository(RetrofitClient.api, db.userDao(), session)

        binding.buttonRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val name = binding.editName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirm = binding.editConfirmPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonRegister.isEnabled = false
        lifecycleScope.launch {
            when (val result = authRepository.register(name, email, password)) {
                is AuthResult.Success -> {
                    startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                    finish()
                }
                is AuthResult.Error -> {
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
