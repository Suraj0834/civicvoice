package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.civicvoice.databinding.ActivityCreateEventBinding
import com.example.civicvoice.network.Event
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateEventBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        binding.submitEventButton.setOnClickListener {
            submitEvent()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.create_event)
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun submitEvent() {
        val title = binding.etEventTitle.text.toString().trim()
        val description = binding.etEventDescription.text.toString().trim()
        val date = binding.etEventDate.text.toString().trim()
        val location = binding.etEventLocation.text.toString().trim()
        val department = binding.etEventDepartment.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || date.isEmpty() || location.isEmpty() || department.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val user = UserSession.getCurrentUser() ?: run {
            Toast.makeText(this, "Please log in to create an event", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val eventRequest = mapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "location" to location,
            "department" to department,
            "createdBy" to user.userId
        )

        RetrofitClient.instance.createEvent(eventRequest).enqueue(object : Callback<Event> {
            override fun onResponse(call: Call<Event>, response: Response<Event>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateEventActivity, "Event created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateEventActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Event>, t: Throwable) {
                Toast.makeText(this@CreateEventActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}