package com.example.InvestEd.ui.rewards

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.InvestEd.databinding.FragmentRewardsBinding
import com.example.InvestEd.ui.account.BadgeAdapter
import com.example.InvestEd.viewmodel.BadgeViewModel
import com.example.InvestEd.viewmodel.RewardsViewModel

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RewardsViewModel by viewModels()
    private val badgeViewModel: BadgeViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var rewardAdapter: RewardAdapter
    private lateinit var redemptionAdapter: RedemptionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRewardsRecyclerView()
        setupRedemptionsRecyclerView()
        setupBadgeRecyclerView()

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }

        viewModel.loadRewards()
        viewModel.loadRewardsFromFirestore()
        viewModel.loadMyRedemptions()
        badgeViewModel.loadBadges()

        observeViewModel()
    }

    private fun setupRewardsRecyclerView() {
        rewardAdapter = RewardAdapter { reward ->
            showConfirmDialog(reward.cost, reward.pesosEquivalent)
        }
        binding.rvRewards.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRewards.adapter = rewardAdapter
    }

    private fun setupRedemptionsRecyclerView() {
        redemptionAdapter = RedemptionAdapter()
        binding.rvRedemptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRedemptions.adapter = redemptionAdapter
    }

    private fun setupBadgeRecyclerView() {
        badgeAdapter = BadgeAdapter()
        binding.rvBadges.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvBadges.adapter = badgeAdapter
    }

    private fun showConfirmDialog(pointsCost: Long, pesosReward: Double) {
        val currentPoints = (viewModel.state.value as? RewardsViewModel.RewardsState.Success)?.points?.toLong() ?: 0L

        if (currentPoints < pointsCost) {
            Toast.makeText(requireContext(), "Not enough points!", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Exchange Points")
            .setMessage(
                "Exchange ${String.format("%,d", pointsCost)} pts for ₱${String.format("%.2f", pesosReward)}?\n\n" +
                        "₱${String.format("%.2f", pesosReward)} will be added directly to your balance."
            )
            .setPositiveButton("Exchange") { _, _ ->
                viewModel.exchangePoints(pointsCost, pesosReward)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {

        // Points state
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RewardsViewModel.RewardsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RewardsViewModel.RewardsState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val pts = state.points
                    binding.tvTotalPoints.text = "Total Points: ${String.format("%,d", pts)} pts"
                    binding.tvBadgeHint.text =
                        if (pts == 0L) "0 pts earned yet"
                        else "${String.format("%,d", pts)} pts earned — keep going!"

                    val minCost = viewModel.rewards.value?.minOfOrNull { it.cost } ?: 1000L
                    binding.tvPointsNeeded.text = when {
                        pts >= minCost -> "You have enough points to exchange!"
                        else -> "You need ${String.format("%,d", minCost - pts)} more pts to exchange"
                    }
                    rewardAdapter.setUserPoints(pts.toInt())
                }
                is RewardsViewModel.RewardsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        // Exchange state
        viewModel.exchangeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RewardsViewModel.ExchangeState.Idle -> Unit
                is RewardsViewModel.ExchangeState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RewardsViewModel.ExchangeState.Pending -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Exchange request submitted!", Toast.LENGTH_SHORT).show()
                    viewModel.resetExchangeState()
                }
                is RewardsViewModel.ExchangeState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "🎉 ₱${String.format("%.2f", state.pesosAdded)} added to your balance!",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetExchangeState()
                    viewModel.loadRewards()
                    viewModel.loadMyRedemptions()
                }
                is RewardsViewModel.ExchangeState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetExchangeState()
                }
            }
        }

        // Rewards catalog
        viewModel.rewards.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.tvNoRewards.text       = "No rewards available yet."
                binding.tvNoRewards.visibility = View.VISIBLE
                binding.rvRewards.visibility   = View.GONE
                binding.tvRewardCount.text     = "0 rewards"
            } else {
                binding.tvNoRewards.visibility = View.GONE
                binding.rvRewards.visibility   = View.VISIBLE
                binding.tvRewardCount.text     = "${list.size} reward${if (list.size != 1) "s" else ""}"
                rewardAdapter.submitList(list)
            }
        }

        // My redemptions
        viewModel.redemptions.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                binding.tvNoRedemptions.visibility = View.VISIBLE
                binding.rvRedemptions.visibility   = View.GONE
            } else {
                binding.tvNoRedemptions.visibility = View.GONE
                binding.rvRedemptions.visibility   = View.VISIBLE
                redemptionAdapter.submitList(list)
            }
        }

        // Badges
        badgeViewModel.badgeState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BadgeViewModel.BadgeState.Success -> {
                    badgeAdapter.submitList(state.badges)
                    binding.tvBadgesTitle.visibility = View.VISIBLE
                    binding.rvBadges.visibility      = View.VISIBLE
                }
                is BadgeViewModel.BadgeState.Error -> {
                    binding.tvBadgesTitle.visibility = View.GONE
                    binding.rvBadges.visibility      = View.GONE
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