package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.network.Feedback
import com.example.civicvoice.network.FeedbackRequest
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedbackActivity : AppCompatActivity() {
    private val TAG = "FeedbackActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val complaintId = intent.getStringExtra("complaint_id") ?: run {
            showError("Invalid complaint ID")
            finish()
            return
        }

        val feedbackInput = findViewById<EditText>(R.id.feedbackInput)
        val submitButton = findViewById<Button>(R.id.submitFeedbackButton)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.submit_feedback)
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        submitButton.setOnClickListener {
            val feedback = feedbackInput.text.toString().trim()
            if (feedback.isEmpty()) {
                findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_feedback).error = getString(R.string.error_field_required)
            } else if (feedback.length < 10) {
                findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_feedback).error = getString(R.string.error_feedback_too_short)
            } else {
                findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_feedback).error = null
                submitFeedback(complaintId, feedback)
            }
        }
    }

    private fun submitFeedback(complaintId: String, feedback: String) {
        val user = UserSession.getCurrentUser() ?: run {
            showError("Please log in to submit feedback")
            finish()
            return
        }

        val feedbackRequest = FeedbackRequest(
            complaintId = complaintId,
            content = feedback,
            userId = user.userId
        )
        Log.d(TAG, "Submitting feedback: $feedbackRequest")

        RetrofitClient.instance.submitFeedback(feedbackRequest).enqueue(object : Callback<Feedback> {
            override fun onResponse(call: Call<Feedback>, response: Response<Feedback>) {
                Log.d(TAG, "Response code: ${response.code()}, body: ${response.body()}, error: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    Toast.makeText(this@FeedbackActivity, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    when (response.code()) {
                        400 -> showError("Invalid feedback or complaint ID")
                        401 -> {
                            showError("Session expired. Please log in again.")
                            UserSession.logout()
                            startActivity(Intent(this@FeedbackActivity, LoginActivity::class.java))
                            finish()
                        }
                        403 -> showError("Unauthorized to submit feedback for this complaint")
                        else -> showError("Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<Feedback>, t: Throwable) {
                Log.e(TAG, "Network error", t)
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAction("OK") {}
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light, theme))
            .setTextColor(resources.getColor(android.R.color.white, theme))
            .show()
    }
}