package com.example.civicvoice

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityCreatePostBinding
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.PostRequestBody
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class CreatePostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePostBinding
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private val TAG = "CreatePostActivity"
    private var createPostCall: Call<Post>? = null
    private var uploadImageCall: Call<Map<String, String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(this)
        if (!UserSession.isValid() || UserSession.getCurrentUser()?.isAnonymous == true) {
            showError("Please sign up or log in to create a post")
            navigateToLogin()
            return
        }
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDepartmentSpinner()
        setupClickListeners()
        setupAnimations()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.create_post_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupDepartmentSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.departments,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.departmentSpinner.adapter = adapter

        UserSession.getCurrentUser()?.department?.let { department ->
            val departments = resources.getStringArray(R.array.departments)
            val position = departments.indexOf(department)
            if (position >= 0) {
                binding.departmentSpinner.setSelection(position)
            }
        }
    }

    private fun setupClickListeners() {
        binding.selectImageButton.setOnClickListener {
            openGallery()
        }

        binding.submitPostButton.setOnClickListener {
            submitPost()
        }

        binding.postImageView.setOnClickListener {
            if (selectedImageUri != null) {
                selectedImageUri = null
                binding.postImageView.setImageDrawable(null)
                binding.postImageView.animate().alpha(0f).setDuration(300).start()
            }
        }
    }

    private fun setupAnimations() {
        binding.cardView.alpha = 0f
        binding.cardView.animate().alpha(1f).setDuration(400).start()
        binding.titleInputLayout.animate().translationY(0f).alpha(1f).setDuration(300).start()
        binding.descriptionInputLayout.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(100).start()
        binding.hashtagsInputLayout.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(150).start()
        binding.departmentInputLayout.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(200).start()
        binding.locationInputLayout.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(250).start()
        binding.selectImageButton.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(300).start()
        binding.submitPostButton.animate().translationY(0f).alpha(1f).setDuration(300).setStartDelay(400).start()
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
                binding.postImageView.setImageURI(selectedImageUri)
                binding.postImageView.animate().alpha(1f).setDuration(300).start()
            }
        }
    }

    private fun submitPost() {
        val user = UserSession.getCurrentUser() ?: run {
            showError("Please log in")
            navigateToLogin()
            return
        }
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val hashtags = binding.hashtagsEditText.text.toString().trim()
        val department = binding.departmentSpinner.selectedItem.toString()
        val location = binding.locationEditText.text.toString().trim()

        if (!validateInputs(title, description, department, location)) return

        showLoading(true)

        if (selectedImageUri != null) {
            uploadImage { imageId ->
                if (imageId != null) {
                    createPost(title, description, location, department, hashtags, imageId)
                } else {
                    showLoading(false)
                    showError("Failed to upload image")
                }
            }
        } else {
            createPost(title, description, location, department, hashtags, null)
        }
    }

    private fun uploadImage(callback: (String?) -> Unit) {
        val file = selectedImageUri?.let { uriToFile(it) } ?: return callback(null)
        val imagePart = MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))

        uploadImageCall = RetrofitClient.instance.uploadImage(imagePart)
        uploadImageCall?.enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                uploadImageCall = null
                if (response.isSuccessful) {
                    val imageId = response.body()?.get("imageId")
                    callback(imageId)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                uploadImageCall = null
                callback(null)
            }
        })
    }

    private fun createPost(title: String, description: String, location: String, department: String, hashtags: String, imageId: String?) {
        val data = PostRequestBody(
            title = title,
            description = description,
            location = location,
            department = department,
            hashtags = extractHashtags(hashtags),
            imageId = imageId
        )

        createPostCall = RetrofitClient.instance.createPost(data)
        createPostCall?.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                showLoading(false)
                createPostCall = null
                if (response.isSuccessful) {
                    Toast.makeText(this@CreatePostActivity, "Post created successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else if (response.code() == 403) {
                        showError("Anonymous users cannot post")
                        navigateToLogin()
                    } else {
                        showError("Failed to create post: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                showLoading(false)
                createPostCall = null
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun validateInputs(title: String, description: String, department: String, location: String): Boolean {
        binding.titleInputLayout.error = null
        binding.descriptionInputLayout.error = null
        binding.hashtagsInputLayout.error = null
        binding.departmentInputLayout.error = null
        binding.locationInputLayout.error = null

        return when {
            title.isEmpty() -> {
                binding.titleInputLayout.error = getString(R.string.error_field_required)
                false
            }
            description.isEmpty() -> {
                binding.descriptionInputLayout.error = getString(R.string.error_field_required)
                false
            }
            department.isEmpty() || department == "Select Department" -> {
                binding.departmentInputLayout.error = getString(R.string.error_field_required)
                false
            }
            location.isEmpty() -> {
                binding.locationInputLayout.error = getString(R.string.error_field_required)
                false
            }
            else -> true
        }
    }

    private fun extractHashtags(input: String): List<String> {
        return input.split(" ").filter { it.startsWith("#") }.map { it.removePrefix("#").trim() }.filter { it.isNotEmpty() }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "post_image_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun showLoading(show: Boolean) {
        binding.submitPostButton.isEnabled = !show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { /* Dismiss */ }
            .setBackgroundTint(resolveColorAttr(android.R.attr.colorError))
            .setTextColor(resolveColorAttr(com.google.android.material.R.attr.colorOnError))
            .show()
    }

    private fun navigateToLogin() {
        UserSession.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun resolveColorAttr(attr: Int): Int {
        val typedArray = theme.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    override fun onDestroy() {
        super.onDestroy()
        createPostCall?.cancel()
        uploadImageCall?.cancel()
        createPostCall = null
        uploadImageCall = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}