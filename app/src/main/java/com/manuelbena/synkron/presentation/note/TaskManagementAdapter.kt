package com.manuelbena.synkron.presentation.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemTaskStaggeredBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.toLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskManagementAdapter(
    private val onItemClick: (TaskDomain) -> Unit
) : ListAdapter<TaskDomain, TaskManagementAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
            ItemTaskStaggeredBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskStaggeredBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskDomain) {
            val context = binding.root.context

            binding.tvTitle.text = task.summary
            binding.tvCategory.text = task.typeTask.uppercase()

            // Descripción: Si está vacía, ocultamos para que el mosaico se ajuste
            if (task.description.isNullOrEmpty()) {
                binding.tvDescription.visibility = View.GONE
            } else {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = task.description
            }

            // Fecha
            val date = task.start.toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES"))
            binding.tvDate.text = date.format(formatter)

            // Color de categoría
            try {
                val colorRes = task.typeTask.getCategoryColor()
                val colorInt = ContextCompat.getColor(context, colorRes)
                binding.viewCategoryIndicator.background.setTint(colorInt)
            } catch (e: Exception) {
                // Fallback
            }

            // Prioridad
            if (task.priority.equals("Alta", ignoreCase = true)) {
                binding.ivPriority.visibility = View.VISIBLE
            } else {
                binding.ivPriority.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem == newItem
    }
}