package com.example.InvestEd.ui.settings

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        binding.tvEmail.text = email

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnSendResetEmail.setOnClickListener {
            if (email.isBlank()) {
                Toast.makeText(requireContext(), "No email found for this account", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSendResetEmail.isEnabled = false

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendResetEmail.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "✅ Reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendResetEmail.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}