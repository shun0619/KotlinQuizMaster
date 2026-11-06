package com.example.kotlinquizmaster.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.util.Session

/** Shows the logo for a moment, then opens the right home screen or the login screen. */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val next = Runnable {
        val role = Session.role(this)
        if (role != null) {
            Session.openHome(this, role)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(next, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(next)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DELAY_MS = 2000L
    }
}
