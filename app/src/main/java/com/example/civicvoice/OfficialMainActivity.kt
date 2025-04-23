package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityOfficialMainBinding
import com.example.civicvoice.network.*
import com.example.civicvoice.utils.UserSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OfficialMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityOfficialMainBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var statsAdapter: StatsAdapter
    private var currentFilter: String? = null
    private var currentQuery: String = ""
    private var currentSortOption: String = "Newest First"
    private var isActivityActive = false
    private val pendingCalls = mutableListOf<Call<*>>()

    private val createTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            refreshData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(this)
        if (!UserSession.isValid()) {
            showError("Invalid session. Please log in again.")
            navigateToLogin()
            return
        }
        binding = ActivityOfficialMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isActivityActive = true

        setupToolbar()
        setupNavigationDrawer()
        setupRecyclerView()
        setupStatsRecyclerView()
        setupAddTaskButton()
        refreshData()
        updateNavHeader()
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingCalls.forEach { it.cancel() }
        pendingCalls.clear()
        isActivityActive = false
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            title = getString(R.string.official_dashboard)
        }
    }

    private fun setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener(this)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun updateNavHeader() {
        try {
            val user = UserSession.getCurrentUser() ?: run {
                showError("Please log in")
                navigateToLogin()
                return
            }
            val headerView = binding.navView.getHeaderView(0)
            val userName = headerView.findViewById<TextView>(R.id.txtUserName2)
            val userEmail = headerView.findViewById<TextView>(R.id.txtUserEmail2)
            userName?.text = user.name ?: "Official User"
            userEmail?.text = user.email ?: "official@example.com"
        } catch (e: Exception) {
            Log.e("OfficialMainActivity", "Error updating nav header", e)
            if (isActivityActive) {
                showError("Failed to update profile: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        val department = intent.getStringExtra("department") ?: ""
        postAdapter = PostAdapter(
            userType = "official",
            department = department,
            onPostClick = { post ->
                Intent(this, PostDetailActivity::class.java).apply {
                    putExtra("post_id", post._id)
                    putExtra("CAN_EDIT", true)
                    startActivity(this)
                }
            },
            onUpvoteClick = { /* Officials can't upvote */ },
            onDownvoteClick = { /* Officials can't downvote */ },
            onAddOfficialComment = { post -> showOfficialCommentDialog(post) },
            onMarkInProgress = { post -> markPostInProgress(post) },
            onMarkCompleted = { post -> markPostCompleted(post) },
            onAddRegularComment = { post, comment -> addRegularComment(post, comment) }
        ).apply {
            setHasStableIds(true)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OfficialMainActivity)
            adapter = postAdapter
            setHasFixedSize(true)
            addItemDecoration(LinearHorizontalSpacingDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
        }
    }

    private fun setupStatsRecyclerView() {
        statsAdapter = StatsAdapter { filterStatus ->
            currentFilter = filterStatus
            applyFilters()
        }
        binding.statsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@OfficialMainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = statsAdapter
            setHasFixedSize(true)
            addItemDecoration(LinearHorizontalSpacingDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
        }
    }

    private fun setupAddTaskButton() {
        binding.btnAddTask.setOnClickListener {
            createTaskLauncher.launch(Intent(this, AddTaskActivity::class.java))
        }
    }

    private fun refreshData() {
        binding.progressBar.visibility = View.VISIBLE
        loadPosts()
        updateStats()
    }

    private fun loadPosts() {
        val call = RetrofitClient.instance.getPosts()
        pendingCalls.add(call)
        call.enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    applyFilters(response.body() ?: emptyList())
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to load posts: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                binding.progressBar.visibility = View.GONE
                showError("Network error: ${t.message}")
            }
        })
    }

    private fun updateStats() {
        val department = intent.getStringExtra("department") ?: ""
        val call = RetrofitClient.instance.getPosts()
        pendingCalls.add(call)
        call.enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                if (response.isSuccessful) {
                    val departmentPosts = response.body()?.filter {
                        it.department == department && !it.isAnonymous && !isOfficialUser(it.userId)
                    } ?: emptyList()
                    val stats = listOf(
                        Stat(
                            title = getString(R.string.total_posts),
                            value = departmentPosts.size.toString(),
                            iconRes = R.drawable.ic_total,
                            iconTint = ContextCompat.getColor(this@OfficialMainActivity, R.color.colorPrimary)
                        ),
                        Stat(
                            title = getString(R.string.pending),
                            value = departmentPosts.count { it.status == "Pending" }.toString(),
                            iconRes = R.drawable.ic_pending,
                            iconTint = ContextCompat.getColor(this@OfficialMainActivity, R.color.colorWarning),
                            filterStatus = "Pending"
                        ),
                        Stat(
                            title = getString(R.string.in_progress),
                            value = departmentPosts.count { it.status == "In Progress" }.toString(),
                            iconRes = R.drawable.ic_in_progress,
                            iconTint = ContextCompat.getColor(this@OfficialMainActivity, R.color.colorAccent),
                            filterStatus = "In Progress"
                        ),
                        Stat(
                            title = getString(R.string.completed),
                            value = departmentPosts.count { it.status == "Completed" }.toString(),
                            iconRes = R.drawable.ic_completed,
                            iconTint = ContextCompat.getColor(this@OfficialMainActivity, R.color.colorSuccess),
                            filterStatus = "Completed"
                        )
                    )
                    statsAdapter.submitList(stats)
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to load stats: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                showError("Failed to load stats: ${t.message}")
            }
        })
    }

    private fun applyFilters(posts: List<Post> = emptyList()) {
        val department = intent.getStringExtra("department") ?: ""
        var filteredPosts = posts.filter { post ->
            post.department == department && !post.isAnonymous && !isOfficialUser(post.userId)
        }

        currentFilter?.let { status ->
            filteredPosts = filteredPosts.filter { it.status == status }
        }

        if (currentQuery.isNotEmpty()) {
            filteredPosts = filteredPosts.filter { post ->
                post.title.contains(currentQuery, true) ||
                        post.description.contains(currentQuery, true) ||
                        post.location.contains(currentQuery, true) ||
                        post.hashtags.any { it.contains(currentQuery, true) }
            }
        }

        filteredPosts = when (currentSortOption) {
            "Newest First" -> filteredPosts.sortedByDescending { it.date }
            "Oldest First" -> filteredPosts.sortedBy { it.date }
            "Most Upvoted" -> filteredPosts.sortedByDescending { it.upvotes }
            "Most Comments" -> filteredPosts.sortedByDescending { it.comments.size + it.officialComments.size }
            else -> filteredPosts
        }

        postAdapter.submitList(filteredPosts)
    }

    private fun isOfficialUser(userId: String): Boolean {
        val user = UserSession.getCurrentUser()
        return user?.userId == userId && user.isOfficial
    }

    private fun showOfficialCommentDialog(post: Post) {
        if (!isActivityActive) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_official_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Official Comment")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val commentText = commentInput.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    addOfficialComment(post, commentText)
                } else {
                    Snackbar.make(binding.root, "Comment cannot be empty", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Templates") { _, _ -> showTemplatesDialog(post) }
            .show()
    }

    private fun showTemplatesDialog(post: Post) {
        if (!isActivityActive) return
        val templates = resources.getStringArray(R.array.official_comment_templates)
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Template")
            .setItems(templates) { _, which ->
                addOfficialComment(post, templates[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addOfficialComment(post: Post, commentText: String) {
        val user = UserSession.getCurrentUser() ?: return
        val commentRequest = CommentRequest(
            content = commentText,
            isOfficial = true,
            author = user.username
        )
        val call = RetrofitClient.instance.addComment(post._id, commentRequest)
        pendingCalls.add(call)
        call.enqueue(object : Callback<com.example.civicvoice.network.Comment> {
            override fun onResponse(call: Call<com.example.civicvoice.network.Comment>, response: Response<com.example.civicvoice.network.Comment>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                if (response.isSuccessful) {
                    loadPosts()
                    Snackbar.make(binding.root, "Official comment added", Snackbar.LENGTH_SHORT).show()
                    NotificationHelper.sendNotification(
                        this@OfficialMainActivity,
                        post.userId,
                        "Official Update: ${post.title}",
                        commentText,
                        post.title,
                        "official"
                    )
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to add comment: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<com.example.civicvoice.network.Comment>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                showError("Failed to add comment: ${t.message}")
            }
        })
    }

    private fun markPostInProgress(post: Post) {
        val call = RetrofitClient.instance.updatePostStatus(post._id, mapOf("status" to "In Progress"))
        pendingCalls.add(call)
        call.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                if (response.isSuccessful) {
                    loadPosts()
                    updateStats()
                    Snackbar.make(binding.root, "Marked as In Progress", Snackbar.LENGTH_SHORT).show()
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to update status: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                showError("Failed to update status: ${t.message}")
            }
        })
    }

    private fun markPostCompleted(post: Post) {
        val call = RetrofitClient.instance.updatePostStatus(post._id, mapOf("status" to "Completed"))
        pendingCalls.add(call)
        call.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                if (response.isSuccessful) {
                    loadPosts()
                    updateStats()
                    Snackbar.make(binding.root, "Marked as Completed", Snackbar.LENGTH_SHORT).show()
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to update status: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                showError("Failed to update status: ${t.message}")
            }
        })
    }

    private fun addRegularComment(post: Post, commentText: String) {
        val user = UserSession.getCurrentUser() ?: return
        val commentRequest = CommentRequest(
            content = commentText,
            isOfficial = false,
            author = user.username
        )
        val call = RetrofitClient.instance.addComment(post._id, commentRequest)
        pendingCalls.add(call)
        call.enqueue(object : Callback<com.example.civicvoice.network.Comment> {
            override fun onResponse(call: Call<com.example.civicvoice.network.Comment>, response: Response<com.example.civicvoice.network.Comment>) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                if (response.isSuccessful) {
                    loadPosts()
                    Snackbar.make(binding.root, "Comment added", Snackbar.LENGTH_SHORT).show()
                } else {
                    if (response.code() == 401) {
                        showError("Session expired. Please log in again.")
                        navigateToLogin()
                    } else {
                        showError("Failed to add comment: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<com.example.civicvoice.network.Comment>, t: Throwable) {
                if (!isActivityActive) return
                pendingCalls.remove(call)
                showError("Failed to add comment: ${t.message}")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        (searchItem?.actionView as? SearchView)?.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = true
                override fun onQueryTextChange(newText: String?): Boolean {
                    currentQuery = newText?.trim() ?: ""
                    applyFilters()
                    return true
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter -> {
                showFilterDialog()
                true
            }
            R.id.refresh -> {
                refreshData()
                true
            }
            R.id.sort -> {
                showSortDialog()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        if (!isActivityActive) return
        val options = arrayOf("All", "Pending", "In Progress", "Completed")
        MaterialAlertDialogBuilder(this)
            .setTitle("Filter Posts")
            .setItems(options) { _, which ->
                currentFilter = when (which) {
                    1 -> "Pending"
                    2 -> "In Progress"
                    3 -> "Completed"
                    else -> null
                }
                applyFilters()
            }
            .show()
    }

    private fun showSortDialog() {
        if (!isActivityActive) return
        val options = arrayOf("Newest First", "Oldest First", "Most Upvoted", "Most Comments")
        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Posts")
            .setItems(options) { _, which ->
                currentSortOption = options[which]
                applyFilters()
                Snackbar.make(binding.root, "Sorted by $currentSortOption", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {}
            R.id.offical_notification -> startActivity(Intent(this, NotificationsActivity::class.java))
            R.id.nav_tasks -> startActivity(Intent(this, TasksActivity::class.java))
            R.id.nav_Event -> startActivity(Intent(this, EventsActivity::class.java))
            R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_logout -> {
                UserSession.logout()
                navigateToLogin()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showHelpDialog() {
        if (!isActivityActive) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Help")
            .setMessage("Contact support at support@civicvoice.com")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String?) {
        if (!isActivityActive || isFinishing) return
        Log.e("OfficialMainActivity", "Error: $message")
        Snackbar.make(binding.root, message ?: "An unexpected error occurred", Snackbar.LENGTH_LONG)
            .setAction("OK") { /* Dismiss */ }
            .show()
    }

    private fun navigateToLogin() {
        UserSession.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}