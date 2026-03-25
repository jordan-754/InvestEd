package com.example.InvestEd.ui.personaldetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.InvestEd.databinding.FragmentPersonalDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class PersonalDetailsFragment : Fragment() {

    private var _binding: FragmentPersonalDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserDetails()
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
    }

    private fun loadUserDetails() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(uid).get().await()
            withContext(Dispatchers.Main) {
                val fullName  = doc.getString("fullName")   ?: "—"
                val email     = doc.getString("email")      ?: "—"
                val birthdate = doc.getString("birthdate")?.takeIf { it.isNotBlank() } ?: "Not set"
                val school    = doc.getString("school")?.takeIf { it.isNotBlank() }    ?: "Not set"
                val place     = doc.getString("place")?.takeIf { it.isNotBlank() }     ?: "Not set"

                binding.tvFullName.text  = fullName
                binding.tvEmail.text     = email
                binding.tvBirthdate.text = birthdate
                binding.tvSchool.text    = school
                binding.tvPlace.text     = place

                // Set avatar initial from first letter of full name
                binding.tvAvatarInitial.text = fullName.firstOrNull()?.uppercase() ?: "?"

                // Calculate age from birthdate (format MM/DD/YYYY)
                binding.tvAge.text = calculateAge(birthdate)
            }
        }
    }

    private fun calculateAge(birthdate: String): String {
        return try {
            val parts = birthdate.split("/")
            if (parts.size != 3) return "Not set"
            val month = parts[0].toInt()
            val day   = parts[1].toInt()
            val year  = parts[2].toInt()

            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - year
            if (today.get(Calendar.MONTH) + 1 < month ||
                (today.get(Calendar.MONTH) + 1 == month && today.get(Calendar.DAY_OF_MONTH) < day)
            ) age--

            "$age years old"
        } catch (e: Exception) {
            "Not set"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}