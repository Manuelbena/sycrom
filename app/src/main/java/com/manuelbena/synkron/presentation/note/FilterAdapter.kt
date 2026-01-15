package com.manuelbena.synkron.presentation.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemCategoryRowSelectorBinding

class FilterAdapter(
    private val filters: List<String>,
    private val onFilterSelected: (String) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        return FilterViewHolder(
            ItemCategoryRowSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position], position == selectedIndex)
    }

    override fun getItemCount() = filters.size

    inner class FilterViewHolder(private val binding: ItemCategoryRowSelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(filterName: String, isSelected: Boolean) {
            // CORREGIDO: Usamos el ID del TextView que está en el XML
            binding.tvCategoryLabel.text = filterName.uppercase()

            val context = binding.root.context

            if (isSelected) {
                // Background en el CardView (que es binding.containerCategorySelector o root)
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_primary))
                // Color de texto en el TextView
                binding.tvCategoryLabel.setTextColor(ContextCompat.getColor(context, R.color.white))
                // Tinte del icono
                binding.ivCategoryLabelIcon.setColorFilter(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                binding.tvCategoryLabel.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                binding.ivCategoryLabelIcon.setColorFilter(ContextCompat.getColor(context, R.color.secondary_text))
            }

            // Ocultamos elementos que no usamos para este filtro simple
            binding.tvSelectedCategoryName.visibility = android.view.View.GONE
            binding.flSelectedCategoryIcon.visibility = android.view.View.GONE

            binding.root.setOnClickListener {
                val previousIndex = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(previousIndex)
                notifyItemChanged(selectedIndex)
                onFilterSelected(filterName)
            }
        }
    }
}