package com.example.InvestEd.ui.learning

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels   // ← KEY FIX: shared ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentLearningBinding
import com.example.InvestEd.viewmodel.LearningViewModel
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R

class LearningFragment : Fragment() {

    private var _binding: FragmentLearningBinding? = null
    private val binding get() = _binding!!

    // ✅ FIX: activityViewModels — same instance as LessonDetailFragment
    private val viewModel: LearningViewModel by activityViewModels()
    private lateinit var adapter: LessonAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh completed status when returning from lesson detail
        viewModel.completedLessons.value?.let { completed ->
            adapter.updateCompletedLessons(completed)
        }
    }

    private fun setupRecyclerView() {
        adapter = LessonAdapter(
            onLessonClick    = { lesson -> openLessonDetail(lesson) },
            completedLessons = emptySet()
        )
        binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLessons.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.learningState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LearningViewModel.LearningState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.rvLessons.visibility   = View.GONE
                }
                is LearningViewModel.LearningState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.rvLessons.visibility   = View.VISIBLE
                    adapter.submitList(state.lessons)
                }
                is LearningViewModel.LearningState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.VISIBLE
                    binding.rvLessons.visibility   = View.GONE
                }
                is LearningViewModel.LearningState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.completedLessons.observe(viewLifecycleOwner) { completed ->
            adapter.updateCompletedLessons(completed)
        }

        viewModel.completionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LearningViewModel.CompletionState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "🎉 +${state.points} pts earned for '${state.lessonTitle}'!",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetCompletionState()
                }
                is LearningViewModel.CompletionState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetCompletionState()
                }
                else -> Unit
            }
        }
    }

    private fun openLessonDetail(lesson: com.example.InvestEd.model.Lesson) {
        val bundle = Bundle().apply {
            putString("lessonId",      lesson.id)
            putString("lessonTitle",   lesson.title)
            putString("lessonContent", lesson.content)
            putString("lessonIcon",    lesson.icon)
            putString("lessonColor",   lesson.color)
            putInt   ("lessonPoints",  lesson.points)
            // ✅ NOTE: isCompleted is no longer passed — LessonDetailFragment
            // now reads it live from the shared ViewModel instead
        }
        findNavController().navigate(
            R.id.action_learningFragment_to_lessonDetailFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}