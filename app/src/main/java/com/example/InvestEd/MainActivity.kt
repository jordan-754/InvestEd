package com.example.InvestEd

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.InvestEd.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        // ✅ Destinations where bottom nav should be HIDDEN
        val noBottomNav = setOf(
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.createPasswordFragment,
            R.id.otpSendFragment,
            R.id.otpVerifyFragment,
            R.id.successFragment,
            R.id.aiChatFragment,
            R.id.settingsFragment,
            R.id.changePasswordFragment,
            R.id.personalDetailsFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.visibility =
                if (destination.id in noBottomNav) View.GONE else View.VISIBLE
        }

        // ✅ Handle Firebase email verification deep link
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val data = intent.data
        if (data != null) {
            val link = data.toString()
            // ✅ Firebase email verification link
            if (FirebaseAuth.getInstance().isSignInWithEmailLink(link)) {
                // Just reload the current user — Firebase handles verification internally
                FirebaseAuth.getInstance().currentUser?.reload()
                // Don't open any screen — the user will log in normally
            }
        }
    }
}