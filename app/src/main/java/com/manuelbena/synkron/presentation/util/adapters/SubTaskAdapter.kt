package com.manuelbena.synkron.presentation.util.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.SubTaskDomain


class SubtaskAdapter(
    // Lambda para notificar al ViewModel cuando un check cambia
    private val onSubtaskCheckedChange: (subtask: SubTaskDomain, isDone: Boolean) -> Unit
) : ListAdapter<SubTaskDomain, SubtaskAdapter.SubtaskViewHolder>(SubtaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.subtaskCheckbox)

        fun bind(subtask: SubTaskDomain) {
            checkBox.text = subtask.title

            // --- LÓGICA DE TACHADO ---
            // Aplicar/Quitar el tachado según el estado inicial
            updateStrikeThrough(subtask.isDone)
            // --- FIN DE LÓGICA DE TACHADO ---

            // 1. Establecemos el estado inicial del check sin disparar el listener
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = subtask.isDone

            // 2. Añadimos el listener para futuras interacciones del usuario
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Aplicar/Quitar el tachado INMEDIATAMENTE al hacer clic
                updateStrikeThrough(isChecked)

                // Solo notificamos si la posición es válida
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSubtaskCheckedChange(getItem(adapterPosition), isChecked)
                }
            }
        }

        /**
         * Helper para aplicar o quitar el tachado del texto del checkbox.
         */
        private fun updateStrikeThrough(isDone: Boolean) {
            if (isDone) {
                checkBox.paintFlags = checkBox.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                checkBox.paintFlags = checkBox.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
}

/**
 * DiffCallback para que el ListAdapter actualice la lista de forma eficiente.
 */
class SubtaskDiffCallback : DiffUtil.ItemCallback<SubTaskDomain>() {
    override fun areItemsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
        // Asumimos que el título es único DENTRO de una tarea
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
        return oldItem == newItem
    }
}

