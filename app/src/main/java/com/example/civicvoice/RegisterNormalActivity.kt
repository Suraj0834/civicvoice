package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityRegisterNormalBinding
import com.example.civicvoice.network.RegisterRequest
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.UserResponse
import com.example.civicvoice.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterNormalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterNormalBinding
    private val TAG = "RegisterNormalActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterNormalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRegisterButton()
        setupInputListeners()
    }

    private fun setupRegisterButton() {
        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                registerCitizen()
            }
        }
    }

    private fun setupInputListeners() {
        binding.nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.nameInputLayout.error = null
        }
        binding.usernameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.usernameInputLayout.error = null
        }
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.emailInputLayout.error = null
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordInputLayout.error = null
        }
        binding.phoneEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.phoneInputLayout.error = null
        }
    }

    private fun validateInputs(): Boolean {
        binding.errorTextView.visibility = View.GONE

        val name = binding.nameEditText.text.toString().trim()
        val username = binding.usernameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameInputLayout.error = getString(R.string.error_name_required)
            return false
        }

        if (username.isEmpty()) {
            binding.usernameInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (username.length < 4) {
            binding.usernameInputLayout.error = getString(R.string.error_username_short)
            return false
        }

        if (email.isEmpty()) {
            binding.emailInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = getString(R.string.error_invalid_email)
            return false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_password_short)
            return false
        }

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.phoneInputLayout.error = getString(R.string.error_invalid_phone)
            return false
        }

        return true
    }

    private fun registerCitizen() {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = getString(R.string.registering)

        val name = binding.nameEditText.text.toString().trim()
        val username = binding.usernameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()

        val registerRequest = RegisterRequest(
            username = username,
            email = email,
            password = password,
            phone = phone,
            name = name,
            userType = "normal",
            department = null // Explicitly pass null for normal users
        )
        RetrofitClient.instance.register(registerRequest).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                binding.registerButton.isEnabled = true
                binding.registerButton.text = getString(R.string.register)
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    if (userResponse != null) {
                        UserSession.setUser(userResponse.user, userResponse.token)
                        showRegistrationSuccess()
                        navigateToMainActivity()
                    } else {
                        showError("Empty response from server")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when {
                        response.code() == 400 && errorBody?.contains("already exists") == true -> "Username or email already taken"
                        response.code() == 400 -> "Invalid input data: $errorBody"
                        response.code() == 500 -> "Server error, please try again later"
                        else -> "Registration failed: ${response.code()}"
                    }
                    showError(errorMessage)
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                binding.registerButton.isEnabled = true
                binding.registerButton.text = getString(R.string.register)
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun showRegistrationSuccess() {
        val snackbar = Snackbar.make(binding.root, "Registration successful!", Snackbar.LENGTH_LONG)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}