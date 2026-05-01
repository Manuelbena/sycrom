package com.manuelbena.synkron.presentation.dialogs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemReminderRowBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod

class ReminderManagerAdapter(
    private val onDeleteClick: (ReminderItem) -> Unit
) : ListAdapter<ReminderItem, ReminderManagerAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ReminderViewHolder(private val binding: ItemReminderRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReminderItem) {
            // 1. Configurar la Hora y el Mensaje
            binding.tvTime.text = item.displayTime
            binding.tvTypeLabel.text = item.message // Asegúrate de tener este ID en el XML (ej. tvTitle o tvMessage)

            // 2. Configurar el Icono según el Método (Nuevo Enum)
            val iconRes = when (item.method) {
                ReminderMethod.ALARM -> R.drawable.ic_access_time
                ReminderMethod.WHATSAPP -> R.drawable.ic_share // O tu icono de whatsapp si lo tienes
                ReminderMethod.NOTIFICATION -> R.drawable.ic_notifications
            }
            binding.ivTypeIcon.setImageResource(iconRes)

            // 3. Configurar el Subtítulo (Antelación en lugar de días)
            // Si el offset es 0, es "A la hora", si no, mostramos los minutos antes.
            val subtitleText = if (item.offsetMinutes > 0) {
                "${item.offsetMinutes} min antes"
            } else {
                "En el momento"
            }
            // Asumo que tienes un TextView secundario, si no, usa el de mensaje o crea uno.
            // Si tu XML tiene un tvDays o similar, úsalo para esto:
            // binding.tvSubtitle.text = subtitleText

            // 4. Click en eliminar
            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    class ReminderDiffCallback : DiffUtil.ItemCallback<ReminderItem>() {
        override fun areItemsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
            return oldItem == newItem
        }
    }
}