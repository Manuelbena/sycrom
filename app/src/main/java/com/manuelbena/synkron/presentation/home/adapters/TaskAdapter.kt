package com.manuelbena.synkron.presentation.home.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.domain.models.TaskDomain
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
        private val binding: ItemTaskTodayBinding
    ) : BaseViewHolder<TaskDomain>(binding) {

        init {
            // Gestionamos los clics desde el 'init' del ViewHolder.
            // Es más eficiente que crearlos en 'onBindViewHolder'.
            binding.root.setOnClickListener {
                executeIfValidPosition { task ->
                    onItemClick(task)
                }
            }

            binding.btnTaskOptions.setOnClickListener {
                executeIfValidPosition { task ->
                    showPopupMenu(binding.btnTaskOptions, task)
                }
            }

            binding.cbIsDone.setOnCheckedChangeListener { _, isChecked ->
                executeIfValidPosition { task ->
                    // 1. Actualiza la UI localmente (tachado)
                    updateStrikeThrough(isChecked)
                    // 2. Informa al exterior (ViewModel) del cambio
                    onTaskCheckedChange(task, isChecked)
                }
            }
        }

        /**
         * Rellena la vista con los datos del [item] de la tarea.
         */
        override fun bind(item: TaskDomain) {
            binding.apply {
                // --- Información Básica ---
                tvEventTitle.text = item.title
                tvEventType.text = item.typeTask
                tvEventLocation.text = item.place.takeIf { it.isNotEmpty() } ?: "Sin ubicación"

                // --- Estado 'Terminado' (CheckBox y Tachado) ---
                updateStrikeThrough(item.isDone)

                // Lógica CRÍTICA del CheckBox para evitar bucles de 'bind':
                // 1. Quitamos el listener para que 'setChecked' no dispare un evento.
                cbIsDone.setOnCheckedChangeListener(null)
                // 2. Asignamos el estado del check BASADO EN EL MODELO.
                cbIsDone.isChecked = item.isDone
                // 3. Volvemos a poner el listener para futuros clics del usuario.
                cbIsDone.setOnCheckedChangeListener { _, isChecked ->
                    executeIfValidPosition { task ->
                        updateStrikeThrough(isChecked)
                        onTaskCheckedChange(task, isChecked)
                    }
                }

                // --- Formateo de Duración y Hora ---
                tvDuration.text = formatDuration(item.duration)
                tvAttendees.text = formatHour(item.hour) // Nota: El ID 'tvAttendees' parece incorrecto, sugiero 'tvEventTime'

                // --- Lógica de Progreso de Subtareas ---
                val totalSubtasks = item.subTasks.size
                val completedSubtasks = item.subTasks.count { it.isDone }

                if (totalSubtasks > 0) {
                    val progress = (completedSubtasks * 100) / totalSubtasks
                    showProgress(true)

                    pbCircularProgress.progress = progress

                    tvSubtasks.text = "$completedSubtasks/$totalSubtasks"
                } else {
                    showProgress(false)
                    tvSubtasks.visibility = View.GONE

                }
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