package com.example.InvestEd.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentGoalsBinding
import com.example.InvestEd.databinding.DialogAddGoalBinding
import com.example.InvestEd.databinding.DialogAddAmountBinding
import com.example.InvestEd.databinding.DialogGoalWithdrawBinding
import com.example.InvestEd.ui.goals.Editgoaldialogfragment
import com.example.InvestEd.model.Goal
import com.example.InvestEd.viewmodel.GoalsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoalsViewModel by viewModels()
    private lateinit var adapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        binding.fabAddGoal.setOnClickListener { showAddGoalDialog() }
    }

    private fun setupRecyclerView() {
        adapter = GoalAdapter(
            onDelete    = { goal -> viewModel.deleteGoal(goalId = goal.id, goalTitle = goal.title) },
            onAddAmount = { goal ->
                if (goal.progressPercent >= 100) {
                    showWithdrawDialog(goal)
                } else {
                    showAddAmountDialog(goal)
                }
            },
            onEdit      = { goal ->
                Editgoaldialogfragment
                    .newInstance(goal)
                    .show(childFragmentManager, "EditGoalDialog")
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = adapter
    }

    private fun showWithdrawDialog(goal: Goal) {
        val dialog        = BottomSheetDialog(requireContext())
        val dialogBinding = DialogGoalWithdrawBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvGoalName.text   = "${goal.title} — Completed!"
        dialogBinding.tvGoalAmount.text = "Amount to withdraw: ₱${String.format("%,.2f", goal.currentAmount)}"

        val wallets = listOf("GCash", "Maya", "Maribank")
        val walletAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            wallets
        )
        dialogBinding.spinnerWallet.setAdapter(walletAdapter)
        dialogBinding.spinnerWallet.setText(wallets[0], false)

        dialogBinding.btnSubmitWithdraw.setOnClickListener {
            val walletType   = dialogBinding.spinnerWallet.text.toString().trim()
            val walletNumber = dialogBinding.etWalletNumber.text.toString().trim()
            val fullName     = dialogBinding.etFullName.text.toString().trim()

            dialogBinding.layoutWallet.error       = null
            dialogBinding.layoutWalletNumber.error = null
            dialogBinding.layoutFullName.error     = null

            var hasError = false

            if (walletType.isBlank() || !listOf("GCash", "Maya", "Maribank").contains(walletType)) {
                dialogBinding.layoutWallet.error = "Please select an e-wallet"
                hasError = true
            }

            when {
                walletNumber.isBlank() -> {
                    dialogBinding.layoutWalletNumber.error = "Please enter your e-wallet number"
                    hasError = true
                }
                !walletNumber.matches(Regex("^[0-9]+$")) -> {
                    dialogBinding.layoutWalletNumber.error = "Number must contain digits only"
                    hasError = true
                }
                !walletNumber.startsWith("09") -> {
                    dialogBinding.layoutWalletNumber.error = "Number must start with 09"
                    hasError = true
                }
                walletNumber.length != 11 -> {
                    dialogBinding.layoutWalletNumber.error = "Number must be exactly 11 digits"
                    hasError = true
                }
            }

            when {
                fullName.isBlank() -> {
                    dialogBinding.layoutFullName.error = "Please enter your full name"
                    hasError = true
                }
                fullName.length < 5 -> {
                    dialogBinding.layoutFullName.error = "Full name must be at least 5 characters"
                    hasError = true
                }
                !fullName.matches(Regex("^[a-zA-Z ,.'-]+$")) -> {
                    dialogBinding.layoutFullName.error = "Full name must contain letters only"
                    hasError = true
                }
                !fullName.contains(" ") -> {
                    dialogBinding.layoutFullName.error = "Please enter both first and last name"
                    hasError = true
                }
            }

            if (hasError) return@setOnClickListener

            viewModel.submitGoalWithdrawal(
                goalId       = goal.id,
                goalTitle    = goal.title,
                amount       = goal.currentAmount,
                walletType   = walletType,
                walletNumber = walletNumber,
                fullName     = fullName
            )
            dialog.dismiss()
            Toast.makeText(
                requireContext(),
                "Withdrawal request submitted! Admin will process it soon.",
                Toast.LENGTH_LONG
            ).show()
        }
        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.goalsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GoalsViewModel.GoalsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility     = View.GONE
                }
                is GoalsViewModel.GoalsState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.GONE
                    binding.rvGoals.visibility     = View.VISIBLE
                    adapter.submitList(state.goals)
                }
                is GoalsViewModel.GoalsState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.visibility     = View.VISIBLE
                    binding.rvGoals.visibility     = View.GONE
                }
                is GoalsViewModel.GoalsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.actionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GoalsViewModel.ActionState.Success -> {
                    Toast.makeText(requireContext(), "Goal Added!", Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is GoalsViewModel.ActionState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                else -> Unit
            }
        }
    }

    private fun showAddGoalDialog() {
        val dialog        = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddGoalBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val today = Calendar.getInstance()
        dialogBinding.etDeadline.setOnClickListener {
            val cal    = Calendar.getInstance()
            val picker = DatePickerDialog(
                requireContext(),
                { _, y, m, d -> dialogBinding.etDeadline.setText("${m + 1}/$d/$y") },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.minDate = today.timeInMillis
            picker.show()
        }

        dialogBinding.btnSaveGoal.setOnClickListener {
            viewModel.addGoal(
                dialogBinding.etGoalTitle.text.toString(),
                dialogBinding.etTargetAmount.text.toString(),
                dialogBinding.etDeadline.text.toString()
            )
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAddAmountDialog(goal: Goal) {
        val dialog        = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddAmountBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvGoalName.text     = "💰 ${goal.title}"
        dialogBinding.tvCurrentSaved.text =
            "Saved: ₱${String.format("%.2f", goal.currentAmount)} / ₱${String.format("%.2f", goal.targetAmount)}"

        viewModel.currentBalance.observe(viewLifecycleOwner) { currentBalance ->
            dialogBinding.tvAvailableBalance.text =
                "Available balance: ₱${String.format("%.2f", currentBalance)}"
        }

        dialogBinding.btnSaveAmount.setOnClickListener {
            val currentBalance = viewModel.currentBalance.value ?: 0.0
            val input          = dialogBinding.etAddAmount.text.toString().toDoubleOrNull()
            when {
                input == null || input <= 0 ->
                    Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                input > currentBalance ->
                    Toast.makeText(requireContext(),
                        "Insufficient balance. You only have ₱${String.format("%.2f", currentBalance)}",
                        Toast.LENGTH_LONG).show()
                goal.currentAmount + input > goal.targetAmount ->
                    Toast.makeText(requireContext(),
                        "Amount exceeds target. Max you can add: ₱${String.format("%.2f", goal.targetAmount - goal.currentAmount)}",
                        Toast.LENGTH_LONG).show()
                else -> {
                    val newTotal = goal.currentAmount + input
                    viewModel.updateCurrentAmount(goal.id, newTotal, goal.title, input)
                    Toast.makeText(requireContext(), "₱${String.format("%.2f", input)} added!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                    if (newTotal >= goal.targetAmount) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("🎉 Goal Completed!")
                            .setMessage(
                                "\"${goal.title}\" is complete!\n\n" +
                                        "Tap the Withdraw button on your goal to request your money via e-wallet."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}