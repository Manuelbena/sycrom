package com.manuelbena.synkron.presentation.note

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemCategoryRowSelectorBinding
import com.manuelbena.synkron.databinding.ItemSelectorBinding
import com.manuelbena.synkron.presentation.models.FilterModel

class FilterAdapter(
    private var filters: List<FilterModel>, // CAMBIO: Ahora recibe una lista de objetos, no Strings
    private val onFilterSelected: (FilterModel) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        // Asegúrate de que este Binding corresponda al XML 'item_category_row_selector.xml' nuevo
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
            // 1. ASIGNAR DATOS
            binding.tvName.text = item.name
            binding.tvCount.text = item.count.toString()

            // Icono opcional
            if (item.iconRes != null) {
                binding.ivIcon.isVisible = true
                binding.ivIcon.setImageResource(item.iconRes)
            } else {
                binding.ivIcon.isVisible = false
            }

            // 2. LÓGICA DE COLORES (Estilo Material Design Morado)
            if (item.isSelected) {
                // --- ESTADO: SELECCIONADO (Fondo Morado) ---

                // Card Principal: Morado
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#7E57C2"))

                // Texto e Icono: Blanco
                binding.tvName.setTextColor(Color.WHITE)
                binding.ivIcon.setColorFilter(Color.WHITE)

                // Badge (Contador): Fondo Blanco, Texto Morado
                binding.cardBadge.setCardBackgroundColor(Color.WHITE)
                binding.tvCount.setTextColor(Color.parseColor("#7E57C2"))

            } else {
                // --- ESTADO: NO SELECCIONADO (Fondo Gris) ---

                // Card Principal: Gris muy claro
                binding.cardContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"))

                // Texto e Icono: Gris Oscuro
                binding.tvName.setTextColor(Color.parseColor("#4B5563"))
                binding.ivIcon.setColorFilter(Color.parseColor("#4B5563"))

                // Badge (Contador): Fondo Morado, Texto Blanco
                binding.cardBadge.setCardBackgroundColor(Color.parseColor("#7E57C2"))
                binding.tvCount.setTextColor(Color.WHITE)
            }

            // 3. CLICK LISTENER
            binding.root.setOnClickListener {
                // Desmarcamos todos visualmente
                filters.forEach { it.isSelected = false }
                // Marcamos el actual
                item.isSelected = true

                // Notificamos cambios para repintar los colores
                notifyDataSetChanged()

                // Devolvemos el evento al fragmento
                onFilterSelected(item)
            }
        }
    }

    // Método helper por si necesitas actualizar la lista desde el Fragmento más tarde
    fun updateList(newFilters: List<FilterModel>) {
        this.filters = newFilters
        notifyDataSetChanged()
    }
}