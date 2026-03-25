package com.example.InvestEd.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Navigate to Change Password screen
        binding.rowChangePassword.setOnClickListener {
            findNavController().navigate(R.id.changePasswordFragment)
        }

        // Delete account with confirmation
        binding.rowDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        // Step 1 — First confirmation
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Continue") { _, _ ->
                showFinalDeleteConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalDeleteConfirmation() {
        // Step 2 — Final confirmation
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Final Warning")
            .setMessage("All your data including investments, goals, and progress will be permanently deleted. This CANNOT be undone.\n\nDo you still want to delete your account?")
            .setPositiveButton("Yes, Delete My Account") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("No, Keep My Account", null)
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid  = user.uid

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Delete Firestore user document
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .delete().await()

                // 2. Delete Firebase Auth account
                user.delete().await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Account deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate back to login and clear entire back stack
                    findNavController().navigate(
                        R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Firebase requires recent login for deletion
                    if (e.message?.contains("requires recent authentication") == true) {
                        Toast.makeText(
                            requireContext(),
                            "Please log out and log back in before deleting your account",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete account: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}