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
import com.manuelbena.synkron.presentation.models.TaskPresentation

class TaskAdapter(
    private val onItemClicked: (TaskPresentation) -> Unit
) :
    BaseAdapter<TaskPresentation, TaskAdapter.TaskViewHolder>(
        diffCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder =
        TaskViewHolder(
            ItemTaskTodayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )


    inner class TaskViewHolder(private val binding: ItemTaskTodayBinding) :
        BaseViewHolder<TaskPresentation>(binding) {
        @SuppressLint("SetTextI18n")
        override fun bind(data: TaskPresentation) {
            binding.tvEventTitle.text = data.title
            binding.tvEventType.text = data.typeTask
            binding.tvEventLocation.text = data.place
            binding.tvAttendees.text = data.hour
            binding.tvProgressPercentage.text = "${calcularPorcentajeSubtareasCompletadas(listOf(data)).toInt()}%"
            binding.pbCircularProgress.progress = calcularPorcentajeSubtareasCompletadas(listOf(data)).toInt()
            binding.pbLinearProgress.progress = calcularPorcentajeSubtareasCompletadas(listOf(data)).toInt()
            binding.tvSubtasks.text = "${data.subTasks.count(){it.isDone}} / ${data.subTasks.size} subtareas"

            if (data.isDone){
                binding.tvEventTitle.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvEventTitle.setTextColor(Color.GRAY)
            }
            if (data.subTasks.isEmpty()){
                binding.llSubtasks.visibility = View.INVISIBLE
                binding.progressCircular.visibility = View.INVISIBLE
            }
            binding.root.setOnClickListener {
                // Cuando se hace clic, llama a la función lambda pasando la tarea actual
                onItemClicked(data)
            }
        }
    }
    fun calcularPorcentajeSubtareasCompletadas(tareas: List<TaskPresentation>): Double {
        // 1. Contar el número total de subtareas en todas las tareas.
        // Usamos flatMap para unir todas las listas de subtareas en una sola.
        val todasLasSubtareas = tareas.flatMap { it.subTasks }
        val totalSubtareas = todasLasSubtareas.size

        // Si no hay ninguna subtarea, el progreso es 0.
        if (totalSubtareas == 0) {
            return 0.0
        }

        // 2. Contar cuántas de esas subtareas están completadas (isDone = true).
        val subtareasCompletadas = todasLasSubtareas.count { it.isDone }

        // 3. Calcular el porcentaje y devolverlo.
        // Convertimos los números a Double para obtener un resultado con decimales.
        return (subtareasCompletadas.toDouble() / totalSubtareas.toDouble()) * 100
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<TaskPresentation>() {
            override fun areItemsTheSame(
                oldItem: TaskPresentation,
                newItem: TaskPresentation
            ): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(
                oldItem: TaskPresentation,
                newItem: TaskPresentation
            ): Boolean {
                TODO("Not yet implemented")
            }


        }
    }



}