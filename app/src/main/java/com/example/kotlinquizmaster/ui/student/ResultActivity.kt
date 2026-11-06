package com.example.kotlinquizmaster.ui.student

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityResultBinding
import com.example.kotlinquizmaster.databinding.ItemWrongAnswerBinding
import com.example.kotlinquizmaster.model.Quiz

/** Score, pass or fail, and a review of the wrong answers. */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val attempt = QuizRepository(this).attempt(intent.getLongExtra(EXTRA_ATTEMPT_ID, -1L)) ?: run { finish(); return }
        val quiz = attempt.quiz
        val passed = attempt.isPassed()

        binding.quizTitle.text = "Level ${quiz.level} · ${quiz.title}"
        binding.scoreText.text = "${attempt.score()}/${Quiz.QUESTION_COUNT}"
        binding.verdict.text = if (passed) "Passed" else "Not passed yet"
        binding.verdict.setTextColor(ContextCompat.getColor(this, if (passed) R.color.pass_green else android.R.color.holo_red_dark))
        binding.message.text = when {
            passed && quiz.level < Quiz.LEVELS.last() -> "Great work! Level ${quiz.level + 1} is your next step."
            passed -> "Excellent! You have passed the final level."
            else -> "You need 8 correct answers to pass. Review the questions below and try again."
        }

        val wrong = attempt.wrongAnswers()
        binding.wrongHeading.isVisible = wrong.isNotEmpty()
        wrong.forEach { a ->
            val card = ItemWrongAnswerBinding.inflate(layoutInflater, binding.wrongContainer, true)
            card.questionText.text = a.question.text
            card.yourAnswer.text = "${getString(R.string.your_answer)}: ${a.selectedChoice.text}"
            card.correctAnswer.text = "${getString(R.string.correct_answer)}: ${a.question.correctChoice().text}"
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.backButton.setOnClickListener { finish() }
        binding.retryButton.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java).putExtra(QuizActivity.EXTRA_QUIZ_ID, quiz.quizId))
            finish()
        }
    }

    companion object {
        const val EXTRA_ATTEMPT_ID = "attempt_id"
    }
}
