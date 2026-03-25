package com.example.InvestEd.ui.register

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentCreatePasswordBinding
import com.example.InvestEd.viewmodel.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth

class CreatePasswordFragment : Fragment() {

    private var _binding: FragmentCreatePasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<View>(R.id.bottomNavigationView)?.visibility = View.GONE

        val firstName = arguments?.getString("firstName") ?: ""
        val lastName  = arguments?.getString("lastName")  ?: ""
        val email     = arguments?.getString("email")     ?: ""
        val birthdate = arguments?.getString("birthdate") ?: ""
        val school    = arguments?.getString("school")    ?: ""
        val place     = arguments?.getString("place")     ?: ""

        binding.btnSignUp.setOnClickListener {
            val password        = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // ✅ Clear errors first
            binding.layoutPassword.error        = null
            binding.layoutConfirmPassword.error = null

            var hasError = false

            if (password.isBlank()) {
                binding.layoutPassword.error = "Password is required"
                hasError = true
            } else if (password.length < 8) {
                binding.layoutPassword.error = "Password must be at least 8 characters"
                hasError = true
            } else if (!password.any { it.isUpperCase() }) {
                binding.layoutPassword.error = "Must contain at least one uppercase letter"
                hasError = true
            } else if (!password.any { it.isLowerCase() }) {
                binding.layoutPassword.error = "Must contain at least one lowercase letter"
                hasError = true
            } else if (!password.any { it.isDigit() }) {
                binding.layoutPassword.error = "Must contain at least one number"
                hasError = true
            } else if (!password.any { !it.isLetterOrDigit() }) {
                binding.layoutPassword.error = "Must contain at least one special character (!@#\$%^&*)"
                hasError = true
            }

            if (confirmPassword.isBlank()) {
                binding.layoutConfirmPassword.error = "Please confirm your password"
                hasError = true
            } else if (password != confirmPassword) {
                binding.layoutConfirmPassword.error = "Passwords do not match"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            viewModel.register(
                firstName, lastName, email,
                birthdate, school, place,
                password, confirmPassword
            )
        }

        viewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RegisterViewModel.RegisterState.Loading -> {
                    binding.btnSignUp.isEnabled    = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RegisterViewModel.RegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    // ✅ Navigate to login first, then show toast
                    findNavController().navigate(
                        R.id.action_createPasswordFragment_to_loginFragment
                    )
                    Toast.makeText(
                        requireContext(),
                        "✅ Account created! A verification email has been sent to $email. Please verify then log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetState()
                }
                is RegisterViewModel.RegisterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignUp.isEnabled    = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                else -> {
                    binding.btnSignUp.isEnabled    = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.bottomNavigationView)?.visibility = View.VISIBLE
        _binding = null
    }
}