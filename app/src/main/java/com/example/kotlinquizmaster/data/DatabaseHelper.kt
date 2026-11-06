package com.example.kotlinquizmaster.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Creates the local SQLite database that follows the logical ERD. */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE quizzes (
                quiz_id INTEGER PRIMARY KEY AUTOINCREMENT,
                created_by INTEGER NOT NULL REFERENCES users(user_id),
                title TEXT NOT NULL,
                level INTEGER NOT NULL UNIQUE)"""
        )
        db.execSQL(
            """CREATE TABLE questions (
                question_id INTEGER PRIMARY KEY AUTOINCREMENT,
                quiz_id INTEGER NOT NULL REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
                question_text TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE choices (
                choice_id INTEGER PRIMARY KEY AUTOINCREMENT,
                question_id INTEGER NOT NULL REFERENCES questions(question_id) ON DELETE CASCADE,
                choice_text TEXT NOT NULL,
                is_correct INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE attempts (
                attempt_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(user_id),
                quiz_id INTEGER NOT NULL REFERENCES quizzes(quiz_id),
                submitted_at INTEGER NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE answers (
                answer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                attempt_id INTEGER NOT NULL REFERENCES attempts(attempt_id) ON DELETE CASCADE,
                choice_id INTEGER NOT NULL REFERENCES choices(choice_id))"""
        )
        SeedData.insert(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf("answers", "attempts", "choices", "questions", "quizzes", "users").forEach {
            db.execSQL("DROP TABLE IF EXISTS $it")
        }
        onCreate(db)
    }

    companion object {
        const val DB_NAME = "kotlin_quiz_master.db"
        const val DB_VERSION = 1
    }
}
