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
import com.example.InvestEd.databinding.FragmentOtpVerifyBinding
import com.example.InvestEd.viewmodel.OtpViewModel

class OtpVerifyFragment : Fragment() {

    private var _binding: FragmentOtpVerifyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OtpViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtpVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = arguments?.getString("email") ?: ""
        binding.tvEmailHint.text = "We sent a password reset link to:\n$email\n\nClick the link in your email to reset your password."

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnVerifyOtp.setOnClickListener {
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }

        binding.tvResendCode.setOnClickListener {
            if (email.isNotBlank()) {
                viewModel.sendOtp(email)
                Toast.makeText(requireContext(), "Reset email resent!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.countdown.observe(viewLifecycleOwner) { seconds ->
            if (seconds > 0) {
                binding.tvCountdown.text      = "Resend in 1:${seconds.toString().padStart(2, '0')}"
                binding.tvResendCode.isEnabled = false
            } else {
                binding.tvCountdown.text      = ""
                binding.tvResendCode.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}