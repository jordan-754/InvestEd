package com.example.InvestEd.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.R
import com.example.InvestEd.databinding.FragmentHomeBinding
import com.example.InvestEd.viewmodel.HomeViewModel
import com.example.InvestEd.viewmodel.LeaderboardViewModel
import com.example.InvestEd.ui.home.LeaderboardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val leaderboardViewModel: LeaderboardViewModel by viewModels()
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
        setupLeaderboard()
        observeLeaderboard()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHomeData()
        checkDailyLoginReward()
        loadEstimatedInterest()
    }

    private val MONTHLY_RATE = 0.03

    private fun loadEstimatedInterest() {
        val userId = auth.currentUser?.uid ?: return

        // ── Read all goals for this user, sum their currentAmount ─────────
        // Tries nested path first: users/{uid}/goals
        // If your goals are in the flat /goals collection with a userId field,
        // swap to the commented-out block below.
        db.collection("users")
            .document(userId)
            .collection("goals")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    // Fallback: try flat /goals collection with userId field
                    loadEstimatedInterestFromFlatCollection(userId)
                    return@addOnSuccessListener
                }

                val totalSaved       = docs.sumOf { it.getDouble("currentAmount") ?: 0.0 }
                val estimatedMonthly = totalSaved * MONTHLY_RATE

                binding.tvEstimatedInterest.text =
                    "₱" + String.format("%,.2f", estimatedMonthly)
                binding.tvInterestSubtitle.text =
                    "Based on ₱" + String.format("%,.2f", totalSaved) +
                            " saved in goals · 3% per month"
            }
            .addOnFailureListener {
                // Fallback to flat collection if nested path fails
                loadEstimatedInterestFromFlatCollection(userId)
            }
    }

    // ── Fallback: flat /goals collection with userId field ────────────────
    private fun loadEstimatedInterestFromFlatCollection(userId: String) {
        db.collection("goals")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                val totalSaved       = docs.sumOf { it.getDouble("currentAmount") ?: 0.0 }
                val estimatedMonthly = totalSaved * MONTHLY_RATE

                binding.tvEstimatedInterest.text =
                    "₱" + String.format("%,.2f", estimatedMonthly)
                binding.tvInterestSubtitle.text =
                    "Based on ₱" + String.format("%,.2f", totalSaved) +
                            " saved in goals · 3% per month"
            }
            .addOnFailureListener {
                binding.tvEstimatedInterest.text = "₱0.00"
                binding.tvInterestSubtitle.text  = "Could not load goal data"
            }
    }

    private fun setupLeaderboard() {
        leaderboardAdapter = LeaderboardAdapter("")
        binding.rvLeaderboard.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = leaderboardAdapter
        binding.tvRefreshLeaderboard.setOnClickListener {
            leaderboardViewModel.load()
        }
    }

    private fun observeLeaderboard() {
        leaderboardViewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LeaderboardViewModel.LeaderboardState.Loading -> {
                    binding.progressLeaderboard.visibility = View.VISIBLE
                    binding.rvLeaderboard.visibility       = View.GONE
                    binding.tvLeaderboardEmpty.visibility  = View.GONE
                }
                is LeaderboardViewModel.LeaderboardState.Success -> {
                    binding.progressLeaderboard.visibility = View.GONE
                    binding.rvLeaderboard.visibility       = View.VISIBLE
                    binding.tvLeaderboardEmpty.visibility  = View.GONE
                    leaderboardAdapter = LeaderboardAdapter(state.currentUserId)
                    binding.rvLeaderboard.adapter = leaderboardAdapter
                    leaderboardAdapter.submitList(state.entries)
                }
                is LeaderboardViewModel.LeaderboardState.Empty -> {
                    binding.progressLeaderboard.visibility = View.GONE
                    binding.rvLeaderboard.visibility       = View.GONE
                    binding.tvLeaderboardEmpty.visibility  = View.VISIBLE
                }
                is LeaderboardViewModel.LeaderboardState.Error -> {
                    binding.progressLeaderboard.visibility = View.GONE
                    binding.tvLeaderboardEmpty.visibility  = View.VISIBLE
                    binding.tvLeaderboardEmpty.text        = "Failed to load leaderboard"
                }
            }
        }
    }

    private fun checkDailyLoginReward() {
        val userId  = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val today   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        userRef.get().addOnSuccessListener { snap ->
            val lastLogin = snap.getString("lastLoginDate") ?: ""
            if (lastLogin != today) {
                userRef.set(
                    mapOf(
                        "lastLoginDate" to today,
                        "rewardPoints"  to FieldValue.increment(5)
                    ),
                    SetOptions.merge()
                ).addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Daily login bonus: +5 pts!",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.loadHomeData()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeViewModel.HomeState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is HomeViewModel.HomeState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val d = state.data
                    binding.tvWelcome.text        = "Hello, ${d.userName}"
                    binding.tvPoints.text         = "${d.rewardPoints} pts"
                    binding.tvTotalSavings.text   = "₱" + String.format("%,.2f", d.totalSavings)
                    binding.tvTotalInvested.text  = "₱" + String.format("%,.2f", d.totalInvested)
                    binding.tvCurrentBalance.text = "₱" + String.format("%,.2f", d.totalSavings)
                }
                is HomeViewModel.HomeState.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnInvest.setOnClickListener   { findNavController().navigate(R.id.microInvestmentFragment) }
        binding.btnGoal.setOnClickListener     { findNavController().navigate(R.id.goalsFragment) }
        binding.btnTracker.setOnClickListener  { findNavController().navigate(R.id.budgetFragment) }
        binding.btnLearn.setOnClickListener    { findNavController().navigate(R.id.learningFragment) }
        binding.btnAiChat.setOnClickListener   { findNavController().navigate(R.id.aiChatFragment) }
        binding.cardAiCoach.setOnClickListener { findNavController().navigate(R.id.aiChatFragment) }
        binding.tvAskAi.setOnClickListener     { findNavController().navigate(R.id.aiChatFragment) }
        binding.tvPoints.setOnClickListener    { findNavController().navigate(R.id.rewardsFragment) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}