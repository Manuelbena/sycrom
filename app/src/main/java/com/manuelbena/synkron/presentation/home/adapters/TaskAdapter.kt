package com.manuelbena.synkron.presentation.home.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.base.BaseAdapter
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class
TaskAdapter(
    private val onItemClicked: (TaskDomain) -> Unit
) :
    BaseAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(
        diffCallback // Usar el DiffUtil definido abajo
    ) {

    // Formateador para la hora, inicializado una vez
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))
        .withZone(ZoneId.systemDefault())


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
        TaskViewHolder(
            ItemTaskTodayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    inner class TaskViewHolder(private val binding: ItemTaskTodayBinding) :
        BaseViewHolder<TaskDomain>(binding) {
        @SuppressLint("SetTextI18n")
        override fun bind(data: TaskDomain) {
            binding.tvEventTitle.text = data.title
            binding.tvEventType.text = data.typeTask
            binding.tvEventLocation.text = data.place ?: "Sin ubicación" // Manejar nulo

            // Formatear la hora desde el Long (timestamp)
            val instant = Instant.ofEpochMilli(data.hour.toLong()) // Asumiendo que 'hour' es el timestamp completo
            binding.tvAttendees.text = timeFormatter.format(instant) // Reutilizado tvAttendees para la hora

            val progress = calcularPorcentajeSubtareasCompletadas(listOf(data)).toInt()
            binding.tvProgressPercentage.text = "$progress%"
            binding.pbCircularProgress.progress = progress
            binding.pbLinearProgress.progress = progress

            val completedSubtasks = data.subTasks.count { it.isDone }
            binding.tvSubtasks.text = "$completedSubtasks / ${data.subTasks.size} subtareas"

            // Aplicar o quitar el tachado
            if (data.isDone){
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvEventTitle.setTextColor(Color.GRAY)
            } else {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                // Restaurar color original (ajusta según tu tema)
                binding.tvEventTitle.setTextColor(binding.root.context.getColor(com.google.android.material.R.color.material_dynamic_primary0))
            }

            // Ocultar/Mostrar sección de subtareas
            val hasSubtasks = data.subTasks.isNotEmpty()
            binding.llSubtasks.visibility = if (hasSubtasks) View.VISIBLE else View.INVISIBLE
            binding.progressCircular.visibility = if (hasSubtasks) View.VISIBLE else View.INVISIBLE


            binding.root.setOnClickListener {
                onItemClicked(data)
            }
        }
    }

    // Función auxiliar movida fuera del ViewHolder
    private fun calcularPorcentajeSubtareasCompletadas(tareas: List<TaskDomain>): Double {
        val todasLasSubtareas = tareas.flatMap { it.subTasks }
        val totalSubtareas = todasLasSubtareas.size
        if (totalSubtareas == 0) return 0.0
        val subtareasCompletadas = todasLasSubtareas.count { it.isDone }
        return (subtareasCompletadas.toDouble() / totalSubtareas.toDouble()) * 100
    }

    companion object {
        // DiffUtil Callback implementado correctamente
        private val diffCallback = object : DiffUtil.ItemCallback<TaskDomain>() {
            override fun areItemsTheSame(
                oldItem: TaskDomain,
                newItem: TaskDomain
            ): Boolean {
                // Compara por un identificador único si lo tienes, sino por título/fecha/hora
                return oldItem.title == newItem.title && oldItem.date == newItem.date && oldItem.hour == newItem.hour // Ejemplo si no hay ID único
            }

            @SuppressLint("DiffUtilEquals") // Suprimir advertencia si TaskDomain es data class
            override fun areContentsTheSame(
                oldItem: TaskDomain,
                newItem: TaskDomain
            ): Boolean {
                // Compara todo el contenido del objeto.
                // Si TaskDomain es una data class, la comparación '==' funciona bien.
                return oldItem == newItem
            }
        }
    }
}
