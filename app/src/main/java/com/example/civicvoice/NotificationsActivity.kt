package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityNotificationsBinding
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.Notification
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.notifications)
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onClick = { notification ->
                markNotificationAsRead(notification)
                if (notification.postId != null) {
                    startActivity(Intent(this@NotificationsActivity, PostDetailActivity::class.java).apply {
                        putExtra("post_id", notification.postId)
                    })
                } else {
                    Toast.makeText(this@NotificationsActivity, "No associated post", Toast.LENGTH_SHORT).show()
                }
            },
            onSwipeToDelete = { notification ->
                deleteNotification(notification)
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
            addItemDecoration(DividerItemDecoration(this@NotificationsActivity, DividerItemDecoration.VERTICAL))
            notificationAdapter.attachSwipeToDelete(this)
        }
    }

    private fun markNotificationAsRead(notification: Notification) {
        RetrofitClient.instance.markNotificationAsRead(notification._id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    notification.isRead = true
                    notificationAdapter.notifyItemChanged(notificationAdapter.currentList.indexOf(notification))
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Failed to mark as read: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteNotification(notification: Notification) {
        RetrofitClient.instance.deleteNotification(notification._id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    loadNotifications()
                    Toast.makeText(this@NotificationsActivity, "Notification deleted", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Failed to delete: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadNotifications() {
        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        RetrofitClient.instance.getNotifications().enqueue(object : Callback<List<Notification>> {
            override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                if (response.isSuccessful) {
                    val userNotifications = response.body()
                        ?.filter { it.userId == user.userId }
                        ?.sortedByDescending { it.timestamp } ?: emptyList()
                    notificationAdapter.submitList(userNotifications)
                    binding.emptyState.visibility = if (userNotifications.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@NotificationsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Failed to load notifications: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
}