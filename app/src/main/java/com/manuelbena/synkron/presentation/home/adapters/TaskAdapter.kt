package com.manuelbena.synkron.presentation.home.adapters

import android.animation.Animator
import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.LayerDrawable
import android.os.Build

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseViewHolder
import com.manuelbena.synkron.databinding.ItemTaskTodayBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import android.util.TypedValue // ⬅️ IMPORTANTE
import com.google.android.material.R as R_Material // ⬅️ IMPORTANTE

import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toDurationString

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

    @RequiresApi(Build.VERSION_CODES.P)
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



        @RequiresApi(Build.VERSION_CODES.P)
        override fun bind(item: TaskDomain) {
            binding.apply {
                val context = binding.root.context

                // =============================================================
                // 1. MAPEO DE DATOS (Igual que siempre)
                // =============================================================
                tvEventTitle.text = item.summary
                val iconResId = context.getIconResId(item.categoryIcon)
                val colorResId = context.getColorResId(item.categoryColor)

                // 1. Configuramos el Icono
                ivCategoryIcon.setImageResource(iconResId)

                // 2. Configuramos el Fondo del Icono (Tint)
                ivCategoryIcon.backgroundTintList = ContextCompat.getColorStateList(context, colorResId)

                // Ubicación
                if (item.location.isNullOrEmpty()) {
                    tvEventLocation.visibility = View.GONE
                    iconLocation.visibility = View.GONE
                } else {
                    tvEventLocation.visibility = View.VISIBLE
                    iconLocation.visibility = View.VISIBLE
                    tvEventLocation.text = item.location
                }

                // Hora y Duración
                tvEventTime.text = item.start.toHourString()
                val durationInMinutes = getDurationInMinutes(item.start, item.end)
                if (durationInMinutes > 0) {
                    tvDuration.visibility = View.VISIBLE
                    iconRestant.visibility = View.VISIBLE
                    tvDuration.text = durationInMinutes.toDurationString()
                } else {
                    tvDuration.visibility = View.GONE
                    iconRestant.visibility = View.GONE
                }

                // Subtareas
                val hasSubtasks = item.subTasks.isNotEmpty()
                if (hasSubtasks) {
                    progressBarTaskDetailProgress.visibility = View.VISIBLE
                    textViewTaskDetailProgressText.visibility = View.VISIBLE
                    val total = item.subTasks.size
                    val completed = item.subTasks.count { it.isDone }
                    val progress = if (total > 0) (completed * 100) / total else 0
                    progressBarTaskDetailProgress.progress = progress
                    textViewTaskDetailProgressText.text = "$completed de $total completadas"
                } else {
                    progressBarTaskDetailProgress.visibility = View.INVISIBLE
                    textViewTaskDetailProgressText.visibility = View.INVISIBLE
                }

                // Colores y Estilos

                val priorityColorRes = when (item.priority) {
                    "Alta" -> R.color.priority_high
                    "Media" -> R.color.priority_medium
                    "Baja" -> R.color.priority_low
                    else -> R.color.priority_default
                }
                val priorityColor = ContextCompat.getColor(context, priorityColorRes)
                val typedValue = TypedValue()
                context.theme.resolveAttribute(R_Material.attr.colorSurfaceVariant, typedValue, true)

                val layerDrawable = binding.clTask.background.mutate() as LayerDrawable
                DrawableCompat.setTint(layerDrawable.findDrawableByLayerId(R.id.priority_bar_shape), priorityColor)
                DrawableCompat.setTint(layerDrawable.findDrawableByLayerId(R.id.content_background_shape), typedValue.data)

                // =============================================================
                // 2. CONFIGURACIÓN SEGURA DE ANIMACIÓN Y UI
                // =============================================================

                // A) Limpieza TOTAL antes de configurar (Evita crashes por listeners antiguos)
                swTaskCompleted.setOnCheckedChangeListener(null)
                lottieCelebration.removeAllAnimatorListeners()
                lottieCelebration.cancelAnimation() // Detener cualquier cosa pendiente

                // B) Estado visual inicial (según base de datos)
                swTaskCompleted.isChecked = item.isDone
                lottieCelebration.visibility = View.GONE

                // Tachado inicial
                if (item.isDone) {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvEventTitle.paintFlags = tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // C) LISTENER CON LÓGICA "PRIMERO UI, LUEGO DB"
                swTaskCompleted.setOnCheckedChangeListener { buttonView, isChecked ->

                    // 1. Modificaciones UI Inmediatas (Visuales)
                    if (isChecked) {
                        tvEventTitle.paintFlags = tvEventTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    } else {
                        tvEventTitle.paintFlags = tvEventTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }

                    // Usamos 'post' para evitar errores si el RecyclerView está calculando layout
                    binding.root.post {
                        if (isChecked) {
                            // CASO: MARCAR COMO HECHO (Animación -> Luego Guardar)

                            // Configurar Lottie
                            lottieCelebration.visibility = View.VISIBLE
                            lottieCelebration.speed = 1f
                            lottieCelebration.repeatCount = 0 // Solo una vez
                            lottieCelebration.progress = 0f   // Desde el inicio

                            lottieCelebration.removeAllAnimatorListeners()
                            lottieCelebration.addAnimatorListener(object : Animator.AnimatorListener {
                                override fun onAnimationStart(animation: Animator) {}

                                // AQUÍ ES DONDE GUARDAMOS: AL TERMINAR LA ANIMACIÓN
                                override fun onAnimationEnd(animation: Animator) {
                                    try {
                                        lottieCelebration.visibility = View.GONE
                                        // Solo guardamos si el usuario no cambió de opinión rápido
                                        if (swTaskCompleted.isChecked) {
                                            onTaskCheckedChange(item, true)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onAnimationCancel(animation: Animator) {
                                    lottieCelebration.visibility = View.GONE
                                }
                                override fun onAnimationRepeat(animation: Animator) {}
                            })

                            lottieCelebration.playAnimation()

                        } else {
                            // CASO: DESMARCAR (Guardar Inmediatamente)
                            lottieCelebration.cancelAnimation()
                            lottieCelebration.visibility = View.GONE
                            onTaskCheckedChange(item, false)
                        }
                    }
                }
            }
        }
    }
    private fun Context.getIconResId(resName: String): Int {
        val resId = resources.getIdentifier(resName, "drawable", packageName)
        return if (resId != 0) resId else R.drawable.ic_label // Default si falla
    }

    private fun Context.getColorResId(resName: String): Int {
        val resId = resources.getIdentifier(resName, "color", packageName)
        // Ojo: R.color.priority_default debe existir, o usa otro color seguro como R.color.black
        return if (resId != 0) resId else R.color.priority_default
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