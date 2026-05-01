package com.manuelbena.synkron.presentation.task.adapter // O tu paquete correspondiente

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemCategoryGridBinding


import com.manuelbena.synkron.presentation.models.CategoryType

class CategoryGridAdapter(
    private val onCategorySelected: (CategoryType) -> Unit
) : ListAdapter<CategoryType, CategoryGridAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryType) {
            binding.apply {
                // 1. Datos básicos
                tvTitle.text = item.title
                ivIcon.setImageResource(item.iconRes)

                // 2. Color dinámico del círculo de fondo
                val color = ContextCompat.getColor(root.context, item.colorRes)
                viewCircleBackground.backgroundTintList = ColorStateList.valueOf(color)

                // 3. Click Listener
                root.setOnClickListener {
                    onCategorySelected(item)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryType>() {
        override fun areItemsTheSame(oldItem: CategoryType, newItem: CategoryType) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CategoryType, newItem: CategoryType) = oldItem == newItem
    }
}