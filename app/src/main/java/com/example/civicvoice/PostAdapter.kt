package com.example.civicvoice

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.civicvoice.databinding.ItemPostBinding
import com.example.civicvoice.network.Post
import com.example.civicvoice.network.RetrofitClient
import com.example.civicvoice.utils.CommentMapper
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val userType: String,
    private val department: String?,
    private val onPostClick: (Post) -> Unit,
    private val onUpvoteClick: (Post) -> Unit,
    private val onDownvoteClick: (Post) -> Unit,
    private val onAddOfficialComment: (Post) -> Unit,
    private val onMarkInProgress: (Post) -> Unit,
    private val onMarkCompleted: (Post) -> Unit,
    private val onAddRegularComment: (Post, String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts: List<Post> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    override fun getItemId(position: Int): Long = posts[position]._id.hashCode().toLong()

    fun submitList(newPosts: List<Post>?) {
        posts = newPosts ?: emptyList()
        notifyDataSetChanged()
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        private val officialCommentsAdapter = OfficialCommentsAdapter(emptyList())

        fun bind(post: Post) {
            binding.apply {
                usernameTextView.text = if (post.isAnonymous) "Anonymous" else post.username
                timestampTextView.text = formatTimestamp(post.date)
                descriptionTextView.text = post.title
                locationTextView.text = "Location: ${post.location}"
                departmentTextView.text = "Department: ${post.department}"
                statusTextView.text = "Status: ${post.status}"
                upvoteCountTextView.text = post.upvotes.toString()
                downvoteCountTextView.text = post.downvotes.toString()

                postImageView.isVisible = post.imageId?.isNotEmpty() == true
                if (post.imageId != null) {
                    Glide.with(itemView.context)
                        .load("${RetrofitClient.BASE_URL}images/${post.imageId}")
                        .placeholder(R.drawable.ic_placeholder)
                        .into(postImageView)
                }

                val isAnonymousUser = userType == "anonymous"
                upvoteButton.isEnabled = !isAnonymousUser
                downvoteButton.isEnabled = !isAnonymousUser
                commentEditText.isEnabled = !isAnonymousUser
                postCommentButton.isEnabled = !isAnonymousUser

                if (isAnonymousUser) {
                    upvoteButton.alpha = 0.5f
                    downvoteButton.alpha = 0.5f
                    commentEditText.hint = itemView.context.getString(R.string.sign_up_to_comment)
                    postCommentButton.alpha = 0.5f
                } else {
                    upvoteButton.alpha = 1.0f
                    downvoteButton.alpha = 1.0f
                    commentEditText.hint = itemView.context.getString(R.string.add_comment)
                    postCommentButton.alpha = 1.0f
                }

                upvoteButton.setOnClickListener { if (!isAnonymousUser) onUpvoteClick(post) }
                downvoteButton.setOnClickListener { if (!isAnonymousUser) onDownvoteClick(post) }
                root.setOnClickListener { onPostClick(post) }

                officialActionsLayout.isVisible = userType == "official" && department == post.department
                markInProgressButton.setOnClickListener { onMarkInProgress(post) }
                markCompletedButton.setOnClickListener { onMarkCompleted(post) }

                rvOfficialComments.layoutManager = LinearLayoutManager(itemView.context)
                rvOfficialComments.adapter = officialCommentsAdapter
                officialCommentsAdapter.updateComments(CommentMapper.toDataComments(post.officialComments))
                officialCommentsSection.isVisible = post.officialComments.isNotEmpty()
                btnAddOfficialComment.isVisible = userType == "official"
                btnAddOfficialComment.setOnClickListener { onAddOfficialComment(post) }

                val totalComments = post.comments.size + post.officialComments.size
                if (totalComments > 0) {
                    commentUser.text = post.comments.getOrNull(0)?.username
                    commentContent.text = post.comments.getOrNull(0)?.content
                    // Manually construct the "View all X comments" string
                    viewAllComments.text = if (totalComments == 1) {
                        itemView.context.getString(R.string.view_all_comments)
                    } else {
                        "${itemView.context.getString(R.string.view_all_comments)} ($totalComments)"
                    }
                    commentUser.isVisible = post.comments.isNotEmpty()
                    commentContent.isVisible = post.comments.isNotEmpty()
                } else {
                    commentUser.isVisible = false
                    commentContent.isVisible = false
                    viewAllComments.text = itemView.context.getString(R.string.no_comments)
                }

                commentsSection.setOnClickListener {
                    val intent = Intent(itemView.context, CommentsActivity::class.java).apply {
                        putExtra("post_id", post._id)
                    }
                    itemView.context.startActivity(intent)
                }

                postCommentButton.setOnClickListener {
                    if (!isAnonymousUser) {
                        val commentText = commentEditText.text.toString().trim()
                        if (commentText.isNotEmpty()) {
                            onAddRegularComment(post, commentText)
                            commentEditText.text?.clear()
                        }
                    }
                }

                shareButton.setOnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, post.title)
                        putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.description}\nLocation: ${post.location}")
                    }
                    itemView.context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                }
            }
        }

        private fun formatTimestamp(timestamp: String): String {
            return try {
                val dateLong = timestamp.toLong()
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateLong))
            } catch (e: NumberFormatException) {
                timestamp
            }
        }
    }
}