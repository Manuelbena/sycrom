package com.manuelbena.synkron.presentation.calendar.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ItemCalendarTaskBinding // El Binding del nuevo XML
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.toHourString

class CalendarTaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onTaskCheckedChange: (TaskDomain, Boolean) -> Unit
) : ListAdapter<TaskDomain, CalendarTaskAdapter.ViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCalendarTaskBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskDomain) {
            binding.apply {
                val context = root.context

                // 1. Datos Básicos
                tvTitle.text = task.summary

                // 2. Ubicación (Ocultar si vacío)
                if (task.location.isNullOrEmpty()) {
                    ivLocation.visibility = View.GONE
                    tvLocation.visibility = View.GONE
                } else {
                    ivLocation.visibility = View.VISIBLE
                    tvLocation.visibility = View.VISIBLE
                    tvLocation.text = task.location
                }

                // 3. Horas (Lógica Todo el Día)
                val isAllDay = task.start?.dateTime == null && !task.start?.date.isNullOrEmpty()

                if (isAllDay) {
                    tvStartTime.text = "Todo"
                    tvEndTime.text = "el día"
                } else {
                    tvStartTime.text = task.start.toHourString()
                    // Si hay fin y es distinto, lo mostramos, si no, ocultamos
                    val endText = task.end.toHourString()
                    if (endText != "--:--" && endText != tvStartTime.text) {
                        tvEndTime.text = endText
                        tvEndTime.visibility = View.VISIBLE
                    } else {
                        tvEndTime.visibility = View.GONE
                    }
                }

                // 4. Color de Categoría
                try {
                    val colorRes = task.typeTask.getCategoryColor()
                    val colorInt = ContextCompat.getColor(context, colorRes)
                    viewCategoryIndicator.background.setTint(colorInt)
                } catch (e: Exception) {
                    viewCategoryIndicator.background.setTint(Color.LTGRAY)
                }

                // 5. Estado Completado (Visual)
                swCompleted.setOnCheckedChangeListener(null) // Evitar rebote al reciclar
                swCompleted.isChecked = task.isDone
                updateVisualState(task.isDone)

                // 6. Listeners
                root.setOnClickListener { onItemClick(task) }

                swCompleted.setOnCheckedChangeListener { _, isChecked ->
                    updateVisualState(isChecked)
                    onTaskCheckedChange(task, isChecked)
                }
            }
        }

        private fun updateVisualState(isDone: Boolean) {
            binding.apply {
                if (isDone) {
                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvTitle.alpha = 0.5f
                    // Fondo grisáceo si está hecha
                    cardRoot.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                    cardRoot.elevation = 0f
                } else {
                    tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    tvTitle.alpha = 1.0f
                    // Fondo blanco si está activa
                    cardRoot.setCardBackgroundColor(Color.WHITE)
                    cardRoot.elevation = 2f // Volvemos a elevarla
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem == newItem
    }
}