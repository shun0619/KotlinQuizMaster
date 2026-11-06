package com.example.kotlinquizmaster.ui.instructor

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityQuizEditorBinding
import com.example.kotlinquizmaster.model.QuestionDraft
import com.example.kotlinquizmaster.model.Quiz
import com.example.kotlinquizmaster.util.Session
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Create Quiz and Edit Quiz share this screen.
 * Create: choose a free level and write 10 questions; "Save quiz" appears on question 10.
 * Edit: the level is locked and Save in the app bar can be tapped at any time.
 */
class QuizEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizEditorBinding
    private lateinit var repository: QuizRepository
    private lateinit var radios: List<RadioButton>
    private lateinit var inputs: List<EditText>

    private var editQuizId = -1L
    private val isEditMode get() = editQuizId != -1L
    private var levels: List<Int> = emptyList()
    private val drafts = MutableList(Quiz.QUESTION_COUNT) { QuestionDraft() }
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)
        radios = listOf(binding.correct1, binding.correct2, binding.correct3, binding.correct4)
        inputs = listOf(binding.choice1, binding.choice2, binding.choice3, binding.choice4)
        editQuizId = intent.getLongExtra(EXTRA_QUIZ_ID, -1L)

        if (isEditMode) setUpEdit() else setUpCreate()

        // Only one radio button can be the correct answer.
        radios.forEachIndexed { i, radio ->
            radio.setOnClickListener { radios.forEachIndexed { j, r -> r.isChecked = i == j } }
        }
        binding.previousButton.setOnClickListener { move(-1) }
        binding.nextButton.setOnClickListener {
            if (!isEditMode && index == Quiz.QUESTION_COUNT - 1) save() else move(1)
        }
        binding.toolbar.setNavigationOnClickListener { confirmDiscard() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmDiscard()
        })
        showQuestion()
    }

    private fun setUpCreate() {
        binding.toolbar.title = getString(R.string.create_quiz)
        levels = repository.availableLevels()
        if (levels.isEmpty()) {
            Toast.makeText(this, "Every level already has a quiz.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        binding.levelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, levels.map { "Level $it" })
        binding.note.text = getString(R.string.create_note)
    }

    private fun setUpEdit() {
        binding.toolbar.title = getString(R.string.edit_quiz)
        binding.toolbar.inflateMenu(R.menu.menu_editor)
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_save) { save(); true } else false
        }
        val quiz = repository.loadQuiz(editQuizId)
        if (quiz == null) { finish(); return }
        binding.titleInput.setText(quiz.title)
        levels = listOf(quiz.level)
        binding.levelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Level ${quiz.level} (locked)"))
        binding.levelSpinner.isEnabled = false
        quiz.questions.take(Quiz.QUESTION_COUNT).forEachIndexed { i, q -> drafts[i] = QuestionDraft.from(q) }
        binding.note.text = getString(R.string.edit_note)
    }

    /** Copies what is on screen into the current draft. */
    private fun storeQuestion() {
        val d = drafts[index]
        d.text = binding.questionInput.text?.toString().orEmpty()
        inputs.forEachIndexed { i, input -> d.choiceTexts[i] = input.text?.toString().orEmpty() }
        d.correctIndex = radios.indexOfFirst { it.isChecked }
    }

    private fun showQuestion() {
        val d = drafts[index]
        binding.progressText.text = "Question ${index + 1} of ${Quiz.QUESTION_COUNT}"
        binding.progress.setProgressCompat(index + 1, true)
        binding.questionInput.setText(d.text)
        inputs.forEachIndexed { i, input -> input.setText(d.choiceTexts[i]) }
        radios.forEachIndexed { i, r -> r.isChecked = i == d.correctIndex }
        binding.previousButton.isEnabled = index > 0
        val last = index == Quiz.QUESTION_COUNT - 1
        binding.nextButton.text = when {
            !isEditMode && last -> getString(R.string.save_quiz)
            else -> getString(R.string.next_question)
        }
        binding.nextButton.isEnabled = !(isEditMode && last)
        binding.questionInput.requestFocus()
    }

    private fun move(step: Int) {
        storeQuestion()
        if (step > 0 && !drafts[index].isComplete()) {
            Toast.makeText(this, "Fill in the question, all four choices and the correct answer.", Toast.LENGTH_SHORT).show()
            return
        }
        index = (index + step).coerceIn(0, Quiz.QUESTION_COUNT - 1)
        showQuestion()
    }

    private fun save() {
        storeQuestion()
        val title = binding.titleInput.text?.toString().orEmpty().trim()
        binding.titleLayout.error = null
        if (title.isEmpty()) { binding.titleLayout.error = "Enter a quiz title"; return }
        val firstIncomplete = drafts.indexOfFirst { !it.isComplete() }
        if (firstIncomplete != -1) {
            index = firstIncomplete
            showQuestion()
            Toast.makeText(this, "Question ${firstIncomplete + 1} is not complete.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isEditMode) {
            repository.updateQuiz(editQuizId, title, drafts)
            Toast.makeText(this, "Quiz updated", Toast.LENGTH_SHORT).show()
        } else {
            val level = levels[binding.levelSpinner.selectedItemPosition]
            repository.createQuiz(Session.userId(this), title, level, drafts)
            Toast.makeText(this, "Level $level quiz saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun confirmDiscard() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.discard_title)
            .setMessage(R.string.discard_message)
            .setPositiveButton(R.string.discard) { _, _ -> finish() }
            .setNegativeButton(R.string.keep_editing, null)
            .show()
    }

    companion object {
        const val EXTRA_QUIZ_ID = "quiz_id"
    }
}
