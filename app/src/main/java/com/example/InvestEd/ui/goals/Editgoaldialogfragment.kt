package com.example.InvestEd.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.InvestEd.R
import com.example.InvestEd.model.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Editgoaldialogfragment : DialogFragment() {

    private var goalId: String = ""
    private var goalTitle: String = ""
    private var goalTarget: Double = 0.0
    private var goalDeadline: String = ""

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val ARG_GOAL_ID       = "goal_id"
        private const val ARG_GOAL_TITLE    = "goal_title"
        private const val ARG_GOAL_TARGET   = "goal_target"
        private const val ARG_GOAL_DEADLINE = "goal_deadline"

        fun newInstance(goal: Goal): Editgoaldialogfragment {
            val fragment = Editgoaldialogfragment()
            val args = Bundle()
            args.putString(ARG_GOAL_ID,       goal.id)
            args.putString(ARG_GOAL_TITLE,    goal.title)
            args.putDouble(ARG_GOAL_TARGET,   goal.targetAmount)
            args.putString(ARG_GOAL_DEADLINE, goal.deadline)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            goalId       = args.getString(ARG_GOAL_ID, "")
            goalTitle    = args.getString(ARG_GOAL_TITLE, "")
            goalTarget   = args.getDouble(ARG_GOAL_TARGET, 0.0)
            goalDeadline = args.getString(ARG_GOAL_DEADLINE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_goal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle     = view.findViewById<EditText>(R.id.etEditTitle)
        val etTarget    = view.findViewById<EditText>(R.id.etEditTarget)
        val tvDeadline  = view.findViewById<TextView>(R.id.tvEditDeadline)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)
        val btnSave     = view.findViewById<Button>(R.id.btnSaveEdit)
        val btnCancel   = view.findViewById<Button>(R.id.btnCancelEdit)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressEditSave)

        etTitle.setText(goalTitle)
        etTarget.setText(goalTarget.toString())
        tvDeadline.text = goalDeadline

        var selectedDeadline = goalDeadline

        // ── Date picker: TODAY and FUTURE only ───────────────────────────
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val picker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDeadline = String.format("%04d-%02d-%02d", year, month + 1, day)
                    tvDeadline.text  = selectedDeadline
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.minDate = System.currentTimeMillis()
            picker.show()
        }

        // ── Save → Firestore ─────────────────────────────────────────────
        btnSave.setOnClickListener {
            val newTitle  = etTitle.text.toString().trim()
            val newTarget = etTarget.text.toString().toDoubleOrNull()

            if (newTitle.isEmpty()) {
                etTitle.error = "Title cannot be empty"
                return@setOnClickListener
            }
            if (newTarget == null || newTarget <= 0) {
                etTarget.error = "Enter a valid amount"
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Not logged in.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled      = false

            val updates = hashMapOf<String, Any>(
                "title"        to newTitle,
                "targetAmount" to newTarget,
                "deadline"     to selectedDeadline
            )

            // ── Your rules allow BOTH paths below. We try the nested path
            //    first (users/{uid}/goals/{goalId}). If your goals are
            //    stored in the flat /goals collection instead, swap to
            //    Option B below. ─────────────────────────────────────────

            // Option A — nested: users/{uid}/goals/{goalId}
            db.collection("users")
                .document(currentUser.uid)
                .collection("goals")
                .document(goalId)
                .update(updates)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Goal updated!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    // ── If nested path fails, fall back to flat /goals/{goalId}
                    //    (your rules allow update if resource.data.userId == auth.uid)
                    db.collection("goals")
                        .document(goalId)
                        .update(updates)
                        .addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Goal updated!", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                        .addOnFailureListener { e2 ->
                            progressBar.visibility = View.GONE
                            btnSave.isEnabled      = true
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e2.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
        }

        btnCancel.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        if (window != null) {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}