package com.example.InvestEd.ui.account

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentAccountBinding
import com.example.InvestEd.viewmodel.BadgeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val badgeViewModel: BadgeViewModel by viewModels()
    private val badgeAdapter = BadgeAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBadgeRecyclerView()
        loadUserData()
        setupClickListeners()

        badgeViewModel.badgeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BadgeViewModel.BadgeState.Loading -> {
                    android.util.Log.d("BADGES", "State: Loading")
                }
                is BadgeViewModel.BadgeState.Success -> {
                    android.util.Log.d("BADGES", "State: Success — ${state.badges.size} badges")
                    state.badges.forEach {
                        android.util.Log.d("BADGES", "  > ${it.name} | unlocked=${it.isUnlocked} | icon=${it.icon}")
                    }
                    badgeAdapter.submitList(state.badges)
                }
                is BadgeViewModel.BadgeState.Error -> {
                    android.util.Log.d("BADGES", "State: Error — ${state.message}")
                }
            }
        }
        badgeViewModel.loadBadges()
    }

    private fun setupBadgeRecyclerView() {
        binding.rvBadges.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = badgeAdapter
        }
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                withContext(Dispatchers.Main) {
                    val fullName = doc.getString("fullName") ?: "User"
                    val email    = doc.getString("email")    ?: ""
                    val points   = doc.getLong("rewardPoints")?.toInt() ?: 0

                    binding.tvFullName.text      = fullName
                    binding.tvEmail.text         = email
                    binding.tvRewardPoints.text  = "$points pts"
                    binding.tvAvatarInitial.text = fullName.firstOrNull()?.uppercase() ?: "U"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.rowPersonalDetails.setOnClickListener {
            findNavController().navigate(R.id.personalDetailsFragment)
        }
        binding.rowNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationFragment)
        }
        binding.rowTransactionHistory.setOnClickListener {
            findNavController().navigate(R.id.transactionHistoryFragment)
        }
        binding.rowSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        binding.btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    findNavController().navigate(
                        R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}