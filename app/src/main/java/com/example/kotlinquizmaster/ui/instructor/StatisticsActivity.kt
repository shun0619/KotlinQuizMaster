package com.example.kotlinquizmaster.ui.instructor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityStatisticsBinding
import com.example.kotlinquizmaster.databinding.ItemAttemptBinding
import com.example.kotlinquizmaster.databinding.ItemQuestionStatBinding
import com.example.kotlinquizmaster.model.Quiz

/** Results of every student and the correct-answer rate of each question. */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var repository: QuizRepository
    private var quizId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)
        quizId = intent.getLongExtra(EXTRA_QUIZ_ID, -1L)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.editQuizButton.setOnClickListener {
            startActivity(Intent(this, QuizEditorActivity::class.java).putExtra(QuizEditorActivity.EXTRA_QUIZ_ID, quizId))
        }
    }

    // Reload after editing, because results are recalculated with the new correct answers.
    override fun onResume() {
        super.onResume()
        val quiz = repository.loadQuiz(quizId) ?: run { finish(); return }
        val attempts = repository.attemptsForQuiz(quiz)

        binding.quizTitle.text = "Level ${quiz.level} · ${quiz.title}"
        val average = if (attempts.isEmpty()) 0.0 else attempts.map { it.score() }.average()
        val passRate = if (attempts.isEmpty()) 0 else attempts.count { it.isPassed() } * 100 / attempts.size
        binding.attemptsValue.text = "${attempts.size}\n${getString(R.string.attempts)}"
        binding.averageValue.text = "%.1f\n%s".format(average, getString(R.string.average))
        binding.passRateValue.text = "$passRate%\n${getString(R.string.pass_rate)}"

        binding.attemptContainer.removeAllViews()
        if (attempts.isEmpty()) {
            val row = ItemAttemptBinding.inflate(layoutInflater, binding.attemptContainer, true)
            row.studentName.text = getString(R.string.no_attempts)
        }
        attempts.forEach { a ->
            val row = ItemAttemptBinding.inflate(layoutInflater, binding.attemptContainer, true)
            row.studentName.text = a.studentName
            row.score.text = "${a.score()}/${Quiz.QUESTION_COUNT} · ${if (a.isPassed()) "Pass" else "Fail"}"
        }

        binding.statContainer.removeAllViews()
        // Lowest correct rate first, so questions that need improving stand out.
        repository.questionStats(quiz, attempts).sortedBy { it.correctRate }.forEach { s ->
            val row = ItemQuestionStatBinding.inflate(layoutInflater, binding.statContainer, true)
            row.questionText.text = "Q${s.number}. ${s.text}"
            row.rate.text = if (attempts.isEmpty()) "–" else "${s.correctRate}%"
            row.rateBar.progress = s.correctRate
        }
    }

    companion object {
        const val EXTRA_QUIZ_ID = "quiz_id"
    }
}
