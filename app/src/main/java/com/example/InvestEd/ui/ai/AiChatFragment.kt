// ui/ai/AiChatFragment.kt
package com.example.InvestEd.ui.ai

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentAiChatBinding
import com.example.InvestEd.viewmodel.AiChatViewModel

class AiChatFragment : Fragment() {

    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AiChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupChips()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).also {
            it.stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages.toList()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isTyping.observe(viewLifecycleOwner) { isTyping ->
            binding.tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
        }
    }

    private fun setupChips() {
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.chipSavings.setOnClickListener { viewModel.sendMessage("How should I start saving?") }
        binding.chipInvest.setOnClickListener { viewModel.sendMessage("How do I start investing?") }
        binding.chipBudget.setOnClickListener { viewModel.sendMessage("Help me with budgeting") }
        binding.chipGoals.setOnClickListener { viewModel.sendMessage("How do I set financial goals?") }
        binding.chipEmergency.setOnClickListener { viewModel.sendMessage("What is an emergency fund?") }
        binding.chipNextSteps.setOnClickListener { viewModel.sendMessage("What should I do next with my money?") }
        binding.chipWeeklyPlan.setOnClickListener { viewModel.sendMessage("Can you make me a simple weekly savings plan?") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}