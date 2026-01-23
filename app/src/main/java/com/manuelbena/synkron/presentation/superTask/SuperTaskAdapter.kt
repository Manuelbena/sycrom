package com.manuelbena.synkron.presentation.adapter // Asegúrate del paquete correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemSuperTaskCardBinding
import com.manuelbena.synkron.databinding.ItemSuperTaskRowBinding
import com.manuelbena.synkron.domain.models.SuperTaskModel

class SuperTaskAdapter(
    private val onItemClick: (SuperTaskModel) -> Unit
) : ListAdapter<SuperTaskModel, SuperTaskAdapter.SuperTaskViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuperTaskViewHolder {
        val binding = ItemSuperTaskCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuperTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuperTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SuperTaskViewHolder(private val binding: ItemSuperTaskCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SuperTaskModel) {
            binding.tvTitle.text = item.title
            binding.tvProgressText.text = "${item.completedCount}/${item.totalCount} completados"
            binding.ivMainIcon.setImageResource(item.iconRes)

            // Progreso
            val progress = if (item.totalCount > 0) (item.completedCount * 100) / item.totalCount else 0
            binding.progressBar.progress = progress

            // Botón footer
            binding.btnViewAll.text = "Ver todos (${item.totalCount}) >"

            // --- POBLAR LA LISTA INTERNA (Vista Previa) ---
            binding.lySubTasksContainer.removeAllViews()
            val inflater = LayoutInflater.from(binding.root.context)

            // Mostramos máximo 3 items
            item.subTasks.take(3).forEach { subTask ->
                val rowBinding = ItemSuperTaskRowBinding.inflate(inflater, binding.lySubTasksContainer, true)

                rowBinding.tvSubTaskTitle.text = subTask.title
                rowBinding.tvSubTaskDetails.text = subTask.details

                if (subTask.isCompleted) {
                    rowBinding.ivStatus.setImageResource(R.drawable.ic_check_circle)
                    rowBinding.ivStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.md_theme_primary))
                    rowBinding.tvSubTaskTitle.alpha = 0.5f
                } else {
                    rowBinding.ivStatus.setImageResource(R.drawable.ic_check_circle) // Asegúrate de tener este drawable
                    rowBinding.ivStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.secondary_text))
                    rowBinding.tvSubTaskTitle.alpha = 1.0f
                }
            }

            // Click listeners para abrir el detalle
            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnViewAll.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SuperTaskModel>() {
        override fun areItemsTheSame(oldItem: SuperTaskModel, newItem: SuperTaskModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SuperTaskModel, newItem: SuperTaskModel) = oldItem == newItem
    }
}