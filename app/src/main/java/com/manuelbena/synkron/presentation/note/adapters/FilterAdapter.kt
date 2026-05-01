package com.manuelbena.synkron.presentation.note.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSelectorBinding
import com.manuelbena.synkron.presentation.models.FilterModel

class FilterAdapter(
    private var filters: List<FilterModel>,
    private val onFilterSelected: (FilterModel) -> Unit // FIRMA CORRECTA
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position])
    }

    override fun getItemCount() = filters.size

    inner class FilterViewHolder(private val binding: ItemSelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterModel) {
            binding.tvName.text = item.name
            binding.tvCount.text = item.count.toString()

            if (item.iconRes != null) {
                binding.ivIcon.isVisible = true
                binding.ivIcon.setImageResource(item.iconRes)
            } else {
                binding.ivIcon.isVisible = false
            }

            if (item.isSelected) {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#7E57C2"))
                binding.tvName.setTextColor(Color.WHITE)
                binding.ivIcon.setColorFilter(Color.WHITE)
                binding.cardBadge.setCardBackgroundColor(Color.WHITE)
                binding.tvCount.setTextColor(Color.parseColor("#7E57C2"))
            } else {
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                binding.tvName.setTextColor(Color.parseColor("#4B5563"))
                binding.ivIcon.setColorFilter(Color.parseColor("#4B5563"))
                binding.cardBadge.setCardBackgroundColor(Color.parseColor("#7E57C2"))
                binding.tvCount.setTextColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                if (!item.isSelected) {
                    filters.forEach { it.isSelected = false }
                    item.isSelected = true
                    notifyDataSetChanged()

                    // CORRECCIÓN DE ÉLITE: Enviamos el objeto completo, no solo el ID
                    onFilterSelected(item)
                }
            }
        }
    }

    fun updateList(newFilters: List<FilterModel>) {
        this.filters = newFilters
        notifyDataSetChanged()
    }
}