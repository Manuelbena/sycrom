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

/**
 * Adapter para mostrar la lista de [SubTaskDomain] en [TaskDetailBottomSheet].
 *
 * Utiliza [ListAdapter] y [SubtaskDiffCallback] para un rendimiento óptimo.
 *
 * @param onSubtaskCheckedChange Callback invocado cuando el usuario marca o desmarca una subtarea.
 */
class SubtaskAdapter(
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

    /**
     * ViewHolder para un solo item de subtarea [R.layout.item_subtask].
     */
    inner class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: MaterialCheckBox = itemView.findViewById(R.id.subtaskCheckbox)

        init {
            // Es más eficiente poner el listener aquí que en onBindViewHolder
            checkBox.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    // El estado 'isDone' ya se actualiza por el propio clic
                    val newIsDone = checkBox.isChecked

                    updateStrikeThrough(newIsDone)
                    onSubtaskCheckedChange(item, newIsDone)
                }
            }
        }

        /**
         * Rellena la vista con los datos de la subtarea.
         */
        fun bind(subtask: SubTaskDomain) {
            checkBox.text = subtask.title

            // Lógica para evitar disparar el listener durante el 'bind'
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = subtask.isDone

            updateStrikeThrough(subtask.isDone)

            // Volvemos a poner el listener (aunque en este caso usamos setOnClickListener)
            // Lo dejamos comentado por si se prefiere este enfoque:
            /*
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                 if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    updateStrikeThrough(isChecked)
                    onSubtaskCheckedChange(item, isChecked)
                }
            }
            */
        }

        /**
         * Aplica o quita el tachado del texto del checkbox.
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
 * [DiffUtil.ItemCallback] para que [ListAdapter] sepa cómo
 * detectar cambios entre la lista vieja y la nueva de subtareas.
 */
class SubtaskDiffCallback : DiffUtil.ItemCallback<SubTaskDomain>() {
    /**
     * Comprueba si dos items *representan* el mismo objeto (compara IDs).
     */
    override fun areItemsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Comprueba si los *contenidos* de dos items son idénticos.
     */
    override fun areContentsTheSame(oldItem: SubTaskDomain, newItem: SubTaskDomain): Boolean {
        return oldItem == newItem
    }
}