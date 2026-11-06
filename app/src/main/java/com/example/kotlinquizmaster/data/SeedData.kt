package com.example.kotlinquizmaster.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.kotlinquizmaster.model.Role
import com.example.kotlinquizmaster.util.Security

/**
 * Data written once, when the database is first created:
 * the pre-registered admin, a demo instructor and student, and one sample quiz per level.
 */
object SeedData {

    const val ADMIN_EMAIL = "admin@ais.ac.nz"
    const val ADMIN_PASSWORD = "Admin123!"
    const val INSTRUCTOR_EMAIL = "instructor@ais.ac.nz"
    const val INSTRUCTOR_PASSWORD = "Instructor123!"
    const val STUDENT_EMAIL = "student@ais.ac.nz"
    const val STUDENT_PASSWORD = "Student123!"

    private class Q(val text: String, val choices: List<String>, val correct: Int)

    private val level1 = listOf(
        Q("Which keyword declares a read-only variable?", listOf("val", "var", "let", "const"), 0),
        Q("Which keyword declares a variable whose value can change?", listOf("val", "var", "fun", "when"), 1),
        Q("What does println(\"Hello\") do?", listOf("Reads input from the user", "Prints Hello followed by a new line", "Declares a string", "Nothing"), 1),
        Q("What is the type of the literal 42?", listOf("Double", "String", "Int", "Long"), 2),
        Q("Which symbol starts a single-line comment?", listOf("#", "//", "<!--", "--"), 1),
        Q("What is the result of 7 / 2 when both values are Int?", listOf("3.5", "4", "3", "An error"), 2),
        Q("Which string template shows the value of the variable name?", listOf("\"Hello name\"", "\"Hello \$name\"", "\"Hello {name}\"", "\"Hello + name\""), 1),
        Q("Which type can hold the value null?", listOf("String", "Int", "Boolean", "String?"), 3),
        Q("Which function is the entry point of a Kotlin program?", listOf("main()", "start()", "run()", "init()"), 0),
        Q("What does the !! operator do?", listOf("Creates a list", "Compares two values", "Negates a Boolean twice", "Asserts that a value is not null"), 3)
    )

    private val level2 = listOf(
        Q("Which expression can replace a long if-else-if chain?", listOf("when", "switch", "case", "select"), 0),
        Q("What does for (i in 1..3) println(i) print?", listOf("0 1 2", "1 2 3", "1 2", "0 1 2 3"), 1),
        Q("Which range does NOT include its end value?", listOf("1..5", "1 until 5", "5 downTo 1", "1..5 step 2"), 1),
        Q("Which keyword leaves a loop immediately?", listOf("continue", "return", "break", "exit"), 2),
        Q("What does continue do inside a loop?", listOf("Stops the program", "Skips to the next iteration", "Restarts the loop", "Ends the function"), 1),
        Q("In Kotlin, if can be used as ...", listOf("only a statement", "a loop", "a class", "an expression that returns a value"), 3),
        Q("Which loop runs its body at least once before checking the condition?", listOf("while", "for", "do-while", "repeat"), 2),
        Q("What does repeat(3) { print(\"A\") } print?", listOf("A", "AA", "AAA", "Nothing"), 2),
        Q("Which operator means logical AND?", listOf("&", "&&", "||", "!"), 1),
        Q("val x = 5; println(if (x > 3) \"big\" else \"small\") prints ...", listOf("big", "small", "5", "An error"), 0)
    )

    private val level3 = listOf(
        Q("Which keyword declares a function?", listOf("func", "def", "fun", "function"), 2),
        Q("fun double(n: Int) = n * 2. What does double(4) return?", listOf("4", "6", "8", "16"), 2),
        Q("Which function creates a read-only list?", listOf("mutableListOf()", "listOf()", "arrayListOf()", "setOf()"), 1),
        Q("What is the index of the first element in a list?", listOf("0", "1", "-1", "It depends on the list"), 0),
        Q("Which collection stores key-value pairs?", listOf("List", "Set", "Map", "Array"), 2),
        Q("What does listOf(1, 2, 3).map { it * 2 } return?", listOf("[1, 2, 3]", "[2, 4, 6]", "[3, 4, 5]", "12"), 1),
        Q("What does listOf(1, 2, 3, 4).filter { it % 2 == 0 } return?", listOf("[1, 3]", "[2, 4]", "[1, 2]", "[4]"), 1),
        Q("What is a default argument?", listOf("An argument that must always be passed", "A value used when the caller leaves the argument out", "A global variable", "A return value"), 1),
        Q("Inside a lambda with one parameter, which name refers to that parameter?", listOf("it", "this", "self", "param"), 0),
        Q("What does setOf(1, 1, 2).size return?", listOf("3", "1", "An error", "2"), 3)
    )

    fun insert(db: SQLiteDatabase) {
        insertUser(db, "Administrator", ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN)
        val instructorId = insertUser(db, "Priya Sharma", INSTRUCTOR_EMAIL, INSTRUCTOR_PASSWORD, Role.INSTRUCTOR)
        insertUser(db, "Aroha Ngata", STUDENT_EMAIL, STUDENT_PASSWORD, Role.STUDENT)

        insertQuiz(db, instructorId, "Kotlin foundations", 1, level1)
        insertQuiz(db, instructorId, "Decisions & loops", 2, level2)
        insertQuiz(db, instructorId, "Functions & collections", 3, level3)
    }

    private fun insertUser(db: SQLiteDatabase, name: String, email: String, password: String, role: Role): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("password_hash", Security.hash(password))
            put("role", role.name)
        }
        return db.insert("users", null, values)
    }

    private fun insertQuiz(db: SQLiteDatabase, createdBy: Long, title: String, level: Int, questions: List<Q>) {
        val quizId = db.insert("quizzes", null, ContentValues().apply {
            put("created_by", createdBy)
            put("title", title)
            put("level", level)
        })
        for (q in questions) {
            val questionId = db.insert("questions", null, ContentValues().apply {
                put("quiz_id", quizId)
                put("question_text", q.text)
            })
            q.choices.forEachIndexed { i, text ->
                db.insert("choices", null, ContentValues().apply {
                    put("question_id", questionId)
                    put("choice_text", text)
                    put("is_correct", if (i == q.correct) 1 else 0)
                })
            }
        }
    }
}
