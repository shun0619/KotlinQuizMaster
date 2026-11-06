package com.example.kotlinquizmaster.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivitySignUpBinding
import com.example.kotlinquizmaster.model.Role
import com.example.kotlinquizmaster.util.Session

/** Students create their own account here. */
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.loginLink.setOnClickListener { finish() }
        binding.createButton.setOnClickListener { createAccount() }
    }

    private fun createAccount() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmInput.text?.toString().orEmpty()
        listOf(binding.nameLayout, binding.emailLayout, binding.passwordLayout, binding.confirmLayout).forEach { it.error = null }

        when {
            name.isEmpty() -> { binding.nameLayout.error = "Enter your name"; return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { binding.emailLayout.error = "Enter a valid email"; return }
            password.length < MIN_PASSWORD -> { binding.passwordLayout.error = "Use at least $MIN_PASSWORD characters"; return }
            password != confirm -> { binding.confirmLayout.error = "Passwords do not match"; return }
        }

        val repository = QuizRepository(this)
        if (repository.registerUser(name, email, password, Role.STUDENT) == -1L) {
            binding.emailLayout.error = "This email is already registered"
            return
        }
        val user = repository.login(email, password) ?: return
        Session.save(this, user)
        Toast.makeText(this, "Account created. Welcome, $name!", Toast.LENGTH_SHORT).show()
        Session.openHome(this, Role.STUDENT)
    }

    companion object {
        const val MIN_PASSWORD = 6
    }
}
