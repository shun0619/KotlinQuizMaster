package com.example.kotlinquizmaster.ui.admin

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityAdminBinding
import com.example.kotlinquizmaster.model.Role
import com.example.kotlinquizmaster.ui.auth.SignUpActivity
import com.example.kotlinquizmaster.util.Session

/** The admin creates instructor accounts. */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var repository: QuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)

        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_logout) { Session.logout(this); true } else false
        }
        binding.createButton.setOnClickListener { createInstructor() }
        showInstructors()
    }

    private fun createInstructor() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        listOf(binding.nameLayout, binding.emailLayout, binding.passwordLayout).forEach { it.error = null }

        when {
            name.isEmpty() -> { binding.nameLayout.error = "Enter a name"; return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { binding.emailLayout.error = "Enter a valid email"; return }
            password.length < SignUpActivity.MIN_PASSWORD -> { binding.passwordLayout.error = "Use at least ${SignUpActivity.MIN_PASSWORD} characters"; return }
        }
        if (repository.registerUser(name, email, password, Role.INSTRUCTOR) == -1L) {
            binding.emailLayout.error = "This email is already registered"
            return
        }
        Toast.makeText(this, "Instructor account created for $name", Toast.LENGTH_SHORT).show()
        binding.nameInput.text = null
        binding.emailInput.text = null
        binding.passwordInput.text = null
        showInstructors()
    }

    private fun showInstructors() {
        val names = repository.instructorNames()
        binding.instructorList.text = if (names.isEmpty()) "No instructors yet." else names.joinToString("\n") { "• $it" }
    }
}
