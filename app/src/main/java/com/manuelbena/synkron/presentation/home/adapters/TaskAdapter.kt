package com.manuelbena.synkron.presentation.home.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseAdapter
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import java.util.Locale


/**
 * Adaptador para la lista de tareas (Tasks) en el RecyclerView del Home.
 * Extiende de [BaseAdapter] usando [TaskDomain] como modelo y [TaskViewHolder] como ViewHolder.
 *
 * @param onItemClick Lambda que se ejecuta cuando se pulsa en un item de la lista.
 * @param onMenuAction Lambda para acciones del menú (editar, borrar, compartir).
 * @param onTaskCheckedChange Lambda que se ejecuta cuando el LottieCheckbox es pulsado.
 */
class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onMenuAction: (TaskMenuAction) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit
) : BaseAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    /**
     * Crea un nuevo [TaskViewHolder] inflando el layout del item.
     * Este método es llamado por el RecyclerView cuando necesita crear un nuevo ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskTodayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    /**
     * ViewHolder específico para un item de [TaskDomain].
     * Se encarga de "bindear" (conectar) los datos del modelo [TaskDomain]
     * con las vistas definidas en `item_task_today.xml`.
     */
    inner class TaskViewHolder(
        private val binding: ItemTaskTodayBinding
    ) : BaseViewHolder<TaskDomain>(binding) {

        init {
            // Configura el click listener para todo el item
            binding.root.setOnClickListener {
                // Comprueba que la posición es válida antes de invocar el callback
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }

            // 3. AÑADE EL LISTENER PARA EL BOTÓN DE MENÚ
            binding.btnTaskOptions.setOnClickListener {
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    showPopupMenu(binding.btnTaskOptions, getItem(bindingAdapterPosition))
                }
            }

            // --- MODIFICADO ---
            binding.cbIsDone.setOnClickListener {
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    val newIsDone = !item.isDone // Calculamos el NUEVO estado

                    // Actualizamos el tachado del título INMEDIATAMENTE
                    updateStrikeThrough(newIsDone)


                    // Notificamos al ViewModel el cambio de estado
                    onTaskCheckedChange(item, newIsDone)
                }
            }
            // --- FIN MODIFICACIÓN ---
        }

        // 4. AÑADE LA FUNCIÓN PARA MOSTRAR EL MENÚ
        private fun showPopupMenu(anchorView: View, task: TaskDomain) {
            // Crea el PopupMenu
            val popup = PopupMenu(anchorView.context, anchorView)

            // Infla tu menú XML
            popup.menuInflater.inflate(R.menu.menu_item_task, popup.menu)

            // Configura el listener para los clics en los items del menú
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_task -> {
                        onMenuAction(TaskMenuAction.OnEdit(task)) // Envía la acción
                        true
                    }

                    R.id.action_delete_task -> {
                        onMenuAction(TaskMenuAction.OnDelete(task)) // Envía la acción
                        true
                    }

                    R.id.action_share_task -> {
                        onMenuAction(TaskMenuAction.OnShare(task)) // Envía la acción
                        true
                    }

                    else -> false
                }
            }

            // Muestra el menú
            popup.show()
        }

        /**
         * Este método es llamado por [BaseAdapter.onBindViewHolder]
         * para poblar las vistas con los datos del item.
         */
        override fun bind(item: TaskDomain) {
            // Usamos 'apply' para acceder al binding de forma más cómoda
            binding.apply {
                // Asignar datos básicos
                tvEventTitle.text = item.title

                // --- AÑADIDO: TACHADO PARA TAREA PRINCIPAL ---
                updateStrikeThrough(item.isDone)
                // --- FIN DE AÑADIDO ---

                tvEventType.text = item.typeTask
                tvEventLocation.text = item.place.takeIf { it.isNotEmpty() } ?: "Sin ubicación"
                tvDuration.text = item.duration.takeIf { it > 0 }
                    ?.let {
                        if (it > 60) {
                            val hours = it / 60
                            val minutes = it % 60
                            "${hours} h ${minutes} min"
                        } else {
                            "${it} min"
                        }
                    }
                    ?: "Sin Duración"

                // Formatear la hora (ej: 8 -> "08:00")
                // Hora (desde Int de minutos)
                val hours = item.hour / 60
                val minutes = item.hour % 60
                tvAttendees.text = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)



                // Lógica para calcular el progreso de las sub-tareas
                val totalSubtasks = item.subTasks.size
                // Asumimos que SubTaskDomain tiene un campo 'isDone: Boolean'
                val completedSubtasks = item.subTasks.count { it.isDone }

                if (totalSubtasks > 0) {
                    // Si hay sub-tareas, mostramos la sección de progreso
                    llSubtasks.visibility = View.VISIBLE

                    // Calculamos el porcentaje de progreso
                    val progress = (completedSubtasks * 100) / totalSubtasks

                    // Asignamos el progreso a ambas barras (circular y lineal)
                    pbLinearProgress.progress = progress
                    pbCircularProgress.progress = progress

                    // Mostramos el texto del progreso (ej: "60%")
                    tvProgressPercentage.text = "$progress%"
                    // Mostramos el contador de sub-tareas (ej: "3/5")
                    tvSubtasks.text = "$completedSubtasks/$totalSubtasks"

                } else {
                    // Si no hay sub-tareas, ocultamos la sección
                    llSubtasks.visibility = View.GONE
                    pbCircularProgress.visibility = View.GONE
                    pbLinearProgress.visibility = View.INVISIBLE
                    tvProgressPercentage.visibility = View.INVISIBLE


                    // Ponemos el progreso circular a 0
                    pbCircularProgress.progress = 0

                    tvProgressPercentage.text = "0%"
                }

            }

        }

        /**
         * Helper para aplicar o quitar el tachado del TÍTULO de la tarea.
         */
        private fun updateStrikeThrough(isDone: Boolean) {
            if (isDone) {
                binding.cbIsDone.isChecked = true
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.cbIsDone.isChecked = false
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    sealed class TaskMenuAction {
        data class OnEdit(val task: TaskDomain) : TaskMenuAction()
        data class OnDelete(val task: TaskDomain) : TaskMenuAction()
        data class OnShare(val task: TaskDomain) : TaskMenuAction()
    }


    /**
     * Clase interna para que [ListAdapter] (la clase padre de [BaseAdapter])
     * calcule eficientemente las diferencias entre la lista vieja y la nueva.
     */
    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {

        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.id == newItem.id // <-- Usa esto
        }

        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}

