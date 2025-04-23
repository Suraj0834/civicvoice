package com.example.civicvoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.civicvoice.databinding.ActivityCommentsBinding
import com.example.civicvoice.databinding.ItemOfficialCommentBinding
import com.example.civicvoice.databinding.ItemRegularCommentBinding
import com.example.civicvoice.network.CommentRequest
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.CommentMapper
import com.example.civicvoice.utils.UserSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding
    private lateinit var officialCommentsAdapter: OfficialCommentsAdapter
    private lateinit var regularCommentsAdapter: RegularCommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        loadComments()

        binding.fabAddComment.setOnClickListener {
            val user = UserSession.getCurrentUser()
            if (user?.isAnonymous == true) {
                Toast.makeText(this, "Please sign up to add a comment", Toast.LENGTH_SHORT).show()
            } else {
                showAddCommentDialog()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Comments"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerViews() {
        officialCommentsAdapter = OfficialCommentsAdapter(emptyList())
        binding.rvOfficialComments.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = officialCommentsAdapter
            setHasFixedSize(true)
        }

        regularCommentsAdapter = RegularCommentsAdapter()
        binding.rvRegularComments.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = regularCommentsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadComments() {
        val postId = intent.getStringExtra("post_id") ?: return
        RetrofitClient.instance.getPostById(postId).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val post = response.body() ?: return
                    officialCommentsAdapter.updateComments(CommentMapper.toDataComments(post.officialComments))
                    regularCommentsAdapter.submitList(CommentMapper.toDataComments(post.comments))

                    binding.officialCommentsTitle.isVisible = post.officialComments.isNotEmpty()
                    binding.rvOfficialComments.isVisible = post.officialComments.isNotEmpty()
                    binding.regularCommentsTitle.isVisible = post.comments.isNotEmpty()
                    binding.rvRegularComments.isVisible = post.comments.isNotEmpty()
                } else {
                    Toast.makeText(this@CommentsActivity, "Failed to load comments: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                Toast.makeText(this@CommentsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddCommentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val commentInput = dialogView.findViewById<EditText>(R.id.etComment)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Comment")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val commentText = commentInput.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    addRegularComment(commentText)
                } else {
                    Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addRegularComment(commentText: String) {
        val postId = intent.getStringExtra("post_id") ?: return
        val currentUser = UserSession.getCurrentUser() ?: return

        val commentRequest = CommentRequest(
            content = commentText,
            isOfficial = currentUser.isOfficial,
            author = currentUser.username
        )

        RetrofitClient.instance.addComment(postId, commentRequest).enqueue(object : Callback<com.example.civicvoice.network.Comment> {
            override fun onResponse(call: Call<com.example.civicvoice.network.Comment>, response: Response<com.example.civicvoice.network.Comment>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CommentsActivity, "Comment added", Toast.LENGTH_SHORT).show()
                    loadComments()
                } else {
                    Toast.makeText(this@CommentsActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.civicvoice.network.Comment>, t: Throwable) {
                Toast.makeText(this@CommentsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

class RegularCommentsAdapter : RecyclerView.Adapter<RegularCommentsAdapter.RegularViewHolder>() {
    private var comments: List<com.example.civicvoice.data.Comment> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegularViewHolder {
        val binding = ItemRegularCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegularViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegularViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    fun submitList(newComments: List<com.example.civicvoice.data.Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    class RegularViewHolder(private val binding: ItemRegularCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: com.example.civicvoice.data.Comment) {
            binding.commentUser.text = comment.username
            binding.commentContent.text = comment.content
            binding.officialBadge.isVisible = comment.isOfficial
            binding.commentDate.text = try {
                val dateLong = comment.date.toLong()
                SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()).format(Date(dateLong))
            } catch (e: NumberFormatException) {
                comment.date
            }
        }
    }
}

class OfficialCommentsAdapter(private var comments: List<com.example.civicvoice.data.Comment>) : RecyclerView.Adapter<OfficialCommentsAdapter.OfficialViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficialViewHolder {
        val binding = ItemOfficialCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfficialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfficialViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<com.example.civicvoice.data.Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    class OfficialViewHolder(private val binding: ItemOfficialCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: com.example.civicvoice.data.Comment) {
            binding.tvAuthor.text = comment.username
            binding.tvComment.text = comment.content
            binding.ivOfficialBadge.isVisible = comment.isOfficial
            binding.tvDate.text = try {
                val dateLong = comment.date.toLong()
                SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()).format(Date(dateLong))
            } catch (e: NumberFormatException) {
                comment.date
            }
        }
    }
}