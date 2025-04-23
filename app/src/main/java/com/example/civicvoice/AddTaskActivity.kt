package com.example.civicvoice

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.civicvoice.databinding.ActivityAddTaskBinding
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.Task
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class AddTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTaskBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDatePicker()
        setupListeners()
        setupAnimations()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.add_task)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDatePicker() {
        binding.dueDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val date = String.format("%04d-%02d-%02d", year, month + 1, day)
                    binding.dueDateEditText.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupListeners() {
        binding.submitTaskButton.setOnClickListener {
            submitTask()
        }
    }

    private fun setupAnimations() {
        binding.submitTaskButton.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(100)
            .withEndAction {
                binding.submitTaskButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun submitTask() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val dueDate = binding.dueDateEditText.text.toString().trim()

        binding.titleInputLayout.error = null
        binding.descriptionInputLayout.error = null
        binding.dueDateInputLayout.error = null

        var isValid = true
        if (title.isEmpty()) {
            binding.titleInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        }
        if (description.isEmpty()) {
            binding.descriptionInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        }
        if (dueDate.isEmpty()) {
            binding.dueDateInputLayout.error = getString(R.string.error_field_required)
            isValid = false
        }

        if (!isValid) return

        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "Please log in to create a task", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val taskRequest = mapOf(
            "title" to title,
            "description" to description,
            "dueDate" to dueDate,
            "createdBy" to user.userId
        )

        binding.progressIndicator.isVisible = true
        binding.submitTaskButton.isEnabled = false

        RetrofitClient.instance.createTask(taskRequest).enqueue(object : Callback<Task> {
            override fun onResponse(call: Call<Task>, response: Response<Task>) {
                binding.progressIndicator.isVisible = false
                binding.submitTaskButton.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AddTaskActivity, "Task added successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AddTaskActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Task>, t: Throwable) {
                binding.progressIndicator.isVisible = false
                binding.submitTaskButton.isEnabled = true
                Toast.makeText(this@AddTaskActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}