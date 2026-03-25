package com.example.InvestEd.ui.otp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentOtpSendBinding
import com.example.InvestEd.viewmodel.OtpViewModel

class OtpSendFragment : Fragment() {

    private var _binding: FragmentOtpSendBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtpViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtpSendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = arguments?.getString("email") ?: ""
        binding.etEmail.setText(email)

        // Debug — show what email was received
        Toast.makeText(requireContext(), "Sending to: $email", Toast.LENGTH_LONG).show()

        // Auto-send if email was passed from login screen
        if (email.isNotBlank()) {
            viewModel.sendOtp(email)
        } else {
            Toast.makeText(requireContext(), "No email received!", Toast.LENGTH_LONG).show()
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendOtp(email)
        }
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.otpState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OtpViewModel.OtpState.Idle -> {
                    binding.btnSendOtp.isEnabled   = true
                    binding.progressBar.visibility = View.GONE
                }
                is OtpViewModel.OtpState.Loading -> {
                    binding.btnSendOtp.isEnabled   = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is OtpViewModel.OtpState.OtpSent -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "OTP Sent successfully!", Toast.LENGTH_SHORT).show()
                    val bundle = android.os.Bundle().apply {
                        putString("email", binding.etEmail.text.toString().trim())
                    }
                    findNavController().navigate(R.id.otpVerifyFragment, bundle)
                    viewModel.resetState()
                }
                is OtpViewModel.OtpState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled   = true
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
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