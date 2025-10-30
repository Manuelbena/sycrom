package com.manuelbena.synkron.presentation.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.manuelbena.synkron.base.BaseAdapter
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Adaptador para la lista de tareas (Tasks) en el RecyclerView del Home.
 * Extiende de [BaseAdapter] usando [TaskDomain] como modelo y [TaskViewHolder] como ViewHolder.
 *
 * @param onItemClick Lambda que se ejecuta cuando se pulsa en un item de la lista.
 */
class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit
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
                tvEventType.text = item.typeTask
                tvEventLocation.text = item.place.takeIf { it.isNotEmpty() } ?: "Sin ubicación"
                tvDuration.text = item.duration.takeIf { it > 0 }
                    ?.let { "${it}min" }
                    ?: "Sin Duración"

                // Formatear la hora (ej: 8 -> "08:00")
                // Hora (desde Int de minutos)
                val hours = item.hour / 60
                val minutes = item.hour % 60
                tvAttendees.text =  String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)

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
    }

    /**
     * Clase interna para que [ListAdapter] (la clase padre de [BaseAdapter])
     * calcule eficientemente las diferencias entre la lista vieja y la nueva.
     */
    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {

        /**
         * Comprueba si dos items representan la misma entidad.
         * Idealmente se usa un ID único. A falta de él, usamos una combinación.
         */
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.title == newItem.title && oldItem.date == newItem.date
        }

        /**
         * Comprueba si los contenidos de dos items son idénticos.
         * Como [TaskDomain] es una 'data class', podemos usar '=='
         */
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}