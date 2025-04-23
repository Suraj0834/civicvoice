package com.example.civicvoice

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.civicvoice.network.Comment
import com.example.civicvoice.databinding.ActivityPostDetailBinding
import com.example.civicvoice.network.CommentRequest
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.UserSession
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var post: Post
    private var canEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PostDetailActivity", "Inflating ActivityPostDetailBinding")
        UserSession.init(this)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("PostDetailActivity", "Layout set: activity_post_detail.xml")

        val postId = intent.getStringExtra("post_id") ?: return finish()
        Log.d("PostDetailActivity", "Activity launched with post_id: $postId")
        canEdit = intent.getBooleanExtra("CAN_EDIT", false)

        setupAnonymousUserRestrictions()
        setupToolbar()
        setupButtonListeners()
        loadPost(postId)
    }

    private fun setupAnonymousUserRestrictions() {
        if (UserSession.getCurrentUser()?.isAnonymous == true) {
            binding.upvoteButton.isEnabled = false
            binding.downvoteButton.isEnabled = false
            binding.commentInput.isEnabled = false
            binding.postCommentButton.isEnabled = false
            binding.upvoteButton.alpha = 0.5f
            binding.downvoteButton.alpha = 0.5f
            binding.commentInput.hint = getString(R.string.sign_up_to_comment)
            binding.postCommentButton.alpha = 0.5f
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.post_details)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupButtonListeners() {
        binding.shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, post.title)
                putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.description}\nLocation: ${post.location}")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Post"))
        }

        binding.upvoteButton.setOnClickListener {
            if (UserSession.getCurrentUser()?.isAnonymous == true) {
                Toast.makeText(this, "Sign up to vote", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            upvotePost()
        }

        binding.downvoteButton.setOnClickListener {
            if (UserSession.getCurrentUser()?.isAnonymous == true) {
                Toast.makeText(this, "Sign up to vote", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            downvotePost()
        }

        binding.postCommentButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString()
            if (commentText.isNotBlank()) {
                postComment(commentText)
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.viewAllCommentsButton.setOnClickListener {
            if (UserSession.getCurrentUser()?.isAnonymous == true) {
                Toast.makeText(this, "Please sign up to view comments", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CommentsActivity::class.java).apply {
                    putExtra("post_id", post._id)
                }
                startActivity(intent)
            }
        }

        binding.addActionButton.setOnClickListener {
            Toast.makeText(this, "Add action clicked", Toast.LENGTH_SHORT).show()
        }

        binding.attachImageButton.setOnClickListener {
            Toast.makeText(this, "Attach image clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPost(postId: String) {
        RetrofitClient.instance.getPostById(postId).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    post = response.body() ?: return finish()
                    setupPostDetails()
                    setupActionButton()
                    setupImageCarousel()
                    setupComments()
                    setupOfficialActions()
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to load post: ${response.code()}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun setupPostDetails() {
        Log.d("PostDetailActivity", "Setting up post details: ${post.title}")
        binding.postTitle.text = post.title
        binding.postDescription.text = post.description
        binding.postDepartment.text = getString(R.string.department_prefix, post.department)
        binding.postStatus.text = getString(R.string.status_prefix, post.status)
        binding.postLocation.text = getString(R.string.location_prefix, post.location)
        binding.upvoteCount.text = post.upvotes.toString()
        binding.downvoteCount.text = post.downvotes.toString()
        binding.postUser.text = if (post.isAnonymous) "Anonymous" else post.username
        binding.postDate.text = formatTimestamp(post.date)

        binding.statusProgress.isVisible = true
        binding.statusProgress.progress = when (post.status) {
            "Pending" -> 0
            "In Progress" -> 50
            "Completed" -> 100
            else -> 0
        }

        if (post.imageId?.isNotEmpty() == true) {
            Log.d("PostDetailActivity", "Loading image: ${post.imageId}")
            Glide.with(this)
                .load("${RetrofitClient.BASE_URL}images/${post.imageId}")
                .placeholder(R.drawable.image_placeholder)
                .into(binding.heroImage)
        } else {
            Log.d("PostDetailActivity", "No image to load")
        }
    }

    private fun setupImageCarousel() {
        val images = if (post.imageId?.isNotEmpty() == true) listOf(post.imageId) else emptyList()
        if (images.isNotEmpty()) {
            binding.imageCarousel.isVisible = true
            binding.carouselIndicators.isVisible = images.size > 1
            binding.imageCarousel.adapter = ImageCarouselAdapter(images as List<String>)
            TabLayoutMediator(binding.carouselIndicators, binding.imageCarousel) { _, _ -> }.attach()
        } else {
            binding.imageCarousel.isVisible = false
            binding.carouselIndicators.isVisible = false
        }
    }

    private fun setupComments() {
        binding.commentsLoading.isVisible = true
        binding.commentsContainer.removeAllViews()
        binding.pinnedCommentCard.isVisible = false

        val allComments = (post.comments + post.officialComments).sortedByDescending { it.date.toLongOrNull() ?: 0 }
        binding.commentsLoading.isVisible = false

        allComments.forEach { comment ->
            if (comment.isOfficial) {
                binding.pinnedCommentCard.isVisible = true
                binding.pinnedCommentAuthor.text = comment.username
                binding.pinnedCommentText.text = comment.content
                binding.pinnedCommentLabel.text = getString(R.string.official_comment)
                binding.pinnedCommentAvatar.setImageResource(
                    if (comment.isOfficial) R.drawable.ic_official_badge else R.drawable.ic_profile_placeholder
                )
            } else {
                val commentView = LayoutInflater.from(this)
                    .inflate(R.layout.item_comment, binding.commentsContainer, false)
                commentView.findViewById<TextView>(R.id.commentAuthor).text = comment.username
                commentView.findViewById<TextView>(R.id.commentText).text = comment.content
                commentView.findViewById<ImageView>(R.id.officialBadge).isVisible = comment.isOfficial
                binding.commentsContainer.addView(commentView)
                commentView.alpha = 0f
                commentView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        }
        binding.viewAllCommentsButton.isVisible = allComments.size > 1
    }

    private fun setupOfficialActions() {
        if (canEdit && UserSession.getCurrentUser()?.isAnonymous != true) {
            binding.officialActionsSection.isVisible = true
        } else {
            binding.officialActionsSection.isVisible = false
        }
    }

    private fun setupActionButton() {
        if (canEdit && UserSession.getCurrentUser()?.isAnonymous != true) {
            binding.actionButton.isVisible = true
            binding.actionButton.contentDescription = when (post.status) {
                "Pending" -> getString(R.string.mark_in_progress)
                "In Progress" -> getString(R.string.mark_completed)
                else -> "Reopen Issue"
            }
            binding.actionButton.setOnClickListener {
                val newStatus = when (post.status) {
                    "Pending" -> "In Progress"
                    "In Progress" -> "Completed"
                    else -> "Pending"
                }
                updatePostStatus(newStatus)
            }
        } else {
            binding.actionButton.isVisible = false
        }
    }

    private fun updatePostStatus(newStatus: String) {
        val statusUpdate = mapOf("status" to newStatus)
        RetrofitClient.instance.updatePostStatus(post._id, statusUpdate).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    post = response.body()!!
                    setupPostDetails()
                    setupActionButton()
                    Toast.makeText(this@PostDetailActivity, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to update status: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun upvotePost() {
        RetrofitClient.instance.upvotePost(post._id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    post = response.body()!!
                    binding.upvoteCount.text = post.upvotes.toString()
                    binding.downvoteCount.text = post.downvotes.toString()
                    binding.upvoteButton.isSelected = true
                    binding.downvoteButton.isSelected = false
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to upvote: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downvotePost() {
        RetrofitClient.instance.downvotePost(post._id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    post = response.body()!!
                    binding.upvoteCount.text = post.upvotes.toString()
                    binding.downvoteCount.text = post.downvotes.toString()
                    binding.upvoteButton.isSelected = false
                    binding.downvoteButton.isSelected = true
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to downvote: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun postComment(commentText: String) {
        val user = UserSession.getCurrentUser() ?: return
        val commentData = CommentRequest(
            content = commentText,
            isOfficial = user.isOfficial,
            author = user.username
        )
        RetrofitClient.instance.addComment(post._id, commentData).enqueue(object : Callback<Comment> {
            override fun onResponse(call: Call<Comment>, response: Response<Comment>) {
                if (response.isSuccessful) {
                    binding.commentInput.text?.clear()
                    loadPost(post._id) // Refresh post to get updated comments
                    Toast.makeText(this@PostDetailActivity, "Comment posted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to post comment: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Comment>, t: Throwable) {
                Toast.makeText(this@PostDetailActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val dateLong = timestamp.toLong()
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateLong))
        } catch (e: NumberFormatException) {
            timestamp
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

class ImageCarouselAdapter(private val images: List<String>) : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {
    class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            setBackgroundResource(R.drawable.image_placeholder)
        }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load("${RetrofitClient.BASE_URL}images/${images[position]}")
            .placeholder(R.drawable.image_placeholder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}