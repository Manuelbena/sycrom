package com.manuelbena.synkron.presentation.home.adapters

import android.animation.Animator
import android.annotation.SuppressLint
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.adapters.SubTaskAdapter
import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.getCategoryIcon
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.getName
import com.manuelbena.synkron.presentation.util.getPriorityColor
import com.manuelbena.synkron.presentation.util.toHourString

class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onMenuAction: (TaskMenuAction) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit,
    private val onSubTaskChange: (taskId: Int, subTask: SubTaskDomain) -> Unit
) : ListAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    // Mantiene el estado de expansión de las tareas al hacer scroll
    private val expandedTaskIds = mutableSetOf<Int>()

    // "Memoria" para restaurar el estado de las subtareas si el usuario desmarca la tarea padre
    private val subtasksSnapshot = mutableMapOf<Int, List<SubTaskDomain>>()

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

    override fun onViewRecycled(holder: TaskViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskTodayBinding
    ) : BaseViewHolder<TaskDomain>(binding) {

        private var subTaskAdapter: SubTaskAdapter? = null

        // Bandera para diferenciar acciones del Usuario (click) vs del Sistema (lógica automática)
        // Evita bucles infinitos y corrupción de datos en el snapshot
        private var isAutoChecking = false

        fun unbind() {
            // Limpieza crítica para evitar leaks y comportamientos erráticos al reciclar
            binding.lottieCelebration.cancelAnimation()
            binding.lottieCelebration.visibility = View.GONE
            binding.swTaskCompleted.setOnCheckedChangeListener(null)
            binding.ivChevron.animate().cancel()
            isAutoChecking = false // Reseteo de seguridad
        }

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
            binding.btnTaskOptions.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    showPopupMenu(it, getItem(bindingAdapterPosition))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        override fun bind(item: TaskDomain) {
            // Aseguramos estado limpio al bindear
            isAutoChecking = false

            with(binding) {
                // 1. Datos básicos
                tvEventTitle.text = item.summary
                tvDescription.text = item.description
                tvDescription.isVisible = !item.description.isNullOrEmpty()

                // 2. Secciones Visuales
                setupTimeSection(item)
                setupMetadataSection(item)
                setupTagsSection(item)

                // 3. Subtareas (Usamos copia mutable para gestión optimista en UI)
                val mutableSubtasks = item.subTasks.toMutableList()

                // 4. Configuración Lógica Compleja
                setupSubtasksSection(item, mutableSubtasks)
                setupCompletionState(item, mutableSubtasks)
            }
        }

        /**
         * Maneja la lógica PADRE -> HIJOS
         * Si marcas el padre, se marcan todos los hijos.
         * Si desmarcas el padre, se restaura el estado anterior (snapshot).
         */
        private fun setupCompletionState(item: TaskDomain, mutableSubtasks: MutableList<SubTaskDomain>) {
            binding.swTaskCompleted.setOnCheckedChangeListener(null)

            // Estado visual inicial
            binding.swTaskCompleted.isChecked = item.isDone
            updateVisualDoneState(item.isDone)

            if (!item.isDone) {
                binding.lottieCelebration.visibility = View.GONE
            }

            binding.swTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                updateVisualDoneState(isChecked)

                if (mutableSubtasks.isNotEmpty()) {
                    val newSubtasksList: List<SubTaskDomain>

                    if (isChecked) {
                        // CASO 1: Tarea COMPLETADA
                        if (!isAutoChecking) {
                            // Solo guardamos foto si fue MANUAL.
                            subtasksSnapshot[item.id] = mutableSubtasks.toList()
                        }
                        // Marcamos todas como hechas
                        newSubtasksList = mutableSubtasks.map { it.copy(isDone = true) }
                    } else {
                        // CASO 2: Tarea DESMARCADA
                        if (isAutoChecking) {
                            // Si es automático (por desmarcar un hijo), mantenemos el estado actual
                            newSubtasksList = mutableSubtasks.toList()
                        } else {
                            // Si es manual, intentamos restaurar la "foto" anterior
                            val snapshot = subtasksSnapshot[item.id]
                            newSubtasksList = snapshot ?: mutableSubtasks.map { it.copy(isDone = false) }
                        }
                    }

                    // Actualizar UI Local y Adapter
                    mutableSubtasks.clear()
                    mutableSubtasks.addAll(newSubtasksList)
                    subTaskAdapter?.submitList(newSubtasksList)
                    updateSubtasksHeaderUI(newSubtasksList)

                    // Guardamos Padre + Hijos (Transacción atómica lógica)
                    val updatedTask = item.copy(subTasks = newSubtasksList)
                    if (isChecked) playConfettiAnimation(updatedTask)
                    else onTaskCheckedChange(updatedTask, false)

                } else {
                    // Tarea simple sin subtareas
                    if (isChecked) playConfettiAnimation(item)
                    else onTaskCheckedChange(item, false)
                }
            }
        }

        /**
         * Maneja la lógica HIJOS -> PADRE
         * Si completas todos los hijos -> Se marca el padre.
         * Si desmarcas un hijo y el padre estaba hecho -> Se desmarca el padre.
         */
        private fun setupSubtasksSection(item: TaskDomain, mutableSubtasks: MutableList<SubTaskDomain>) {
            val hasSubtasks = mutableSubtasks.isNotEmpty()
            binding.lySubtasksHeader.isVisible = hasSubtasks

            if (hasSubtasks) {
                updateSubtasksHeaderUI(mutableSubtasks)

                subTaskAdapter = SubTaskAdapter { subTask, isChecked ->
                    val index = mutableSubtasks.indexOfFirst { it.id == subTask.id }
                    if (index != -1) {
                        val updatedSubtask = subTask.copy(isDone = isChecked)
                        mutableSubtasks[index] = updatedSubtask

                        updateSubtasksHeaderUI(mutableSubtasks)

                        // Verificación Maestra
                        val areAllDone = mutableSubtasks.all { it.isDone }
                        val isParentChecked = binding.swTaskCompleted.isChecked

                        if (areAllDone && !isParentChecked) {
                            // CASO A: Todas completas -> Marcar Padre Automáticamente
                            // Usamos post para evitar conflictos de UI y activar la bandera de seguridad
                            binding.root.post {
                                isAutoChecking = true
                                binding.swTaskCompleted.isChecked = true
                                isAutoChecking = false
                            }
                        } else {
                            // CASO B: Cambio normal -> Guardar solo la subtarea
                            onSubTaskChange(item.id, updatedSubtask)

                            if (!areAllDone && isParentChecked) {
                                // CASO C: Se desmarcó una y padre estaba hecho -> Desmarcar Padre
                                binding.root.post {
                                    isAutoChecking = true
                                    binding.swTaskCompleted.isChecked = false
                                    isAutoChecking = false
                                }
                            }
                        }
                    }
                }

                binding.rvSubtasks.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = subTaskAdapter
                    isNestedScrollingEnabled = false
                    setRecycledViewPool(RecyclerView.RecycledViewPool())
                }
                subTaskAdapter?.submitList(mutableSubtasks.toList())

                // Lógica de Expansión/Colapso
                val isExpanded = expandedTaskIds.contains(item.id)
                setupExpansionUI(isExpanded)

                binding.lySubtasksHeader.setOnClickListener {
                    val currentlyExpanded = expandedTaskIds.contains(item.id)
                    if (currentlyExpanded) {
                        expandedTaskIds.remove(item.id)
                        animateExpansion(false)
                    } else {
                        expandedTaskIds.add(item.id)
                        animateExpansion(true)
                    }
                }
            } else {
                binding.llSubtasksContainer.visibility = View.GONE
            }
        }

        // --- Helpers Visuales ---

        private fun setupExpansionUI(isExpanded: Boolean) {
            binding.llSubtasksContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivChevron.rotation = if (isExpanded) 90f else -90f
        }

        private fun animateExpansion(expand: Boolean) {
            binding.llSubtasksContainer.visibility = if (expand) View.VISIBLE else View.GONE
            val targetRotation = if (expand) 90f else -90f
            binding.ivChevron.animate().rotation(targetRotation).start()
        }

        private fun playConfettiAnimation(task: TaskDomain) {
            binding.lottieCelebration.apply {
                visibility = View.VISIBLE
                playAnimation()
                removeAllAnimatorListeners()
                addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                        onTaskCheckedChange(task, true)
                    }
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) { visibility = View.GONE }
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }
        }

        private fun updateSubtasksHeaderUI(subtasks: List<SubTaskDomain>) {
            val total = subtasks.size
            val completed = subtasks.count { it.isDone }
            binding.textViewTaskDetailProgressText.text = "Subtareas ($completed/$total)"
            // Tachar texto si está completo
            if (completed == total && total > 0) {
                binding.textViewTaskDetailProgressText.paintFlags = binding.textViewTaskDetailProgressText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewTaskDetailProgressText.paintFlags = binding.textViewTaskDetailProgressText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        private fun updateVisualDoneState(isDone: Boolean) {
            if (isDone) {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvEventTitle.alpha = 0.5f
            } else {
                binding.tvEventTitle.paintFlags = binding.tvEventTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvEventTitle.alpha = 1.0f
            }
        }

        private fun setupTimeSection(item: TaskDomain) {
            val startStr = item.start?.toHourString() ?: "--:--"
            val endStr = item.end?.toHourString() ?: "--:--"
            val isAllDay = item.start?.dateTime == null && !item.start?.date.isNullOrEmpty()

            if (isAllDay) {
                binding.tvEventTime.text = "Todo el día"
                binding.tvDuration.isVisible = false
                binding.iconTime.setImageResource(R.drawable.ic_calendar_today)
            } else {
                binding.tvEventTime.text = if (endStr != "--:--" && endStr != startStr) "$startStr - $endStr" else startStr
                binding.iconTime.setImageResource(R.drawable.ic_clock_complete)
                val duration = getDurationInMinutes(item.start, item.end)
                binding.tvDuration.isVisible = duration > 0
                if (duration > 0) binding.tvDuration.text = duration.toDurationString()
            }
        }

        private fun setupMetadataSection(item: TaskDomain) {
            val isRecurring = item.recurrence.isNotEmpty()
            binding.ivRepeat.parent.let { (it as View).isVisible = isRecurring }
            if (isRecurring) binding.tvFrequency.text = "Repetir"

            val remindersCount = item.reminders.overrides?.size ?: 0
            val hasAlerts = remindersCount > 0
            binding.ivAlert.parent.let { (it as View).isVisible = hasAlerts }
            if (hasAlerts) binding.tvAlertCount.text = remindersCount.toString()
        }

        private fun setupTagsSection(item: TaskDomain) {
            val context = binding.root.context

            // Función auxiliar inline para convertir DP a Píxeles
            fun dpToPx(dp: Int) = (dp * context.resources.displayMetrics.density).toInt()
            val iconSize = dpToPx(12)

            // 1. Categoría
            val categoryName = item.typeTask.ifEmpty { "General" }
            val catColorRes = categoryName.getCategoryColor()
            val catIconRes = categoryName.getCategoryIcon()
            val catColor = ContextCompat.getColor(context, catColorRes)

            binding.tvCategoryTag.apply {
                text = categoryName.getName()
                setTextColor(catColor)
                background.setTint(catColor)
                background.alpha = 50

                val icon = ContextCompat.getDrawable(context, catIconRes)
                icon?.setTint(catColor)
                icon?.setBounds(0, 0, iconSize, iconSize)
                setCompoundDrawables(icon, null, null, null)
            }

            // 2. Prioridad
            val priorityName = item.priority
            binding.tvPriorityTag.isVisible = priorityName.isNotEmpty()

            if (priorityName.isNotEmpty()) {
                val prioColorRes = priorityName.getPriorityColor()
                // Usamos ic_mark porque 'flag' no existe en tus drawables subidos
                val prioIconRes = R.drawable.flag
                val prioColor = ContextCompat.getColor(context, prioColorRes)

                binding.tvPriorityTag.apply {
                    text = priorityName
                    setTextColor(prioColor)
                    background.setTint(prioColor)
                    background.alpha = 50

                    val flagIcon = ContextCompat.getDrawable(context, prioIconRes)
                    flagIcon?.setTint(prioColor)
                    flagIcon?.setBounds(0, 0, iconSize, iconSize)
                    setCompoundDrawables(flagIcon, null, null, null)
                }
            }
        }

        private fun showPopupMenu(anchorView: View, task: TaskDomain) {
            val contextWrapper =
                ContextThemeWrapper(anchorView.context, R.style.MyPopupMenuStyle)
            val popup = PopupMenu(contextWrapper, anchorView)

            popup.menuInflater.inflate(R.menu.menu_item_task, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_task -> { onMenuAction(TaskMenuAction.OnEdit(task)); true }
                    R.id.action_delete_task -> { onMenuAction(TaskMenuAction.OnDelete(task)); true }
                    R.id.action_share_task -> { onMenuAction(TaskMenuAction.OnShare(task)); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    sealed class TaskMenuAction {
        data class OnEdit(val task: TaskDomain) : TaskMenuAction()
        data class OnDelete(val task: TaskDomain) : TaskMenuAction()
        data class OnShare(val task: TaskDomain) : TaskMenuAction()
    }

    private class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain) =
            if (oldItem.id != 0 && newItem.id != 0) oldItem.id == newItem.id else oldItem.summary == newItem.summary
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem == newItem
    }
}