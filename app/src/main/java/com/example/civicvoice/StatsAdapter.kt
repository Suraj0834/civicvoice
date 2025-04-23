package com.example.civicvoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.civicvoice.databinding.StatItemBinding

class StatsAdapter(
    private val onStatClick: (String?) -> Unit
) : RecyclerView.Adapter<StatsAdapter.StatViewHolder>() {

    private var statsList: List<Stat> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = StatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(statsList[position])
    }

    override fun getItemCount(): Int = statsList.size

    fun submitList(stats: List<Stat>) {
        statsList = stats
        notifyDataSetChanged()
    }

    inner class StatViewHolder(private val binding: StatItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: Stat) {
            with(binding) {
                statTitle.text = stat.title
                statValue.text = stat.value
                statIcon.setImageResource(stat.iconRes)
                statIcon.imageTintList = android.content.res.ColorStateList.valueOf(stat.iconTint)
                root.setOnClickListener { onStatClick(stat.filterStatus) }
            }
        }
    }
}