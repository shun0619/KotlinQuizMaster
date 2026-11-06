package com.example.kotlinquizmaster.ui.instructor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinquizmaster.R
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.ActivityInstructorHomeBinding
import com.example.kotlinquizmaster.databinding.ItemInstructorQuizBinding
import com.example.kotlinquizmaster.model.Quiz
import com.example.kotlinquizmaster.util.Session

/** Lists the quizzes (one per level). Create Quiz is shown only while a level is still free. */
class InstructorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstructorHomeBinding
    private lateinit var repository: QuizRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = QuizRepository(this)

        binding.greeting.text = "Kia ora, ${Session.userName(this)}"
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_logout) { Session.logout(this); true } else false
        }
        binding.quizList.layoutManager = LinearLayoutManager(this)
        binding.createQuizButton.setOnClickListener {
            startActivity(Intent(this, QuizEditorActivity::class.java))
        }
    }

    // Reload every time the screen comes back, so new or edited quizzes appear.
    override fun onResume() {
        super.onResume()
        val quizzes = repository.quizzes()
        val free = repository.availableLevels()
        binding.summary.text = "${quizzes.size} of ${Quiz.LEVELS.size} levels have a quiz"
        binding.createQuizButton.isVisible = free.isNotEmpty()
        binding.quizList.adapter = QuizAdapter(quizzes) { quiz ->
            startActivity(Intent(this, StatisticsActivity::class.java).putExtra(StatisticsActivity.EXTRA_QUIZ_ID, quiz.quizId))
        }
    }

    private class QuizAdapter(private val items: List<Quiz>, private val onClick: (Quiz) -> Unit) :
        RecyclerView.Adapter<QuizAdapter.Holder>() {

        class Holder(val binding: ItemInstructorQuizBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemInstructorQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val quiz = items[position]
            holder.binding.quizTitle.text = "Level ${quiz.level} · ${quiz.title}"
            holder.binding.quizInfo.text = "${Quiz.QUESTION_COUNT} questions · ${quiz.attemptCount} attempts"
            holder.binding.root.setOnClickListener { onClick(quiz) }
        }
    }
}
