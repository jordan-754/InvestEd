// ui/microinvestment/MicroInvestmentFragment.kt
package com.example.InvestEd.ui.microinvestment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentMicroInvestmentBinding
import com.example.InvestEd.viewmodel.MicroInvestmentViewModel

class MicroInvestmentFragment : Fragment() {

    private var _binding: FragmentMicroInvestmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MicroInvestmentViewModel by viewModels()

    private val GOOGLE_FORM_URL = "https://docs.google.com/forms/d/e/1FAIpQLSdBLMrfsTXQwJNHWfYtMCeXHUPgPyb7jSHtZ28OGk78rGBw5Q/viewform?usp=sharing&ouid=115371268254323784420"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMicroInvestmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun getAmount(): String {
        return binding.tilCustomAmount.editText?.text?.toString()?.trim() ?: ""
    }

    private fun getWithdrawAmount(): String {
        return binding.tilWithdrawAmount.editText?.text?.toString()?.trim() ?: ""
    }

    private fun setupClickListeners() {
        // ✅ Disable submit proof button by default
        binding.btnSubmitInvestment.isEnabled = false
        binding.btnSubmitInvestment.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#9E9E9E")
            )

        binding.btnInvestNow.setOnClickListener {
            val amountStr = getAmount()
            val amount = amountStr.toDoubleOrNull()
            when {
                amountStr.isBlank() || amount == null ->
                    Toast.makeText(requireContext(), "Please enter an amount first", Toast.LENGTH_SHORT).show()
                amount < 20 ->
                    Toast.makeText(requireContext(), "Minimum investment is ₱20", Toast.LENGTH_SHORT).show()
                amount > 10000 ->
                    Toast.makeText(requireContext(), "Maximum investment is ₱10,000", Toast.LENGTH_SHORT).show()
                else -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_FORM_URL))
                    startActivity(intent)

                    // ✅ Enable submit proof button after opening the form
                    binding.btnSubmitInvestment.isEnabled = true
                    binding.btnSubmitInvestment.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#4CAF50")
                        )
                }
            }
        }

        binding.btnSubmitInvestment.setOnClickListener {
            val amountStr = getAmount()
            val amount = amountStr.toDoubleOrNull()
            when {
                amountStr.isBlank() || amount == null ->
                    Toast.makeText(requireContext(), "Please enter the amount you invested", Toast.LENGTH_SHORT).show()
                amount < 20 ->
                    Toast.makeText(requireContext(), "Minimum investment is ₱20", Toast.LENGTH_SHORT).show()
                amount > 10000 ->
                    Toast.makeText(requireContext(), "Maximum investment is ₱10,000", Toast.LENGTH_SHORT).show()
                else -> viewModel.submitPendingInvestment(amount)
            }
        }

        binding.btnWithdraw.setOnClickListener {
            val amountStr = getWithdrawAmount()
            val amount = amountStr.toDoubleOrNull()
            when {
                amountStr.isBlank() || amount == null ->
                    Toast.makeText(requireContext(), "Please enter a withdrawal amount", Toast.LENGTH_SHORT).show()
                amount <= 0 ->
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                else -> {
                    val source = if (binding.rgWithdrawSource.checkedRadioButtonId == R.id.rbInvestment)
                        "investment" else "savings"
                    showWithdrawConfirmDialog(amount, source)
                }
            }
        }
    }

    private fun showWithdrawConfirmDialog(amount: Double, source: String) {
        val dialog        = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogBinding = com.example.InvestEd.databinding.DialogGoalWithdrawBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val sourceLabel = if (source == "investment") "investment balance" else "savings"
        dialogBinding.tvGoalName.text   = "💸 Withdraw from $sourceLabel"
        dialogBinding.tvGoalAmount.text = "Amount: ₱${String.format("%,.2f", amount)}"

        // ✅ E-wallet type dropdown
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

            // ✅ Clear errors
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

            // ✅ Submit withdrawal with e-wallet details
            viewModel.submitWithdrawal(
                amount       = amount,
                source       = source,
                walletType   = walletType,
                walletNumber = walletNumber,
                fullName     = fullName
            )
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.currentValue.observe(viewLifecycleOwner) { value ->
            binding.tvTotalInvested.text = "₱${String.format("%.2f", value)}"
        }

        viewModel.latestCashIn.observe(viewLifecycleOwner) { latest ->
            if (latest == null) {
                binding.cardCashInStatus.visibility = View.GONE
            } else {
                binding.cardCashInStatus.visibility = View.VISIBLE
                binding.tvCashInStatus.text = "Status: ${latest.status}"
                binding.tvCashInAmount.text = "Amount: ₱${String.format("%.2f", latest.amount)}"
            }
        }

        viewModel.currentValue.observe(viewLifecycleOwner) { value ->
            binding.tvCurrentValue.text = "₱${String.format("%.2f", value)}"
        }

        viewModel.totalInterest.observe(viewLifecycleOwner) { interest ->
            binding.tvTotalInterest.text = "₱${String.format("%.2f", interest)}"
        }

        viewModel.investState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MicroInvestmentViewModel.InvestState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitInvestment.isEnabled = true
                }
                is MicroInvestmentViewModel.InvestState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSubmitInvestment.isEnabled = false
                }
                is MicroInvestmentViewModel.InvestState.PendingSubmitted -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tilCustomAmount.editText?.text?.clear()
                    binding.cardPending.visibility = View.VISIBLE
                    binding.btnSubmitInvestment.isEnabled = false
                    binding.btnSubmitInvestment.text = "✅ Submitted"
                    binding.btnSubmitInvestment.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9E9E9E")
                        )
                    viewModel.resetState()
                }
                is MicroInvestmentViewModel.InvestState.WithdrawalSubmitted -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tilWithdrawAmount.editText?.text?.clear()
                    binding.cardWithdrawalPending.visibility = View.VISIBLE
                    binding.btnWithdraw.isEnabled = false
                    binding.btnWithdraw.text = "⏳ Withdrawal Pending"
                    binding.btnWithdraw.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#9E9E9E")
                        )
                    Toast.makeText(requireContext(), "Withdrawal request submitted!", Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                is MicroInvestmentViewModel.InvestState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitInvestment.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}