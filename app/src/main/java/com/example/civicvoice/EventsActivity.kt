package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityEventsBinding
import com.example.civicvoice.network.Event
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventsBinding
    private lateinit var adapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadEvents()

        // Hide FAB for non-official users
        val currentUser = UserSession.getCurrentUser()
        binding.fabAddEvent.isVisible = currentUser?.userType == "official"
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.events)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = EventsAdapter(emptyList())
        binding.eventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EventsActivity)
            adapter = this@EventsActivity.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddEvent.setOnClickListener {
            if (UserSession.getCurrentUser()?.isAnonymous == true) {
                Toast.makeText(this, "Please sign up to create an event", Toast.LENGTH_SHORT).show()
            } else if (UserSession.getCurrentUser()?.isOfficial != true) {
                Toast.makeText(this, "Only officials can create events", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, CreateEventActivity::class.java))
            }
        }
    }

    private fun loadEvents() {
        RetrofitClient.instance.getEvents().enqueue(object : Callback<List<Event>> {
            override fun onResponse(call: Call<List<Event>>, response: Response<List<Event>>) {
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    adapter.updateEvents(events)
                    binding.emptyState.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@EventsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Event>>, t: Throwable) {
                Toast.makeText(this@EventsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }
}