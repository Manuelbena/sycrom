package com.manuelbena.synkron.presentation.home.adapters

import TaskDomain
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding

import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import com.manuelbena.synkron.presentation.util.getDurationInMinutes

import com.manuelbena.synkron.presentation.util.toHourString

class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onMenuAction: (TaskMenuAction) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit
) : ListAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    init {
        setHasStableIds(true)
    }

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

    inner class TaskViewHolder(
        private val binding: ItemTaskTodayBinding
    ) : BaseViewHolder<TaskDomain>(binding) {

        init {
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
                    onTaskCheckedChange(getItem(bindingAdapterPosition), binding.cbIsDone.isChecked)
                }
            }
        }

        private fun showPopupMenu(anchorView: View, task: TaskDomain) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.menuInflater.inflate(R.menu.menu_item_task, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_task -> onMenuAction(TaskMenuAction.OnEdit(task))
                    R.id.action_delete_task -> onMenuAction(TaskMenuAction.OnDelete(task))
                    R.id.action_share_task -> onMenuAction(TaskMenuAction.OnShare(task))
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            popup.show()
        }

        /**
         * Rellena la vista con los datos del nuevo TaskDomain.
         */
        override fun bind(item: TaskDomain) {
            binding.apply {

                // --- 1. Mapeo de Título y Categoría ---
                tvEventTitle.text = item.summary // CAMBIO
                chipCategory.text = item.typeTask // CAMBIO (usando el ID del chip)

                // --- 2. Mapeo de Contexto (Línea por Línea) ---

                // Ubicación
                if (item.location.isNullOrEmpty()) { // CAMBIO
                    tvEventLocation.visibility = View.GONE
                    iconLocation.visibility = View.GONE
                } else {
                    tvEventLocation.visibility = View.VISIBLE
                    iconLocation.visibility = View.VISIBLE
                    tvEventLocation.text = item.location
                }

                // Hora (usando el helper .toHourString())
                tvEventTime.text = item.start.toHourString() // CAMBIO

                // Duración (calculada con helpers)
                val durationInMinutes = getDurationInMinutes(item.start, item.end) // CAMBIO
                if (durationInMinutes > 0) {
                    tvDuration.visibility = View.VISIBLE
                    iconDuration.visibility = View.VISIBLE
                    tvDuration.text = durationInMinutes.toDurationString() // CAMBIO
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

                cbIsDone.setOnCheckedChangeListener(null)
                cbIsDone.isChecked = item.isDone
                cbIsDone.setOnCheckedChangeListener { _, isChecked ->
                    onTaskCheckedChange(item, isChecked)
                }

                if (item.isDone) {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

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
    }

    sealed class TaskMenuAction {
        data class OnEdit(val task: TaskDomain) : TaskMenuAction()
        data class OnDelete(val task: TaskDomain) : TaskMenuAction()
        data class OnShare(val task: TaskDomain) : TaskMenuAction()
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem
        }
    }
}