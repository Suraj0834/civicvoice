package com.example.civicvoice

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.civicvoice.databinding.ItemNotificationBinding
import com.example.civicvoice.network.Notification
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val onClick: (Notification) -> Unit,
    private val onSwipeToDelete: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(notification: Notification) {
            with(binding) {
                // Set text fields
                tvTitle.text = notification.title
                tvMessage.text = notification.message
                tvPostTitle.text = notification.postId?.let { "Post ID: $it" } ?: "General Notification"
                tvTimestamp.text = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                    .format(java.util.Date(notification.timestamp.toLong()))

                // Set notification type and icon
                val type = when {
                    notification.title.contains("Comment", true) -> "Comment"
                    notification.title.contains("Update", true) -> "Status Update"
                    else -> "Info"
                }
                chipType.text = type
                chipType.setChipIconResource(
                    when (type) {
                        "Comment" -> R.drawable.ic_comment
                        "Status Update" -> R.drawable.ic_status_update
                        else -> R.drawable.ic_info
                    }
                )

                // Style unread notifications
                val backgroundColor = if (notification.isRead) {
                    R.color.notification_read
                } else {
                    unreadIndicator.visibility = View.VISIBLE
                    R.color.notification_unread
                }
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, backgroundColor)
                )

                // Click animation
                root.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            ViewCompat.animate(root).scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            ViewCompat.animate(root).scaleX(1f).scaleY(1f).setDuration(100).start()
                            if (event.action == MotionEvent.ACTION_UP) {
                                onClick(notification)
                            }
                        }
                    }
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Attach swipe-to-dismiss to RecyclerView
    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = getItem(position)
                onSwipeToDelete(notification)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean =
            oldItem._id == newItem._id

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean =
            oldItem == newItem
    }
}