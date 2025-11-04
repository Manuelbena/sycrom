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


class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onMenuAction: (TaskMenuAction) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit
) : BaseAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskTodayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskTodayBinding
    ) : BaseViewHolder<TaskDomain>(binding) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }

            binding.btnTaskOptions.setOnClickListener {
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    showPopupMenu(binding.btnTaskOptions, getItem(bindingAdapterPosition))
                }
            }

            binding.cbIsDone.setOnClickListener {
                if (bindingAdapterPosition != DiffUtil.DiffResult.NO_POSITION) {
                    val item = getItem(bindingAdapterPosition)
                    val newIsDone = !item.isDone

                    updateStrikeThrough(newIsDone)

                    onTaskCheckedChange(item, newIsDone)
                }
            }
        }

        private fun showPopupMenu(anchorView: View, task: TaskDomain) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.menuInflater.inflate(R.menu.menu_item_task, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_task -> {
                        onMenuAction(TaskMenuAction.OnEdit(task))
                        true
                    }
                    R.id.action_delete_task -> {
                        onMenuAction(TaskMenuAction.OnDelete(task))
                        true
                    }
                    R.id.action_share_task -> {
                        onMenuAction(TaskMenuAction.OnShare(task))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        override fun bind(item: TaskDomain) {
            binding.apply {
                tvEventTitle.text = item.title

                // --- MODIFICACIÓN ---
                // Sincroniza el checkbox y el tachado
                updateStrikeThrough(item.isDone)
                // --- FIN MODIFICACIÓN ---

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

                val hours = item.hour / 60
                val minutes = item.hour % 60
                tvAttendees.text = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)

                val totalSubtasks = item.subTasks.size
                val completedSubtasks = item.subTasks.count { it.isDone }

                if (totalSubtasks > 0) {
                    llSubtasks.visibility = View.VISIBLE
                    val progress = (completedSubtasks * 100) / totalSubtasks
                    pbLinearProgress.progress = progress
                    pbCircularProgress.progress = progress
                    tvProgressPercentage.text = "$progress%"
                    tvSubtasks.text = "$completedSubtasks/$totalSubtasks"
                } else {
                    llSubtasks.visibility = View.GONE
                    pbCircularProgress.visibility = View.GONE
                    pbLinearProgress.visibility = View.INVISIBLE
                    tvProgressPercentage.visibility = View.INVISIBLE
                    pbCircularProgress.progress = 0
                    tvProgressPercentage.text = "0%"
                }
            }
        }

        private fun updateStrikeThrough(isDone: Boolean) {
            // --- MODIFICACIÓN ---
            // Asegura que el checkbox esté sincronizado
            binding.cbIsDone.isChecked = isDone
            // --- FIN MODIFICACIÓN ---

            if (isDone) {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    sealed class TaskMenuAction {
        data class OnEdit(val task: TaskDomain) : TaskMenuAction()
        data class OnDelete(val task: TaskDomain) : TaskMenuAction()
        data class OnShare(val task: TaskDomain) : TaskMenuAction()
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {

        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            // --- MODIFICACIÓN ---
            return oldItem.id == newItem.id // <-- Usar ID es crucial
            // --- FIN MODIFICACIÓN ---
        }

        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}
