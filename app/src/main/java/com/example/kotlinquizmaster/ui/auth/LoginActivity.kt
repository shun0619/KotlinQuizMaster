package com.example.kotlinquizmaster.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityLoginBinding
import com.example.kotlinquizmaster.util.Session

/** One login screen for every role. The role decides which home screen opens. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: QuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)

        // Event 1: click
        binding.loginButton.setOnClickListener { attemptLogin() }
        // Event 2: keyboard "Done" action
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); true } else false
        }
        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        if (email.isEmpty()) { binding.emailLayout.error = "Enter your email"; return }
        if (password.isEmpty()) { binding.passwordLayout.error = "Enter your password"; return }

        val user = repository.login(email, password)
        if (user == null) {
            binding.passwordLayout.error = "Email or password is incorrect"
            return
        }
        Session.save(this, user)
        Toast.makeText(this, getString(R.string.welcome_back) + ", " + user.name, Toast.LENGTH_SHORT).show()
        Session.openHome(this, user.role)
    }
}
