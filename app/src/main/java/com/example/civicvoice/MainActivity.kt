package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.civicvoice.databinding.ActivityMainBinding
import com.example.civicvoice.network.CommentRequest
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var postAdapter: PostAdapter
    private lateinit var statsAdapter: StatsAdapter
    private val categories = listOf("All", "Roads", "Sanitation", "Water", "Electricity", "Public Works")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UserSession.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        setupCategories()
        setupStats()
        setupPosts()
        setupFab()
        setupSearch()
        handleAnonymousUser()
        setupSwipeRefresh()
        setupNotificationIcon()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = getString(R.string.app_name)
        binding.hamButton.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.navView)
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_complaints -> {
                    if (isAnonymous()) showAnonymousWarning() else startActivity(Intent(this, ComplaintsActivity::class.java))
                    true
                }
                R.id.nav_events -> {
                    if (isAnonymous()) showAnonymousWarning() else startActivity(Intent(this, EventsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    if (isAnonymous()) showAnonymousWarning() else startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    UserSession.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }.also { binding.drawerLayout.close() }
        }

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.txtUserName).text = UserSession.getCurrentUser()?.name ?: "Guest"
        headerView.findViewById<TextView>(R.id.txtUserEmail).text = UserSession.getCurrentUser()?.email ?: "guest@example.com"
    }

    private fun setupCategories() {
        categoryAdapter = CategoryAdapter(categories) { category ->
            filterPosts(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupStats() {
        statsAdapter = StatsAdapter { filterStatus ->
            filterPostsByStatus(filterStatus)
        }
        binding.rvStats.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = statsAdapter
            setHasFixedSize(true)
        }
        updateStats()
    }

    private fun updateStats() {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val visiblePosts = response.body()?.filter { !it.isAnonymous } ?: emptyList()
                    val stats = listOf(
                        Stat(
                            title = getString(R.string.total_posts),
                            value = visiblePosts.size.toString(),
                            iconRes = R.drawable.ic_total,
                            iconTint = ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
                        ),
                        Stat(
                            title = getString(R.string.pending),
                            value = visiblePosts.count { it.status == "Pending" }.toString(),
                            iconRes = R.drawable.ic_pending,
                            iconTint = ContextCompat.getColor(this@MainActivity, R.color.colorWarning),
                            filterStatus = "Pending"
                        ),
                        Stat(
                            title = getString(R.string.in_progress),
                            value = visiblePosts.count { it.status == "In Progress" }.toString(),
                            iconRes = R.drawable.ic_in_progress,
                            iconTint = ContextCompat.getColor(this@MainActivity, R.color.colorAccent),
                            filterStatus = "In Progress"
                        ),
                        Stat(
                            title = getString(R.string.completed),
                            value = visiblePosts.count { it.status == "Completed" }.toString(),
                            iconRes = R.drawable.ic_completed,
                            iconTint = ContextCompat.getColor(this@MainActivity, R.color.colorSuccess),
                            filterStatus = "Completed"
                        )
                    )
                    statsAdapter.submitList(stats)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load stats: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to load stats: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterPostsBySearch(query)
            }
        })
        if (isAnonymous()) {
            binding.etSearch.isEnabled = false
            binding.etSearch.hint = getString(R.string.search_disabled)
        }
    }

    private fun setupPosts() {
        val user = UserSession.getCurrentUser()
        val userType = when {
            user?.isAnonymous == true -> "anonymous"
            user?.isOfficial == true -> "official"
            else -> "normal"
        }

        postAdapter = PostAdapter(
            userType = userType,
            department = user?.department,
            onPostClick = { post ->
                if (isAnonymous()) {
                    showAnonymousWarning()
                } else {
                    val intent = Intent(this, PostDetailActivity::class.java).apply {
                        putExtra("post_id", post._id)
                        putExtra("CAN_EDIT", userType == "official" && user?.department == post.department)
                    }
                    startActivity(intent)
                }
            },
            onUpvoteClick = { post ->
                if (isAnonymous()) showAnonymousWarning() else upvotePost(post)
            },
            onDownvoteClick = { post ->
                if (isAnonymous()) showAnonymousWarning() else downvotePost(post)
            },
            onAddOfficialComment = { post ->
                if (userType == "official") showOfficialCommentDialog(post)
                else Toast.makeText(this, "Only officials can add official comments", Toast.LENGTH_SHORT).show()
            },
            onMarkInProgress = { post ->
                if (userType == "official") updatePostStatus(post, "In Progress")
                else Toast.makeText(this, "Only officials can update post status", Toast.LENGTH_SHORT).show()
            },
            onMarkCompleted = { post ->
                if (userType == "official") updatePostStatus(post, "Completed")
                else Toast.makeText(this, "Only officials can update post status", Toast.LENGTH_SHORT).show()
            },
            onAddRegularComment = { post, commentText ->
                if (isAnonymous()) showAnonymousWarning() else addRegularComment(post, commentText)
            }
        )

        binding.rvAllPosts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = postAdapter
        }

        loadPosts()
    }

    private fun loadPosts() {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                binding.swipeRefresh.isRefreshing = false
                if (response.isSuccessful) {
                    val visiblePosts = response.body()?.filter { !it.isAnonymous } ?: emptyList()
                    postAdapter.submitList(visiblePosts)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load posts: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Failed to load posts: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun upvotePost(post: Post) {
        RetrofitClient.instance.upvotePost(post._id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    loadPosts()
                    updateStats()
                } else {
                    Toast.makeText(this@MainActivity, "Upvote failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Upvote failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downvotePost(post: Post) {
        RetrofitClient.instance.downvotePost(post._id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    loadPosts()
                    updateStats()
                } else {
                    Toast.makeText(this@MainActivity, "Downvote failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Downvote failed: ${t.message}", Toast.LENGTH_SHORT).show()
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
        RetrofitClient.instance.addComment(post._id, commentRequest).enqueue(object : Callback<com.example.civicvoice.network.Comment> {
            override fun onResponse(call: Call<com.example.civicvoice.network.Comment>, response: Response<com.example.civicvoice.network.Comment>) {
                if (response.isSuccessful) {
                    loadPosts()
                    Toast.makeText(this@MainActivity, "Comment added", Toast.LENGTH_SHORT).show()
                    updateStats()
                } else {
                    Toast.makeText(this@MainActivity, "Comment failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.example.civicvoice.network.Comment>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Comment failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePostStatus(post: Post, status: String) {
        RetrofitClient.instance.updatePostStatus(post._id, mapOf("status" to status)).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    loadPosts()
                    updateStats()
                    Toast.makeText(this@MainActivity, "Status updated to $status", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to update status: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Status update failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showOfficialCommentDialog(post: Post) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_official_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Official Comment")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val commentText = commentInput.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    addOfficialComment(post, commentText)
                }
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
        RetrofitClient.instance.addComment(post._id, commentRequest).enqueue(object : Callback<com.example.civicvoice.network.Comment> {
            override fun onResponse(call: Call<com.example.civicvoice.network.Comment>, response: Response<com.example.civicvoice.network.Comment>) {
                if (response.isSuccessful) {
                    loadPosts()
                    Toast.makeText(this@MainActivity, "Official comment added", Toast.LENGTH_SHORT).show()
                    updateStats()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to add comment: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.example.civicvoice.network.Comment>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to add comment: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupFab() {
        binding.fabAddPost.setOnClickListener {
            if (isAnonymous()) showAnonymousWarning()
            else startActivity(Intent(this, CreatePostActivity::class.java))
        }
    }

    private fun handleAnonymousUser() {
        val isAnonymous = isAnonymous()
        if (isAnonymous) {
            binding.anonymousBadge.visibility = View.VISIBLE
            binding.notificationIcon.visibility = View.GONE
            binding.searchCard.visibility = View.GONE
            binding.fabAddPost.hide()
            binding.anonymousWarningCard.visibility = View.VISIBLE
            binding.btnSignUp.setOnClickListener {
                startActivity(Intent(this, RegisterNormalActivity::class.java))
            }
        } else {
            binding.anonymousBadge.visibility = View.GONE
            binding.notificationIcon.visibility = View.VISIBLE
            binding.searchCard.visibility = View.VISIBLE
            binding.fabAddPost.show()
            binding.anonymousWarningCard.visibility = View.GONE
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPosts()
            updateStats()
        }
    }

    private fun setupNotificationIcon() {
        binding.notificationIcon.setOnClickListener {
            if (isAnonymous()) showAnonymousWarning()
            else startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun filterPosts(category: String) {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val filteredPosts = if (category == "All") {
                        response.body()?.filter { !it.isAnonymous }
                    } else {
                        response.body()?.filter { it.department.equals(category, ignoreCase = true) && !it.isAnonymous }
                    }
                    postAdapter.submitList(filteredPosts)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to filter posts: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to filter posts: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterPostsByStatus(status: String?) {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val filteredPosts = if (status == null) {
                        response.body()?.filter { !it.isAnonymous }
                    } else {
                        response.body()?.filter { it.status == status && !it.isAnonymous }
                    }
                    postAdapter.submitList(filteredPosts)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to filter posts: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to filter posts: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterPostsBySearch(query: String) {
        RetrofitClient.instance.getPosts().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val filteredPosts = response.body()?.filter { post ->
                        !post.isAnonymous && (
                                post.title.contains(query, true) ||
                                        post.description.contains(query, true) ||
                                        post.location.contains(query, true) ||
                                        post.hashtags.any { it.contains(query, true) }
                                )
                    }
                    postAdapter.submitList(filteredPosts)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to search posts: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed to search posts: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAnonymousWarning() {
        Toast.makeText(this, "Please sign up to access this feature", Toast.LENGTH_SHORT).show()
        binding.anonymousWarningCard.visibility = View.VISIBLE
    }

    private fun isAnonymous(): Boolean {
        return UserSession.getCurrentUser()?.isAnonymous == true
    }
}