package com.example.kotlinquizmaster.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.kotlinquizmaster.model.Role
import com.example.kotlinquizmaster.model.User
import com.example.kotlinquizmaster.ui.admin.AdminActivity
import com.example.kotlinquizmaster.ui.auth.LoginActivity
import com.example.kotlinquizmaster.ui.instructor.InstructorHomeActivity
import com.example.kotlinquizmaster.ui.student.StudentHomeActivity

/** Remembers who is logged in on this shared device. */
object Session {
    private const val PREFS = "session"
    private const val KEY_ID = "user_id"
    private const val KEY_NAME = "user_name"
    private const val KEY_ROLE = "user_role"

    fun save(context: Context, user: User) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_ID, user.userId)
            .putString(KEY_NAME, user.name)
            .putString(KEY_ROLE, user.role.name)
            .apply()
    }

    fun userId(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ID, -1L)

    fun userName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NAME, "") ?: ""

    fun role(context: Context): Role? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ROLE, null)
        return Role.entries.firstOrNull { it.name == value }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Opens the home screen for the given role and closes everything behind it. */
    fun openHome(activity: Activity, role: Role) {
        val target = when (role) {
            Role.ADMIN -> AdminActivity::class.java
            Role.INSTRUCTOR -> InstructorHomeActivity::class.java
            Role.STUDENT -> StudentHomeActivity::class.java
        }
        activity.startActivity(Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        activity.finish()
    }

    fun logout(activity: Activity) {
        clear(activity)
        activity.startActivity(Intent(activity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        activity.finish()
    }
}
