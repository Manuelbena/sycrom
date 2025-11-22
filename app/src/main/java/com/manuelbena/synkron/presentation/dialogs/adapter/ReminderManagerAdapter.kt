package com.manuelbena.synkron.presentation.dialogs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemReminderRowBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod

class ReminderManagerAdapter(
    private val onDeleteClick: (ReminderItem) -> Unit
) : ListAdapter<ReminderItem, ReminderManagerAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemReminderRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReminderItem) {
            binding.tvTime.text = item.getFormattedText()

            if (item.method == ReminderMethod.EMAIL) {
                binding.tvTypeLabel.text = "Correo electrónico"
                binding.ivTypeIcon.setImageResource(R.drawable.baseline_edit_calendar_24) // Asegúrate de tener este icono
            } else {
                binding.tvTypeLabel.text = "Notificación"
                binding.ivTypeIcon.setImageResource(R.drawable.ic_notifications_black_24dp)
            }

            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ReminderItem>() {
        override fun areItemsTheSame(oldItem: ReminderItem, newItem: ReminderItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ReminderItem, newItem: ReminderItem) = oldItem == newItem
    }
}