package com.manuelbena.synkron.presentation.assistant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.data.local.entities.AIAssistantInsightEntity
import com.manuelbena.synkron.databinding.ItemAssistantInsightBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantInsightAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<AIAssistantInsightEntity, AssistantInsightAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemAssistantInsightBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AIAssistantInsightEntity, onDeleteClick: (Int) -> Unit) {
            binding.tvType.text = if (item.type == "AUDITORIA") "Auditoría Semanal" else "Gestión de Foco"
            binding.tvMessage.text = item.message
            
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(item.date))

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAssistantInsightBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<AIAssistantInsightEntity>() {
        override fun areItemsTheSame(oldItem: AIAssistantInsightEntity, newItem: AIAssistantInsightEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AIAssistantInsightEntity, newItem: AIAssistantInsightEntity) = oldItem == newItem
    }
}
