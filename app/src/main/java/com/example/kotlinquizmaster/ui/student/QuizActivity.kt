package com.example.kotlinquizmaster.ui.student

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityQuizBinding
import com.example.kotlinquizmaster.model.Question
import com.example.kotlinquizmaster.model.Quiz
import com.example.kotlinquizmaster.util.Session
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Takes a quiz: one question per page, in random order. */
class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private lateinit var repository: QuizRepository
    private lateinit var radios: List<RadioButton>
    private lateinit var quiz: Quiz
    private var questions: List<Question> = emptyList()
    private lateinit var selected: LongArray   // chosen choice id per question, 0 = none
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)
        radios = listOf(binding.choice0, binding.choice1, binding.choice2, binding.choice3)

        quiz = repository.loadQuiz(intent.getLongExtra(EXTRA_QUIZ_ID, -1L)) ?: run { finish(); return }
        binding.toolbar.title = "Level ${quiz.level} · ${quiz.title}"
        questions = quiz.shuffledQuestions()
        selected = LongArray(questions.size)

        // Event 3: radio group selection change
        binding.choiceGroup.setOnCheckedChangeListener { _, checkedId ->
            val i = radios.indexOfFirst { it.id == checkedId }
            if (i >= 0) selected[index] = questions[index].choices[i].choiceId
            binding.nextButton.isEnabled = i >= 0
        }
        binding.backButton.setOnClickListener { if (index > 0) { index--; showQuestion() } }
        binding.nextButton.setOnClickListener {
            if (index < questions.size - 1) { index++; showQuestion() } else submit()
        }
        binding.toolbar.setNavigationOnClickListener { confirmLeave() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmLeave()
        })
        showQuestion()
    }

    private fun showQuestion() {
        val q = questions[index]
        binding.progressText.text = "Question ${index + 1} of ${questions.size}"
        binding.progress.max = questions.size
        binding.progress.setProgressCompat(index + 1, true)
        binding.questionText.text = q.text
        binding.choiceGroup.clearCheck()
        q.choices.forEachIndexed { i, c ->
            radios[i].text = c.text
            if (c.choiceId == selected[index]) radios[i].isChecked = true
        }
        binding.backButton.isEnabled = index > 0
        binding.nextButton.text = getString(if (index == questions.size - 1) R.string.submit else R.string.next)
        binding.nextButton.isEnabled = selected[index] != 0L
        binding.scroll.scrollTo(0, 0)
    }

    private fun submit() {
        val attemptId = repository.saveAttempt(Session.userId(this), quiz.quizId, selected.toList())
        startActivity(Intent(this, ResultActivity::class.java).putExtra(ResultActivity.EXTRA_ATTEMPT_ID, attemptId))
        finish()
    }

    private fun confirmLeave() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.leave_quiz_title)
            .setMessage(R.string.leave_quiz_message)
            .setPositiveButton(R.string.leave) { _, _ -> finish() }
            .setNegativeButton(R.string.stay, null)
            .show()
    }

    companion object {
        const val EXTRA_QUIZ_ID = "quiz_id"
    }
}
