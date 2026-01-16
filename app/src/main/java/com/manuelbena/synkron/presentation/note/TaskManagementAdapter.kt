package com.manuelbena.synkron.presentation.note // Asegúrate del paquete correcto

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.databinding.ItemTaskStaggeredBinding
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding

import com.manuelbena.synkron.domain.models.TaskDomain

class TaskManagementAdapter(
    private val onTaskClick: (TaskDomain) -> Unit
) : ListAdapter<TaskDomain, TaskManagementAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskStaggeredBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskStaggeredBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskDomain) {
            binding.tvTitle.text = task.summary
            binding.tvDescription.text = task.description
            binding.tvDate.text = task.start?.toString()




            binding.root.setOnClickListener {
                onTaskClick(task)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.id == newItem.id // Asumiendo que tiene ID
        }

        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}