package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityRegisterOfficialBinding
import com.example.civicvoice.network.RegisterRequest
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.UserResponse
import com.example.civicvoice.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterOfficialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterOfficialBinding
    private val TAG = "RegisterOfficialActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterOfficialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDepartmentDropdown()
        setupRegisterButton()
        setupInputListeners()
    }

    private fun setupDepartmentDropdown() {
        val departments = listOf("Sanitation", "Infrastructure", "Health", "Environment" ,"Roads")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
        binding.departmentSpinner.setAdapter(adapter)
        binding.departmentSpinner.setOnClickListener { binding.departmentSpinner.showDropDown() }
        binding.departmentSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.departmentSpinner.showDropDown()
        }
    }

    private fun setupRegisterButton() {
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val department = binding.departmentSpinner.text.toString().trim()

            if (!validateInputs(name, username, email, phone, password, department)) {
                return@setOnClickListener
            }

            registerOfficial(name, username, email, phone, password, department)
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
        binding.phoneEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.phoneInputLayout.error = null
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.passwordInputLayout.error = null
        }
        binding.departmentSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.departmentInputLayout.error = null
        }
    }

    private fun validateInputs(name: String, username: String, email: String, phone: String, password: String, department: String): Boolean {
        binding.errorTextView.visibility = View.GONE

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

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (!Patterns.PHONE.matcher(phone).matches()) {
            binding.phoneInputLayout.error = getString(R.string.error_invalid_phone)
            return false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.error_field_required)
            return false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_password_short)
            return false
        }

        if (department.isEmpty()) {
            binding.departmentInputLayout.error = getString(R.string.error_field_required)
            return false
        }

        return true
    }

    private fun registerOfficial(name: String, username: String, email: String, phone: String, password: String, department: String) {
        binding.registerButton.isEnabled = false
        binding.registerButton.text = getString(R.string.registering)

        val registerRequest = RegisterRequest(username, email, password, phone, name, "official", department)
        RetrofitClient.instance.register(registerRequest).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                binding.registerButton.isEnabled = true
                binding.registerButton.text = getString(R.string.register)
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    if (userResponse != null) {
                        UserSession.setUser(userResponse.user, userResponse.token)
                        Snackbar.make(binding.root, "Official registered successfully", Snackbar.LENGTH_LONG).show()
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

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, OfficialMainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}