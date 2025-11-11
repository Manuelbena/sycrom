package com.manuelbena.synkron.presentation.home.adapters

import TaskDomain
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding

import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toHourString
import java.util.Locale

/**
 * Adaptador para el RecyclerView de tareas en [HomeFragment].
 *
 * Utiliza [ListAdapter] para gestionar eficientemente las actualizaciones de la lista
 * gracias a [TaskDiffCallback].
 *
 * @param onItemClick Callback invocado cuando se pulsa sobre el item (tarjeta).
 * @param onMenuAction Callback invocado cuando se selecciona una acción del menú (Editar, Borrar, etc.).
 * @param onTaskCheckedChange Callback invocado cuando el estado del CheckBox "Terminado" cambia.
 */
class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onMenuAction: (TaskMenuAction) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit
) : ListAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    init {
        // Habilitamos Stable IDs para optimizar el rendimiento del RecyclerView.
        // El RecyclerView puede identificar items únicos y evitar re-binds innecesarios.
        setHasStableIds(true)
    }

    /**
     * Usamos el ID de la tarea (proveniente de la BBDD) como ID estable.
     */
    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskTodayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que representa la vista de una única tarea (`item_task_today.xml`).
     */
    inner class TaskViewHolder(
        private val binding: ItemTaskTodayBinding // Asumiendo que tu XML se llama item_task_today.xml
    ) : BaseViewHolder<TaskDomain>(binding) {

        init {
            // --- Listeners (Añadimos el del CheckBox de abajo) ---
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }

            binding.btnTaskOptions.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    showPopupMenu(binding.btnTaskOptions, getItem(bindingAdapterPosition))
                }
            }

            binding.cbIsDone.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    val task = getItem(bindingAdapterPosition)
                    val isChecked = binding.cbIsDone.isChecked

                    // Llamamos a la acción del ViewModel
                    onTaskCheckedChange(task, isChecked)
                }
            }
        }

        /**
         * Rellena la vista con los datos del [item] de la tarea.
         */
        /**
         * Rellena la vista con los datos del nuevo TaskDomain.
         */
        override fun bind(item: TaskDomain) {
            binding.apply {

                // --- 1. Mapeo de Título y Categoría ---
                tvEventTitle.text = item.summary
                chipCategory.text = item.typeTask

                // --- 2. Mapeo de Contexto (Línea por Línea) ---

                // Ubicación
                if (item.location.isNullOrEmpty()) {
                    tvEventLocation.visibility = View.GONE
                    iconLocation.visibility = View.GONE
                } else {
                    tvEventLocation.visibility = View.VISIBLE
                    iconLocation.visibility = View.VISIBLE
                    tvEventLocation.text = item.location
                }

                // Hora (usando el helper .toHourString())
                if (item.start != null) {
                    tvEventTime.text = item.start.toHourString()
                } else {
                    tvEventTime.text = "--:--"
                }

                // Duración (calculada con helpers)
                val durationInMinutes = getDurationInMinutes(item.start, item.end)
                if (durationInMinutes > 0) {
                    tvDuration.visibility = View.VISIBLE
                    iconDuration.visibility = View.VISIBLE
                    tvDuration.text = durationInMinutes.toDurationString()
                } else {
                    tvDuration.visibility = View.GONE
                    iconDuration.visibility = View.GONE
                }

                // --- 3. Mapeo de Progreso (Círculo) ---
                val hasSubtasks = item.subTasks.isNotEmpty()
                if (hasSubtasks) {
                    progressCircular.visibility = View.VISIBLE
                    val total = item.subTasks.size
                    val completed = item.subTasks.count { it.isDone }
                    val progress = if (total > 0) (completed * 100) / total else 0

                    pbCircularProgress.progress = progress
                    tvProgressPercentage.text = "$progress%"
                } else {
                    progressCircular.visibility = View.INVISIBLE
                }

                // --- 4. Mapeo de Estado (CheckBox y Estilo) ---

                // Sincronizar el CheckBox de abajo
                cbIsDone.setOnCheckedChangeListener(null) // Quitar listener
                cbIsDone.isChecked = item.isDone
                cbIsDone.setOnCheckedChangeListener { _, isChecked -> // Poner listener
                    onTaskCheckedChange(item, isChecked)
                }

                // Aplicar Tachado al Título
                if (item.isDone) {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // Atenuar la tarjeta si está completada
                cardTaskRoot.alpha = if (item.isDone) 0.6f else 1.0f

                // --- 5. Lógica "Viva" (Prioridad) ---
                val priorityColorRes = when (item.priority) {
                    "Alta" -> R.color.priority_high_bg
                    "Media" -> R.color.priority_medium_bg
                    else -> R.color.priority_low_bg
                }
                cardTaskRoot.strokeColor = ContextCompat.getColor(binding.root.context, priorityColorRes)
            }
        }
        /**
         * Muestra u oculta los elementos visuales del progreso.
         */
        private fun showProgress(show: Boolean) {
            binding.pbCircularProgress.isVisible = show // ID 'progress_circular' en XML

        }

        /**
         * Tacha o quita el tachado del título de la tarea.
         */
        private fun updateStrikeThrough(isDone: Boolean) {
            if (isDone) {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        /**
         * Muestra el menú contextual (Editar, Borrar, Compartir).
         */
        private fun showPopupMenu(anchorView: View, task: TaskDomain) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.menuInflater.inflate(R.menu.menu_item_task, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                val action = when (menuItem.itemId) {
                    R.id.action_edit_task -> TaskMenuAction.OnEdit(task)
                    R.id.action_delete_task -> TaskMenuAction.OnDelete(task)
                    R.id.action_share_task -> TaskMenuAction.OnShare(task)
                    else -> null
                }
                action?.let { onMenuAction(it) }
                action != null
            }
            popup.show()
        }

        /**
         * Formatea la duración en minutos a un string "X h Y min" o "Y min".
         */
        private fun formatDuration(durationInMinutes: Int): String {
            return when {
                durationInMinutes <= 0 -> "Sin Duración"
                durationInMinutes >= 60 -> {
                    val hours = durationInMinutes / 60
                    val minutes = durationInMinutes % 60
                    if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
                }
                else -> "${durationInMinutes} min"
            }
        }

        /**
         * Formatea la hora (total de minutos desde medianoche) a un string "HH:MM".
         */
        private fun formatHour(totalMinutes: Int): String {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        }

        /**
         * Helper para ejecutar una acción solo si la posición del adapter es válida,
         * evitando crashes por clics durante una animación de borrado.
         */
        private inline fun executeIfValidPosition(action: (TaskDomain) -> Unit) {
            if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                action(getItem(bindingAdapterPosition))
            }
        }
    }

    /**
     * Define las acciones que el usuario puede tomar desde el menú de una tarea.
     * Usamos una Sealed Class para un manejo de tipos seguro.
     */
    sealed class TaskMenuAction {
        data class OnEdit(val task: TaskDomain) : TaskMenuAction()
        data class OnDelete(val task: TaskDomain) : TaskMenuAction()
        data class OnShare(val task: TaskDomain) : TaskMenuAction()
    }

    /**
     * [DiffUtil.ItemCallback] para que [ListAdapter] sepa cómo
     * detectar cambios entre la lista vieja y la nueva.
     */
    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        /**
         * Comprueba si dos items *representan* el mismo objeto (compara IDs).
         */
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Comprueba si los *contenidos* de dos items son idénticos (compara el objeto entero).
         * Se llama solo si [areItemsTheSame] devuelve true.
         */
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}