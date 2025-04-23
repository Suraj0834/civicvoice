package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityComplaintsBinding
import com.example.civicvoice.network.Complaint
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ComplaintsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintsBinding
    private lateinit var adapter: ComplaintsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadComplaints()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar.root
        setSupportActionBar(toolbar as Toolbar?)
        supportActionBar?.apply {
            title = getString(R.string.complaints)
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ComplaintsAdapter(
            emptyList(),
            onDeleteClick = { complaint -> deleteComplaint(complaint) },
            onFeedbackClick = { complaint -> showFeedbackDialog(complaint) }
        )
        binding.complaintsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ComplaintsActivity)
            adapter = this@ComplaintsActivity.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadComplaints()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun loadComplaints() {
        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "Please log in to view complaints", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        RetrofitClient.instance.getComplaints().enqueue(object : Callback<List<Complaint>> {
            override fun onResponse(call: Call<List<Complaint>>, response: Response<List<Complaint>>) {
                if (response.isSuccessful) {
                    val userComplaints = response.body()?.filter { it.userId == user.userId } ?: emptyList()
                    adapter.updateComplaints(userComplaints)
                    if (userComplaints.isEmpty()) {
                        Toast.makeText(this@ComplaintsActivity, "No complaints registered yet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ComplaintsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Complaint>>, t: Throwable) {
                Toast.makeText(this@ComplaintsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteComplaint(complaint: Complaint) {
        RetrofitClient.instance.deleteComplaint(complaint._id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ComplaintsActivity, "Complaint deleted", Toast.LENGTH_SHORT).show()
                    loadComplaints()
                } else {
                    Toast.makeText(this@ComplaintsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@ComplaintsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFeedbackDialog(complaint: Complaint) {
        val intent = Intent(this, FeedbackActivity::class.java).apply {
            putExtra("complaint_id", complaint._id)
        }
        startActivity(intent)
        Toast.makeText(this, "Feedback for ${complaint.title}", Toast.LENGTH_SHORT).show()
    }
}