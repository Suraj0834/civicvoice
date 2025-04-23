package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityLoginBinding
import com.example.civicvoice.network.AppUser
import com.example.civicvoice.network.LoginRequest
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.UserResponse
import com.example.civicvoice.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(this) // Initialize UserSession to load persisted data
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputListeners()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            attemptLogin()
        }

        binding.forgotPasswordTextView.setOnClickListener {
            Snackbar.make(binding.root, "Forgot Password not implemented yet", Snackbar.LENGTH_SHORT).show()
        }

        binding.anonymousLoginButton.setOnClickListener {
            loginAsAnonymous()
        }
    }

    private fun setupInputListeners() {
        binding.usernameEditText.addTextChangedListener(inputWatcher)
        binding.passwordEditText.addTextChangedListener(inputWatcher)
    }

    private val inputWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            binding.usernameInputLayout.error = null
            binding.passwordInputLayout.error = null
            binding.errorTextView.visibility = View.GONE
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun attemptLogin() {
        val username = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (!validateInputs(username, password)) return

        showLoading(true)
        val loginRequest = LoginRequest(username, password)
        Log.d(TAG, "Login request: username=$username")
        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    if (userResponse != null) {
                        Log.d(TAG, "Login successful: ${userResponse.user.username}, type: ${userResponse.user.userType}")
                        UserSession.setUser(userResponse.user, userResponse.token)
                        navigateBasedOnUserType(userResponse.user)
                    } else {
                        Log.e(TAG, "Empty response body")
                        showLoginError("Empty response from server")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Login failed: ${response.code()} - $errorBody")
                    val errorMessage = when {
                        response.code() == 400 && errorBody?.contains("User not found") == true -> "Username does not exist"
                        response.code() == 400 && errorBody?.contains("Invalid credentials") == true -> "Incorrect username or password"
                        response.code() == 500 -> "Server error, please try again later"
                        else -> "Login failed: ${response.code()} - $errorBody"
                    }
                    showLoginError(errorMessage)
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network failure: ${t.message}", t)
                showLoginError("Network error: ${t.message}")
            }
        })
    }

    private fun validateInputs(username: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                binding.usernameInputLayout.error = getString(R.string.error_username_required)
                false
            }
            password.isEmpty() -> {
                binding.passwordInputLayout.error = getString(R.string.error_password_required)
                false
            }
            else -> true
        }
    }

    private fun navigateBasedOnUserType(user: AppUser) {
        val intent = when (user.userType.lowercase()) {
            "official" -> Intent(this, OfficialMainActivity::class.java).apply {
                putExtra("department", user.department ?: "")
            }
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun loginAsAnonymous() {
        showLoading(true)
        RetrofitClient.instance.anonymousLogin().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    if (userResponse != null) {
                        Log.d(TAG, "Anonymous login successful: ${userResponse.user.username}")
                        UserSession.setUser(userResponse.user, userResponse.token)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("is_anonymous", true)
                        })
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        Log.e(TAG, "Empty response body")
                        showLoginError("Empty response from server")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Anonymous login failed: ${response.code()} - $errorBody")
                    showLoginError("Anonymous login failed: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                showLoading(false)
                Log.e(TAG, "Network failure: ${t.message}", t)
                showLoginError("Network error: ${t.message}")
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding.loginButton.isEnabled = !show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showLoginError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}