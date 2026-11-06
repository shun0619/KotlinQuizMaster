package com.example.kotlinquizmaster.ui.student

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.databinding.ActivityStudentHomeBinding
import com.example.kotlinquizmaster.util.Session

/** Student home: bottom navigation swaps the Quizzes, Gallery and Contact Us fragments. */
class StudentHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_docs -> { openKotlinDocs(); true }
                R.id.action_logout -> { Session.logout(this); true }
                else -> false
            }
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_quizzes -> show(QuizListFragment(), R.string.quizzes)
                R.id.nav_gallery -> show(GalleryFragment(), R.string.gallery)
                R.id.nav_contact -> show(ContactFragment(), R.string.contact_us)
            }
            true
        }
        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.nav_quizzes
    }

    private fun show(fragment: Fragment, titleRes: Int) {
        binding.toolbar.setTitle(titleRes)
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    /** Browse a website with an implicit intent. */
    private fun openKotlinDocs() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.kotlin_docs_url).toUri()))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser is installed.", Toast.LENGTH_SHORT).show()
        }
    }
}
