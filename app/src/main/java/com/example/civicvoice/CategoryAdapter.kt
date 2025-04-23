package com.example.civicvoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.civicvoice.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val categories: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedCategory: String = "All"

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String) {
            with(binding) {
                tvCategoryName.text = category

                val isSelected = category == selectedCategory
                val context = root.context

                val cardBgColor = ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.colorPrimaryLight else R.color.cardBackground
                )
                val textColor = ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.colorPrimaryDark else R.color.textColorPrimary
                )
                val strokeColor = ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.colorPrimaryDark else R.color.gray_400
                )

                root.setCardBackgroundColor(cardBgColor)
                tvCategoryName.setTextColor(textColor)
                root.strokeColor = strokeColor

                val iconRes = when (category.lowercase()) {
                    "roads" -> R.drawable.road
                    "sanitation" -> R.drawable.home
                    "water" -> R.drawable.water
                    "electricity" -> R.drawable.plug
                    "public works" -> R.drawable.helmet
                    else -> R.drawable.ic_category_default
                }
                ivCategoryIcon.setImageResource(iconRes)

                root.setOnClickListener {
                    selectedCategory = category
                    notifyDataSetChanged()
                    onItemClick(category)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}