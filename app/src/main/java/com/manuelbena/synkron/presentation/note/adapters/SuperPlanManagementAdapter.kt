package com.manuelbena.synkron.presentation.note.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSuperPlanListBinding
import com.manuelbena.synkron.domain.models.SuperTaskModel

class SuperPlanManagementAdapter(
    private val onItemClick: (SuperTaskModel) -> Unit
) : ListAdapter<SuperTaskModel, SuperPlanManagementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSuperPlanListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSuperPlanListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SuperTaskModel) {
            binding.tvTitle.text = item.title
            // Calculamos elementos
            binding.tvSubtitle.text = "${item.subTasks.size} elementos"

            // Icono dinámico según el modelo
            binding.ivIcon.setImageResource(item.getIconRes()) // Asegúrate de tener este helper en tu modelo o haz un when aquí

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SuperTaskModel>() {
        override fun areItemsTheSame(oldItem: SuperTaskModel, newItem: SuperTaskModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SuperTaskModel, newItem: SuperTaskModel) = oldItem == newItem
    }
}