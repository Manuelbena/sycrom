package com.manuelbena.synkron.presentation.home.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
) : ListAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) { // <-- CAMBIO: Pasamos el DiffUtil

    // --- INICIO DE CAMBIO: Añadimos StableIds ---
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Usamos el ID de la tarea (Long) directamente
        return getItem(position).id
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

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
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }

            binding.btnTaskOptions.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    showPopupMenu(binding.btnTaskOptions, getItem(bindingAdapterPosition))
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

                updateStrikeThrough(item.isDone)

                // 1. Quitamos el listener para que no se dispare solo
                cbIsDone.setOnCheckedChangeListener(null)
                // 2. Asignamos el estado del check BASADO EN EL MODELO
                cbIsDone.isChecked = item.isDone
                // 3. Volvemos a poner el listener para clicks del usuario
                cbIsDone.setOnCheckedChangeListener { _, isChecked ->
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        val task = getItem(bindingAdapterPosition)
                        updateStrikeThrough(isChecked)
                        onTaskCheckedChange(task, isChecked)
                    }
                }


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

                if (item.subTasks.isNotEmpty()) {
                    pbLinearProgress.visibility = View.VISIBLE
                    pbCircularProgress.visibility = View.VISIBLE
                    tvProgressPercentage.visibility = View.VISIBLE

                    llSubtasks.visibility = View.VISIBLE
                    val progress = if (totalSubtasks > 0) (completedSubtasks * 100) / totalSubtasks else 0 // Evitar división por cero
                    pbLinearProgress.progress = progress
                    pbCircularProgress.progress = progress
                    tvProgressPercentage.text = "$progress%"
                    tvSubtasks.text = "$completedSubtasks/$totalSubtasks"
                } else {
                    pbLinearProgress.visibility = View.GONE
                    pbCircularProgress.visibility = View.GONE
                    tvProgressPercentage.visibility = View.GONE
                    llSubtasks.visibility = View.GONE // Ocultamos la barra de progreso
                    tvSubtasks.text = "Sin subtareas"
                    pbCircularProgress.progress = 0
                    tvProgressPercentage.text = "0%" // O " - " si prefieres
                }
            }
        }

        private fun updateStrikeThrough(isDone: Boolean) {
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

    // --- CAMBIO: Añadimos el DiffUtil.ItemCallback ---
    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {

        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem.id == newItem.id // Compara solo por ID
        }

        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain): Boolean {
            return oldItem == newItem // Compara todo el objeto
        }
    }
}
