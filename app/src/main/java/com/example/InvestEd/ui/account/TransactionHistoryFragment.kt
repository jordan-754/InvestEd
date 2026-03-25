// ui/account/TransactionHistoryFragment.kt
package com.example.InvestEd.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentTransactionHistoryBinding
import com.example.InvestEd.viewmodel.TransactionHistoryViewModel

class TransactionHistoryFragment : Fragment() {

    private var _binding: FragmentTransactionHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionHistoryViewModel by viewModels()
    private val adapter = TransactionAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        observeViewModel()
        viewModel.loadTransactions()
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TransactionHistoryViewModel.TransactionState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is TransactionHistoryViewModel.TransactionState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.GONE
                    adapter.submitList(state.transactions)
                }
                is TransactionHistoryViewModel.TransactionState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                }
                is TransactionHistoryViewModel.TransactionState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}