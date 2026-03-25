package com.example.InvestEd.ui.learning

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels   // ← KEY FIX: shared across fragments
import com.example.InvestEd.databinding.FragmentLessonDetailBinding
import com.example.InvestEd.viewmodel.LearningViewModel

class LessonDetailFragment : Fragment() {

    private var _binding: FragmentLessonDetailBinding? = null
    private val binding get() = _binding!!

    // ✅ FIX: use activityViewModels so this shares the SAME instance
    // as LearningFragment — the completedLessons guard will now work correctly
    private val viewModel: LearningViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lessonId = arguments?.getString("lessonId")    ?: ""
        val title    = arguments?.getString("lessonTitle") ?: "No Title"
        val content  = arguments?.getString("lessonContent") ?: "No Content available."
        val points   = arguments?.getInt("lessonPoints")   ?: 0

        // ✅ FIX: always check live completedLessons from shared ViewModel
        // instead of relying on the stale `isCompleted` bundle argument
        val isCompleted = viewModel.completedLessons.value?.contains(lessonId) == true

        binding.tvLessonTitle.text   = title
        binding.tvLessonContent.text = content
        binding.tvPoints.text        = "+$points pts"

        updateButtonState(isCompleted)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnComplete.setOnClickListener {
            // Extra guard: check again before calling — prevents double-tap races
            val alreadyDone = viewModel.completedLessons.value?.contains(lessonId) == true
            if (alreadyDone) {
                updateButtonState(true)
                return@setOnClickListener
            }
            viewModel.completeLesson(lessonId, points, title)
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LearningViewModel.ActionState.Success -> {
                    updateButtonState(true)
                    Toast.makeText(
                        requireContext(),
                        "+${state.points} pts earned!",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetActionState()
                }
                is LearningViewModel.ActionState.AlreadyDone -> {
                    updateButtonState(true)
                    Toast.makeText(
                        requireContext(),
                        "Lesson already completed today",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetActionState()
                }
                is LearningViewModel.ActionState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                else -> Unit
            }
        }

        // ✅ FIX: observe live completedLessons so button updates
        // immediately if another fragment already completed this lesson
        viewModel.completedLessons.observe(viewLifecycleOwner) { completed ->
            if (completed.contains(lessonId)) {
                updateButtonState(true)
            }
        }
    }

    private fun updateButtonState(completed: Boolean) {
        binding.btnComplete.text      = if (completed) "Completed" else "Mark as Complete"
        binding.btnComplete.isEnabled = !completed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}