package com.example.civicvoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.civicvoice.databinding.ItemComplaintBinding
import com.example.civicvoice.network.Complaint
import com.example.civicvoice.utils.UserSession

class ComplaintsAdapter(
    private var complaints: List<Complaint>,
    private val onDeleteClick: (Complaint) -> Unit,
    private val onFeedbackClick: (Complaint) -> Unit
) : RecyclerView.Adapter<ComplaintsAdapter.ComplaintViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val binding = ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ComplaintViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        holder.bind(complaints[position])
    }

    override fun getItemCount(): Int = complaints.size

    fun updateComplaints(newComplaints: List<Complaint>) {
        complaints = newComplaints
        notifyDataSetChanged()
    }

    inner class ComplaintViewHolder(private val binding: ItemComplaintBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(complaint: Complaint) {
            binding.apply {
                complaintTitle.text = complaint.title
                complaintDescription.text = complaint.description
                complaintLocation.text = complaint.location
                complaintStatus.text = complaint.status
                complaintDepartment.text = complaint.department

                deleteButton.visibility = if (complaint.userId == UserSession.getCurrentUser()?.userId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                deleteButton.setOnClickListener { onDeleteClick(complaint) }

                feedbackButton.visibility = if (complaint.status == "Completed" && complaint.userId == UserSession.getCurrentUser()?.userId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                feedbackButton.setOnClickListener { onFeedbackClick(complaint) }
            }
        }
    }
}