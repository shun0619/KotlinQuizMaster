package com.example.kotlinquizmaster.util

import java.security.MessageDigest

/** Passwords are never stored as plain text; only their SHA-256 hash is saved. */
object Security {
    fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
