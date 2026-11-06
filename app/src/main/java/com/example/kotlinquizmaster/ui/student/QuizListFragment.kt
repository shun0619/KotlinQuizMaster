package com.example.kotlinquizmaster.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinquizmaster.data.QuizRepository
import com.example.kotlinquizmaster.databinding.FragmentQuizListBinding
import com.example.kotlinquizmaster.databinding.ItemStudentQuizBinding
import com.example.kotlinquizmaster.model.Attempt
import com.example.kotlinquizmaster.model.Quiz
import com.example.kotlinquizmaster.util.Session

/**
 * The quizzes a student can take, with their latest score.
 * A level is locked until the student has passed the level before it.
 */
class QuizListFragment : Fragment() {

    private class Row(val quiz: Quiz, val latestScore: Int?, val locked: Boolean, val previousLevel: Int?)

    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuizListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.greeting.text = "Kia ora, ${Session.userName(requireContext())}"
        binding.quizList.layoutManager = LinearLayoutManager(requireContext())
    }

    // Reload when coming back from a result, so a newly unlocked level appears at once.
    override fun onResume() {
        super.onResume()
        val context = requireContext()
        val repository = QuizRepository(context)
        val userId = Session.userId(context)

        val rows = mutableListOf<Row>()
        var previous: Quiz? = null
        var previousPassed = true
        for (quiz in repository.quizzes()) {           // already ordered by level
            val locked = previous != null && !previousPassed
            rows.add(Row(quiz, repository.latestScore(userId, quiz.quizId), locked, previous?.level))
            previousPassed = repository.hasPassed(userId, quiz.quizId)
            previous = quiz
        }

        binding.quizList.adapter = Adapter(rows) { row ->
            if (row.locked) {
                Toast.makeText(context, "Pass Level ${row.previousLevel} first to unlock this quiz.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(context, QuizActivity::class.java).putExtra(QuizActivity.EXTRA_QUIZ_ID, row.quiz.quizId))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class Adapter(private val rows: List<Row>, private val onClick: (Row) -> Unit) :
        RecyclerView.Adapter<Adapter.Holder>() {

        class Holder(val binding: ItemStudentQuizBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemStudentQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val row = rows[position]
            val score = row.latestScore
            holder.binding.levelBadge.text = "L${row.quiz.level}"
            holder.binding.quizTitle.text = row.quiz.title
            holder.binding.status.text = when {
                row.locked -> "Locked · pass Level ${row.previousLevel} to unlock"
                score == null -> "Not started · ${Quiz.QUESTION_COUNT} questions"
                score >= Attempt.PASS_SCORE -> "Passed · latest $score/${Quiz.QUESTION_COUNT}"
                else -> "Try again · latest $score/${Quiz.QUESTION_COUNT}"
            }
            holder.binding.root.alpha = if (row.locked) 0.5f else 1f
            holder.binding.root.setOnClickListener { onClick(row) }
        }
    }
}
