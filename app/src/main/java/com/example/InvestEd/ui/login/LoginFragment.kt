package com.example.InvestEd.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentLoginBinding
import com.example.InvestEd.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isBlank()) {
                Toast.makeText(requireContext(), "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bundle = android.os.Bundle().apply { putString("email", email) }
            findNavController().navigate(R.id.otpSendFragment, bundle)
        }

        binding.tvSignUp.setOnClickListener {
            // ✅ Sign out any existing Firebase session before normal registration
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoginViewModel.LoginState.Idle -> {
                    binding.btnLogin.isEnabled     = true
                    binding.progressBar.visibility = View.GONE
                }
                is LoginViewModel.LoginState.Loading -> {
                    binding.btnLogin.isEnabled     = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is LoginViewModel.LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    findNavController().navigate(
                        R.id.homeFragment, null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true).build()
                    )
                    viewModel.resetState()
                }
                is LoginViewModel.LoginState.GoogleSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.isProfileComplete) {
                        // ✅ Returning Google user — go straight to home
                        findNavController().navigate(
                            R.id.homeFragment, null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true).build()
                        )
                    } else {
                        // ✅ New Google user — pass bundle so RegisterFragment
                        //    calls completeGoogleProfile() instead of register()
                        val bundle = android.os.Bundle().apply {
                            putBoolean("isGoogleSignUp", true)
                            putString("firstName", state.user.firstName)
                            putString("lastName",  state.user.lastName)
                            putString("email",     state.user.email)
                        }
                        findNavController().navigate(
                            R.id.registerFragment,
                            bundle,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true).build()
                        )
                    }
                    viewModel.resetState()
                }
                is LoginViewModel.LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled     = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}