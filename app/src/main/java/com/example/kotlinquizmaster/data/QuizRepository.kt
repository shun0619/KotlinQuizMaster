package com.example.kotlinquizmaster.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.kotlinquizmaster.model.Answer
import com.example.kotlinquizmaster.model.Attempt
import com.example.kotlinquizmaster.model.Choice
import com.example.kotlinquizmaster.model.Question
import com.example.kotlinquizmaster.model.QuestionDraft
import com.example.kotlinquizmaster.model.QuestionStat
import com.example.kotlinquizmaster.model.Quiz
import com.example.kotlinquizmaster.model.Role
import com.example.kotlinquizmaster.model.User
import com.example.kotlinquizmaster.util.Security

/** All reading and writing of the database goes through this class. */
class QuizRepository(context: Context) {

    private val helper = DatabaseHelper(context.applicationContext)
    private val db: SQLiteDatabase get() = helper.writableDatabase

    // ---------- users ----------

    /** Returns the user when the email and password match, otherwise null. */
    fun login(email: String, password: String): User? {
        val user = findUserByEmail(email) ?: return null
        return if (user.checkPassword(password)) user else null
    }

    fun findUserByEmail(email: String): User? {
        db.rawQuery(
            "SELECT user_id, name, email, password_hash, role FROM users WHERE LOWER(email) = LOWER(?)",
            arrayOf(email.trim())
        ).use { c ->
            if (!c.moveToFirst()) return null
            return User.create(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), Role.valueOf(c.getString(4)))
        }
    }

    /** Returns the new user's id, or -1 if the email is already registered. */
    fun registerUser(name: String, email: String, password: String, role: Role): Long {
        if (findUserByEmail(email) != null) return -1L
        val values = ContentValues().apply {
            put("name", name.trim())
            put("email", email.trim())
            put("password_hash", Security.hash(password))
            put("role", role.name)
        }
        return db.insert("users", null, values)
    }

    fun instructorNames(): List<String> {
        val list = mutableListOf<String>()
        db.rawQuery("SELECT name, email FROM users WHERE role = ? ORDER BY name", arrayOf(Role.INSTRUCTOR.name)).use { c ->
            while (c.moveToNext()) list.add("${c.getString(0)} (${c.getString(1)})")
        }
        return list
    }

    fun firstInstructorEmail(): String? {
        db.rawQuery("SELECT email FROM users WHERE role = ? ORDER BY user_id LIMIT 1", arrayOf(Role.INSTRUCTOR.name)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else null
        }
    }

    // ---------- quizzes ----------

    /** All quizzes ordered by level, without their questions. */
    fun quizzes(): List<Quiz> {
        val list = mutableListOf<Quiz>()
        db.rawQuery(
            """SELECT q.quiz_id, q.title, q.level, q.created_by,
                      (SELECT COUNT(*) FROM attempts a WHERE a.quiz_id = q.quiz_id)
               FROM quizzes q ORDER BY q.level""",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(Quiz(c.getLong(0), c.getString(1), c.getInt(2), c.getLong(3), attemptCount = c.getInt(4)))
            }
        }
        return list
    }

    /** Levels that do not have a quiz yet (only these can be chosen when creating). */
    fun availableLevels(): List<Int> {
        val used = quizzes().map { it.level }.toSet()
        return Quiz.LEVELS.filter { it !in used }
    }

    /** One quiz with all of its questions and choices. */
    fun loadQuiz(quizId: Long): Quiz? {
        val header = db.rawQuery("SELECT title, level, created_by FROM quizzes WHERE quiz_id = ?", arrayOf(quizId.toString())).use { c ->
            if (c.moveToFirst()) Triple(c.getString(0), c.getInt(1), c.getLong(2)) else null
        } ?: return null

        val questions = mutableListOf<Question>()
        db.rawQuery("SELECT question_id, question_text FROM questions WHERE quiz_id = ? ORDER BY question_id", arrayOf(quizId.toString())).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                questions.add(Question(id, c.getString(1), choices(id)))
            }
        }
        return Quiz(quizId, header.first, header.second, header.third, questions)
    }

    private fun choices(questionId: Long): List<Choice> {
        val list = mutableListOf<Choice>()
        db.rawQuery("SELECT choice_id, choice_text, is_correct FROM choices WHERE question_id = ? ORDER BY choice_id", arrayOf(questionId.toString())).use { c ->
            while (c.moveToNext()) list.add(Choice(c.getLong(0), c.getString(1), c.getInt(2) == 1))
        }
        return list
    }

    /** Creates a quiz with its 10 questions in one transaction. Returns the new quiz id. */
    fun createQuiz(createdBy: Long, title: String, level: Int, drafts: List<QuestionDraft>): Long {
        db.beginTransaction()
        try {
            val quizId = db.insert("quizzes", null, ContentValues().apply {
                put("created_by", createdBy)
                put("title", title.trim())
                put("level", level)
            })
            for (d in drafts) {
                val questionId = db.insert("questions", null, ContentValues().apply {
                    put("quiz_id", quizId)
                    put("question_text", d.text.trim())
                })
                d.choiceTexts.forEachIndexed { i, text ->
                    db.insert("choices", null, ContentValues().apply {
                        put("question_id", questionId)
                        put("choice_text", text.trim())
                        put("is_correct", if (i == d.correctIndex) 1 else 0)
                    })
                }
            }
            db.setTransactionSuccessful()
            return quizId
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Updates the title, questions and choices in place. The ids stay the same, so past answers
     * stay linked and past results are recalculated with the new correct answers.
     */
    fun updateQuiz(quizId: Long, title: String, drafts: List<QuestionDraft>) {
        db.beginTransaction()
        try {
            db.update("quizzes", ContentValues().apply { put("title", title.trim()) }, "quiz_id = ?", arrayOf(quizId.toString()))
            for (d in drafts) {
                db.update("questions", ContentValues().apply { put("question_text", d.text.trim()) },
                    "question_id = ?", arrayOf(d.questionId.toString()))
                d.choiceIds.forEachIndexed { i, choiceId ->
                    db.update("choices", ContentValues().apply {
                        put("choice_text", d.choiceTexts[i].trim())
                        put("is_correct", if (i == d.correctIndex) 1 else 0)
                    }, "choice_id = ?", arrayOf(choiceId.toString()))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---------- attempts ----------

    /** Saves a submitted quiz. selectedChoiceIds holds one choice id per question. */
    fun saveAttempt(userId: Long, quizId: Long, selectedChoiceIds: List<Long>): Long {
        db.beginTransaction()
        try {
            val attemptId = db.insert("attempts", null, ContentValues().apply {
                put("user_id", userId)
                put("quiz_id", quizId)
                put("submitted_at", System.currentTimeMillis())
            })
            for (choiceId in selectedChoiceIds) {
                db.insert("answers", null, ContentValues().apply {
                    put("attempt_id", attemptId)
                    put("choice_id", choiceId)
                })
            }
            db.setTransactionSuccessful()
            return attemptId
        } finally {
            db.endTransaction()
        }
    }

    /** Loads an attempt with its answers. The score is calculated, not stored. */
    fun attempt(attemptId: Long, quiz: Quiz? = null): Attempt? {
        val header = db.rawQuery(
            """SELECT a.quiz_id, a.submitted_at, u.name FROM attempts a
               JOIN users u ON u.user_id = a.user_id WHERE a.attempt_id = ?""",
            arrayOf(attemptId.toString())
        ).use { c -> if (c.moveToFirst()) Triple(c.getLong(0), c.getLong(1), c.getString(2)) else null } ?: return null

        val fullQuiz = quiz ?: loadQuiz(header.first) ?: return null
        val choiceToQuestion = HashMap<Long, Question>()
        fullQuiz.questions.forEach { q -> q.choices.forEach { choiceToQuestion[it.choiceId] = q } }

        val answers = mutableListOf<Answer>()
        db.rawQuery("SELECT answer_id, choice_id FROM answers WHERE attempt_id = ? ORDER BY answer_id", arrayOf(attemptId.toString())).use { c ->
            while (c.moveToNext()) {
                val question = choiceToQuestion[c.getLong(1)] ?: continue
                val choice = question.choices.first { it.choiceId == c.getLong(1) }
                answers.add(Answer(c.getLong(0), question, choice))
            }
        }
        return Attempt(attemptId, header.third, fullQuiz, header.second, answers)
    }

    /** Score of the student's most recent attempt at a quiz, or null if not taken. */
    fun latestScore(userId: Long, quizId: Long): Int? {
        val attemptId = db.rawQuery(
            "SELECT attempt_id FROM attempts WHERE user_id = ? AND quiz_id = ? ORDER BY attempt_id DESC LIMIT 1",
            arrayOf(userId.toString(), quizId.toString())
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else null } ?: return null
        return attempt(attemptId)?.score()
    }

    /** True if the student has passed the quiz at least once (8 or more correct). */
    fun hasPassed(userId: Long, quizId: Long): Boolean {
        val ids = mutableListOf<Long>()
        db.rawQuery(
            "SELECT attempt_id FROM attempts WHERE user_id = ? AND quiz_id = ?",
            arrayOf(userId.toString(), quizId.toString())
        ).use { c -> while (c.moveToNext()) ids.add(c.getLong(0)) }
        val quiz = loadQuiz(quizId) ?: return false
        return ids.any { attempt(it, quiz)?.isPassed() == true }
    }

    /** Every attempt at a quiz, newest first. */
    fun attemptsForQuiz(quiz: Quiz): List<Attempt> {
        val ids = mutableListOf<Long>()
        db.rawQuery("SELECT attempt_id FROM attempts WHERE quiz_id = ? ORDER BY attempt_id DESC", arrayOf(quiz.quizId.toString())).use { c ->
            while (c.moveToNext()) ids.add(c.getLong(0))
        }
        return ids.mapNotNull { attempt(it, quiz) }
    }

    /** Correct-answer rate (0-100) for each question of the quiz. */
    fun questionStats(quiz: Quiz, attempts: List<Attempt>): List<QuestionStat> =
        quiz.questions.mapIndexed { i, q ->
            val answered = attempts.mapNotNull { a -> a.answers.firstOrNull { it.question.questionId == q.questionId } }
            val rate = if (answered.isEmpty()) 0 else answered.count { it.isCorrect() } * 100 / answered.size
            QuestionStat(i + 1, q.text, rate)
        }
}
