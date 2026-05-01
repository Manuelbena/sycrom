package com.manuelbena.synkron.presentation.util.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemSubtaskBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain

class SubTaskAdapter(
    private val onSubTaskChecked: (SubTaskDomain, Boolean) -> Unit
) : ListAdapter<SubTaskDomain, SubTaskAdapter.SubTaskViewHolder>(SubTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTaskViewHolder {
        val binding = ItemSubtaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubTaskViewHolder(private val binding: ItemSubtaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubTaskDomain) {
            binding.tvSubtaskTitle.text = item.title

            // Limpiamos listener para evitar bucles infinitos al setear el estado
            binding.cbSubtask.setOnCheckedChangeListener(null)
            binding.cbSubtask.isChecked = item.isDone

            // --- LÓGICA VISUAL DE TACHADO ---
            updateStrikethrough(item.isDone)

            binding.cbSubtask.setOnCheckedChangeListener { _, isChecked ->
                // 1. Actualización visual inmediata
                updateStrikethrough(isChecked)
                // 2. Notificar al padre (TaskAdapter)
                onSubTaskChecked(item, isChecked)
            }
        }

        private fun updateStrikethrough(isDone: Boolean) {
            if (isDone) {
                binding.tvSubtaskTitle.paintFlags = binding.tvSubtaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvSubtaskTitle.alpha = 0.5f // Texto más claro
            } else {
                binding.tvSubtaskTitle.paintFlags = binding.tvSubtaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvSubtaskTitle.alpha = 1.0f // Texto normal
            }
        }
    }

    class SubTaskDiffCallback : DiffUtil.ItemCallback<SubTaskDomain>() {
        override fun areItemsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain) = oldItem == newItem
    }
}