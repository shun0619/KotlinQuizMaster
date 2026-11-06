package com.example.kotlinquizmaster.model

import com.example.kotlinquizmaster.util.Security

/** The three kinds of user. Stored in the role column of the users table. */
enum class Role { ADMIN, INSTRUCTOR, STUDENT }

/** Base class of every user (inheritance in the class diagram). */
abstract class User(
    val userId: Long,
    val name: String,
    val email: String,
    protected val passwordHash: String
) {
    abstract val role: Role

    fun checkPassword(password: String): Boolean = Security.hash(password) == passwordHash

    companion object {
        /** Creates the right subclass from a row of the users table. */
        fun create(userId: Long, name: String, email: String, passwordHash: String, role: Role): User =
            when (role) {
                Role.ADMIN -> Admin(userId, name, email, passwordHash)
                Role.INSTRUCTOR -> Instructor(userId, name, email, passwordHash)
                Role.STUDENT -> Student(userId, name, email, passwordHash)
            }
    }
}

class Admin(userId: Long, name: String, email: String, passwordHash: String) :
    User(userId, name, email, passwordHash) {
    override val role = Role.ADMIN
}

class Instructor(userId: Long, name: String, email: String, passwordHash: String) :
    User(userId, name, email, passwordHash) {
    override val role = Role.INSTRUCTOR
}

class Student(userId: Long, name: String, email: String, passwordHash: String) :
    User(userId, name, email, passwordHash) {
    override val role = Role.STUDENT
}

data class Choice(val choiceId: Long, val text: String, val isCorrect: Boolean)

data class Question(val questionId: Long, val text: String, val choices: List<Choice>) {
    fun correctChoice(): Choice = choices.first { it.isCorrect }
}

data class Quiz(
    val quizId: Long,
    val title: String,
    val level: Int,
    val createdBy: Long,
    val questions: List<Question> = emptyList(),
    val attemptCount: Int = 0
) {
    fun shuffledQuestions(): List<Question> = questions.shuffled()

    companion object {
        const val QUESTION_COUNT = 10
        const val CHOICE_COUNT = 4
        val LEVELS = listOf(1, 2, 3)
    }
}

data class Answer(val answerId: Long, val question: Question, val selectedChoice: Choice) {
    fun isCorrect(): Boolean = selectedChoice.isCorrect
}

data class Attempt(
    val attemptId: Long,
    val studentName: String,
    val quiz: Quiz,
    val submittedAt: Long,
    val answers: List<Answer>
) {
    fun score(): Int = answers.count { it.isCorrect() }
    fun isPassed(): Boolean = score() >= PASS_SCORE
    fun wrongAnswers(): List<Answer> = answers.filter { !it.isCorrect() }

    companion object {
        const val PASS_SCORE = 8
    }
}

/** Correct-answer rate of one question, used on the statistics screen. */
data class QuestionStat(val number: Int, val text: String, val correctRate: Int)

/** Editable copy of a question used by the Create / Edit Quiz screen. */
class QuestionDraft(
    var questionId: Long = 0,
    var text: String = "",
    val choiceIds: LongArray = LongArray(Quiz.CHOICE_COUNT),
    val choiceTexts: Array<String> = Array(Quiz.CHOICE_COUNT) { "" },
    var correctIndex: Int = -1
) {
    fun isComplete(): Boolean =
        text.isNotBlank() && choiceTexts.all { it.isNotBlank() } && correctIndex in 0 until Quiz.CHOICE_COUNT

    companion object {
        fun from(question: Question): QuestionDraft {
            val draft = QuestionDraft(questionId = question.questionId, text = question.text)
            question.choices.forEachIndexed { i, c ->
                if (i < Quiz.CHOICE_COUNT) {
                    draft.choiceIds[i] = c.choiceId
                    draft.choiceTexts[i] = c.text
                    if (c.isCorrect) draft.correctIndex = i
                }
            }
            return draft
        }
    }
}
