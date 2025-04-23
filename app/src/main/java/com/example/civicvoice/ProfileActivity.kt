package com.example.civicvoice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.civicvoice.databinding.ActivityProfileBinding
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.UserResponse
import com.example.civicvoice.utils.UserSession
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(this) // Initialize UserSession
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        setupToolbar()
        loadUserData()

        binding.changePhotoButton.setOnClickListener {
            openGallery()
        }

        binding.saveProfileButton.setOnClickListener {
            saveUserData()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            if (selectedImageUri != null) {
                binding.profileImageView.setImageURI(selectedImageUri)
            }
        }
    }

    private fun loadUserData() {
        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "No user session. Please log in.", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }
        binding.nameEditText.setText(user.username)
        binding.emailEditText.setText(user.email)
        binding.phoneEditText.setText(user.phone)

        // Load profile image: prefer local SharedPreferences, then server URL
        val encodedImage = sharedPreferences.getString("profileImage", null)
        if (encodedImage != null) {
            val byteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            binding.profileImageView.setImageBitmap(bitmap)
        } else if (user.profilePic != null) {
            Glide.with(this)
                .load(user.profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_person)
                .into(binding.profileImageView)
        }
    }

    private fun saveUserData() {
        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "No user session. Please log in.", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }
        val username = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()

        // Validate inputs
        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Username and email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf(
            "username" to username.toRequestBody("text/plain".toMediaTypeOrNull()),
            "email" to email.toRequestBody("text/plain".toMediaTypeOrNull()),
            "phone" to phone.toRequestBody("text/plain".toMediaTypeOrNull())
        )

        val imagePart = selectedImageUri?.let {
            val file = uriToFile(it)
            MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
        }

        RetrofitClient.instance.updateProfile(data, imagePart).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    if (userResponse != null) {
                        UserSession.setUser(userResponse.user, userResponse.token)
                        saveImageLocally(selectedImageUri)
                        Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Empty response from server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ProfileActivity, "Failed to update profile: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "profile_image_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun saveImageLocally(imageUri: Uri?) {
        imageUri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
            sharedPreferences.edit().putString("profileImage", encodedImage).apply()
            inputStream?.close()
        }
    }

    private fun navigateToLogin() {
        UserSession.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}