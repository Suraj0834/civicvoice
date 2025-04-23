package com.example.civicvoice

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityCreateTaskBinding
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.Task
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateTaskBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSubmitButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar.toolbar) // Access the Toolbar widget
        supportActionBar?.apply {
            title = "Create Task"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitTask.setOnClickListener {
            val title = binding.etTaskTitle.text.toString().trim()
            val description = binding.etTaskDescription.text.toString().trim()
            val dueDate = binding.etDueDate.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || dueDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = UserSession.getCurrentUser() ?: run {
                Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val task = mapOf(
                "title" to title,
                "description" to description,
                "dueDate" to dueDate,
                "status" to "Pending",
                "officialId" to user.userId
            )

            RetrofitClient.instance.createTask(task).enqueue(object : Callback<Task> {
                override fun onResponse(call: Call<Task>, response: Response<Task>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CreateTaskActivity, "Task created", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateTaskActivity, "Failed to create task: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Task>, t: Throwable) {
                    Toast.makeText(this@CreateTaskActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}