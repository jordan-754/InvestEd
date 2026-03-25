// ui/learning/LessonAdapter.kt
package com.example.InvestEd.ui.learning

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.InvestEd.databinding.ItemLessonBinding
import com.example.InvestEd.model.Lesson

class LessonAdapter(
    private val onLessonClick: (Lesson) -> Unit,
    private var completedLessons: Set<String>
) : ListAdapter<Lesson, LessonAdapter.LessonViewHolder>(DiffCallback()) {

    fun updateCompletedLessons(completed: Set<String>) {
        completedLessons = completed
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonViewHolder(
        private val binding: ItemLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lesson: Lesson) {
            val isCompleted = completedLessons.contains(lesson.id)

            binding.tvLessonTitle.text       = lesson.title
            binding.tvLessonDescription.text = lesson.description
            binding.tvDuration.text          = lesson.duration
            binding.tvPoints.text            = "+${lesson.points} pts"

            // Show completed badge using the layout container (no icon placeholder)
            if (isCompleted) {
                binding.layoutCompleted.visibility = View.VISIBLE
            } else {
                binding.layoutCompleted.visibility = View.GONE
            }

            binding.root.setOnClickListener { onLessonClick(lesson) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(a: Lesson, b: Lesson)    = a.id == b.id
        override fun areContentsTheSame(a: Lesson, b: Lesson) = a == b
    }
}
