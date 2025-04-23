package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityTasksBinding
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.network.Task
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TasksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTasksBinding
    private lateinit var tasksAdapter: TasksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadTasks()

        binding.fabAddTask.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "My Tasks"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            emptyList(),
            onDeleteClick = { task ->
                deleteTask(task)
            }
        )
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TasksActivity)
            adapter = tasksAdapter
        }
    }

    private fun loadTasks() {
        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        RetrofitClient.instance.getTasks().enqueue(object : Callback<List<Task>> {
            override fun onResponse(call: Call<List<Task>>, response: Response<List<Task>>) {
                if (response.isSuccessful) {
                    val userTasks = response.body()?.filter { it.officialId == user.userId } ?: emptyList()
                    tasksAdapter.updateTasks(userTasks)
                    binding.emptyState.visibility = if (userTasks.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@TasksActivity, "Failed to load tasks: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Task>>, t: Throwable) {
                Toast.makeText(this@TasksActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteTask(task: Task) {
        RetrofitClient.instance.deleteTask(task._id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    loadTasks()
                    Toast.makeText(this@TasksActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TasksActivity, "Failed to delete task: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@TasksActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadTasks() // Refresh tasks when returning from CreateTaskActivity
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}