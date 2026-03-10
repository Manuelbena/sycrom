package com.manuelbena.synkron.presentation.home.adapters

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTimelineBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.adapters.SubTaskAdapter
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.getCategoryIcon
import com.manuelbena.synkron.presentation.util.getName
import com.manuelbena.synkron.presentation.util.getPriorityColor
import com.manuelbena.synkron.presentation.util.toHourString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onItemClick: (TaskDomain) -> Unit,
    private val onTaskCheckedChange: (task: TaskDomain, isDone: Boolean) -> Unit,
    private val onSubTaskChange: (taskId: Int, subTask: SubTaskDomain) -> Unit
) : ListAdapter<TaskDomain, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    init { setHasStableIds(true) }

    private val expandedTaskIds = mutableSetOf<Int>()
    private val subtasksSnapshot = mutableMapOf<Int, List<SubTaskDomain>>()

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: TaskViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    inner class TaskViewHolder(private val binding: ItemTaskTimelineBinding) : BaseViewHolder<TaskDomain>(binding) {

        private var subTaskAdapter: SubTaskAdapter? = null
        private var isAutoChecking = false

        fun unbind() {
            binding.lottieCelebration.cancelAnimation()
            binding.lottieCelebration.visibility = View.GONE
            binding.cbTaskDone.setOnCheckedChangeListener(null)
            binding.ivChevron.animate().cancel()
            isAutoChecking = false
        }

        init {
            binding.cardTask.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        override fun bind(item: TaskDomain) {
            isAutoChecking = false

            with(binding) {
                // 1. Textos
                tvTaskTitle.text = item.summary
                tvTaskSubtitle.text = item.description
                tvTaskSubtitle.isVisible = !item.description.isNullOrEmpty()

                // 2. Línea de Tiempo y Alpha Color
                setupTimelineSection(item)

                // 3. Tags (Categoría y Prioridad)
                setupTagsSection(item)

                // 4. Subtareas y Acordeón
                val mutableSubtasks = item.subTasks.toMutableList()
                setupSubtasksSection(item, mutableSubtasks)

                // 5. Lógica Compleja de Completado
                setupCompletionState(item, mutableSubtasks)
            }
        }

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
                        onSubTaskChange(item.id, updatedSubtask)

                        val areAllDone = mutableSubtasks.all { it.isDone }
                        val isParentChecked = binding.cbTaskDone.isChecked

                        if (areAllDone && !isParentChecked) {
                            binding.root.post {
                                isAutoChecking = true
                                binding.cbTaskDone.isChecked = true
                                playConfettiAnimation(item.copy(subTasks = mutableSubtasks))
                                isAutoChecking = false
                            }
                        } else if (!areAllDone && isParentChecked) {
                            binding.root.post {
                                isAutoChecking = true
                                binding.cbTaskDone.isChecked = false
                                isAutoChecking = false
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

                // Lógica visual del Acordeón
                val isExpanded = expandedTaskIds.contains(item.id)
                binding.llSubtasksContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.ivChevron.rotation = if (isExpanded) 90f else -90f

                binding.lySubtasksHeader.setOnClickListener {
                    val currentlyExpanded = expandedTaskIds.contains(item.id)
                    if (currentlyExpanded) expandedTaskIds.remove(item.id) else expandedTaskIds.add(item.id)

                    binding.llSubtasksContainer.visibility = if (!currentlyExpanded) View.VISIBLE else View.GONE
                    binding.ivChevron.animate().rotation(if (!currentlyExpanded) 90f else -90f).start()
                }
            } else {
                binding.llSubtasksContainer.visibility = View.GONE
            }
        }

        private fun setupCompletionState(item: TaskDomain, mutableSubtasks: MutableList<SubTaskDomain>) {
            binding.cbTaskDone.setOnCheckedChangeListener(null)
            binding.cbTaskDone.isChecked = item.isDone
            updateVisualDoneState(item.isDone)
            if (!item.isDone) binding.lottieCelebration.visibility = View.GONE

            binding.cbTaskDone.setOnCheckedChangeListener { _, isChecked ->
                updateVisualDoneState(isChecked)

                if (mutableSubtasks.isNotEmpty()) {
                    val newSubtasksList = if (isChecked) {
                        if (!isAutoChecking) subtasksSnapshot[item.id] = mutableSubtasks.toList()
                        mutableSubtasks.map { it.copy(isDone = true) }
                    } else {
                        if (isAutoChecking) mutableSubtasks.toList()
                        else subtasksSnapshot[item.id] ?: mutableSubtasks.map { it.copy(isDone = false) }
                    }

                    mutableSubtasks.clear()
                    mutableSubtasks.addAll(newSubtasksList)
                    subTaskAdapter?.submitList(newSubtasksList)
                    updateSubtasksHeaderUI(newSubtasksList)

                    val updatedTask = item.copy(subTasks = newSubtasksList)
                    if (isChecked && !isAutoChecking) playConfettiAnimation(updatedTask)
                    else onTaskCheckedChange(updatedTask, isChecked)
                } else {
                    if (isChecked && !isAutoChecking) playConfettiAnimation(item)
                    else onTaskCheckedChange(item, isChecked)
                }
            }
        }

        private fun playConfettiAnimation(task: TaskDomain) {
            onTaskCheckedChange(task, true)
            binding.lottieCelebration.apply {
                visibility = View.VISIBLE
                playAnimation()
                removeAllAnimatorListeners()
                addAnimatorListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator) { visibility = View.GONE }
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

            if (completed == total && total > 0) {
                binding.textViewTaskDetailProgressText.paintFlags = binding.textViewTaskDetailProgressText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewTaskDetailProgressText.paintFlags = binding.textViewTaskDetailProgressText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }

        private fun updateVisualDoneState(isDone: Boolean) {
            val targetAlpha = if (isDone) 0.5f else 1.0f
            binding.tvTaskTitle.apply {
                paintFlags = if (isDone) paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG else paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                alpha = targetAlpha
            }
            binding.tvTaskSubtitle.alpha = targetAlpha
            binding.llTags.alpha = targetAlpha
            binding.lySubtasksHeader.alpha = targetAlpha
        }

        private fun setupTimelineSection(item: TaskDomain) {
            binding.apply {
                tvTimelineTimeStart.text = item.start?.toHourString() ?: "--:--"
                tvTimelineTimeEnd.text = item.end?.toHourString() ?: "--:--"

                val context = root.context
                val pureColor = ContextCompat.getColor(context, item.typeTask.getCategoryColor())
                val alphaColor = androidx.core.graphics.ColorUtils.setAlphaComponent(pureColor, 38)

                cardTask.setCardBackgroundColor(alphaColor)
                ivTimelineDotStart.backgroundTintList = ColorStateList.valueOf(pureColor)
                ivTimelineDotEnd.backgroundTintList = ColorStateList.valueOf(pureColor)
                viewLineMiddle.setBackgroundColor(alphaColor)

                val nowMillis = System.currentTimeMillis()
                val startMillis = item.start?.dateTime
                val endMillis = item.end?.dateTime ?: (startMillis?.plus(3600000) ?: 0L)

                if (!item.isDone && startMillis != null && nowMillis in startMillis..<endMillis) {
                    layoutCurrentTimeIndicator.isVisible = true
                    tvCurrentTimeBubble.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))

                    val percentage = if (endMillis > startMillis) ((nowMillis - startMillis).toFloat() / (endMillis - startMillis)) else 0.5f
                    val params = layoutCurrentTimeIndicator.layoutParams as ConstraintLayout.LayoutParams
                    params.verticalBias = percentage.coerceIn(0f, 1f)
                    layoutCurrentTimeIndicator.layoutParams = params

                    ivTimelineDotStart.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                } else {
                    layoutCurrentTimeIndicator.isVisible = false
                }
            }
        }

        private fun setupTagsSection(item: TaskDomain) {
            val context = binding.root.context

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
                icon?.setBounds(0, 0, (12 * context.resources.displayMetrics.density).toInt(), (12 * context.resources.displayMetrics.density).toInt())
                setCompoundDrawables(icon, null, null, null)
            }

            // 2. Prioridad
            val priorityName = item.priority
            binding.tvPriorityTag.isVisible = priorityName.isNotEmpty()

            if (priorityName.isNotEmpty()) {
                val prioColorRes = priorityName.getPriorityColor()
                val prioColor = ContextCompat.getColor(context, prioColorRes)

                binding.tvPriorityTag.apply {
                    text = priorityName.uppercase()
                    setTextColor(prioColor)
                    background.setTint(prioColor)
                    background.alpha = 50
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskDomain>() {
        override fun areItemsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TaskDomain, newItem: TaskDomain) = oldItem == newItem
    }
}