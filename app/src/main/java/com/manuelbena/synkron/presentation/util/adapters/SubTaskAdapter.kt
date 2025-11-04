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

        // --- INICIO DE MODIFICACIÓN ---
        init {
            // 1. Configuramos el listener en el init, igual que en TaskAdapter
            checkBox.setOnClickListener {
                // Solo notificamos si la posición es válida
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    val newIsDone = !item.isDone // Calculamos el NUEVO estado

                    // Aplicar/Quitar el tachado INMEDIATAMENTE al hacer clic
                    updateStrikeThrough(newIsDone)

                    // Notificamos al ViewModel
                    onSubtaskCheckedChange(item, newIsDone)
                }
            }
        }
        // --- FIN DE MODIFICACIÓN ---

        fun bind(subtask: SubTaskDomain) {
            checkBox.text = subtask.title

            // Aplicar/Quitar el tachado según el estado inicial
            updateStrikeThrough(subtask.isDone)

            // Establecemos el estado inicial del check SIN disparar el listener
            // (setOnClickListener no se dispara al cambiar isChecked programáticamente,
            // pero setOnCheckedChangeListener sí lo haría, por eso es bueno ponerlo en null)
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = subtask.isDone
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
        // !! IMPORTANTE: ¡Cambia esto para usar el ID!
        // return oldItem.title == newItem.title // <-- Cambia esto
        return oldItem.id == newItem.id // <-- Por esto
    }

    override fun areContentsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
        return oldItem == newItem
    }
}