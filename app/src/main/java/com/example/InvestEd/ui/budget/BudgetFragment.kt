package com.example.InvestEd.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentBudgetBinding
import com.example.InvestEd.viewmodel.BudgetViewModel

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var adapter: BudgetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        // FAB removed — no click listener needed

        binding.tvSavingsPercent.setOnClickListener {
            viewModel.setFilter(BudgetViewModel.FilterType.SAVINGS)
        }
        binding.tvSpendingPercent.setOnClickListener {
            viewModel.setFilter(BudgetViewModel.FilterType.SPENDING)
        }
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter()
        binding.rvBudgetEntries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudgetEntries.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.budgetState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BudgetViewModel.BudgetState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is BudgetViewModel.BudgetState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val s = state.summary
                    binding.tvTotalAmount.text     = "₱" + String.format("%,.2f", s.totalAmount)
                    binding.tvSavingsPercent.text  = "Savings: ${s.savingsPercent}%"
                    binding.tvSpendingPercent.text = "Spending: ${s.spendingPercent}%"
                    adapter.submitList(s.entries)
                }
                is BudgetViewModel.BudgetState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.activeFilter.observe(viewLifecycleOwner) { filter ->
            updateFilterUI(filter)
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BudgetViewModel.ActionState.Success -> {
                    Toast.makeText(requireContext(), "Entry added!", Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                is BudgetViewModel.ActionState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                else -> Unit
            }
        }
    }

    private fun updateFilterUI(filter: BudgetViewModel.FilterType) {
        when (filter) {
            BudgetViewModel.FilterType.SAVINGS -> {
                binding.tvSavingsPercent.setTextColor(Color.parseColor("#4CAF50"))
                binding.tvSavingsPercent.paint.isUnderlineText = true
                binding.tvSpendingPercent.setTextColor(Color.parseColor("#F44336"))
                binding.tvSpendingPercent.paint.isUnderlineText = false
            }
            BudgetViewModel.FilterType.SPENDING -> {
                binding.tvSpendingPercent.setTextColor(Color.parseColor("#F44336"))
                binding.tvSpendingPercent.paint.isUnderlineText = true
                binding.tvSavingsPercent.setTextColor(Color.parseColor("#4CAF50"))
                binding.tvSavingsPercent.paint.isUnderlineText = false
            }
            BudgetViewModel.FilterType.ALL -> {
                binding.tvSavingsPercent.setTextColor(Color.parseColor("#4CAF50"))
                binding.tvSavingsPercent.paint.isUnderlineText = false
                binding.tvSpendingPercent.setTextColor(Color.parseColor("#F44336"))
                binding.tvSpendingPercent.paint.isUnderlineText = false
            }
        }
        binding.tvSavingsPercent.invalidate()
        binding.tvSpendingPercent.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}