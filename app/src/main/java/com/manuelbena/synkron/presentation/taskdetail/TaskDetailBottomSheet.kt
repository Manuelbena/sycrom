package com.manuelbena.synkron.presentation.taskdetail

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetTaskDetailBinding
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.TaskDomain

import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.getCategoryIcon
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toCalendar
import com.manuelbena.synkron.presentation.util.toDurationString
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.isNotEmpty

@AndroidEntryPoint
class TaskDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val taskId: Int by lazy { arguments?.getInt("TASK_ID") ?: 0 }
    private val viewModel: TaskDetailViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true // Opcional: si quieres que no tenga estado intermedio
                behavior.isDraggable = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getTask(taskId)
        setupListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.task.observe(viewLifecycleOwner) { task ->
            if (task != null) updateUI(task)
        }

        viewModel.shouldDismiss.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(task: TaskDomain) {
        binding.apply {
            // 1. Título y Estado (Lottie)
            tvTaskTitle.text = task.summary
            updateTitleStyle(task.isDone)

            // --- 2. SOLUCIÓN: Lottie Lógica Bidireccional ---
            // Quitamos el listener temporalmente para configurar el estado inicial sin disparar eventos
            lottieCompleteTask.setOnClickListener(null)

            // Si la animación NO está corriendo, forzamos el estado visual exacto
            if (!lottieCompleteTask.isAnimating) {
                lottieCompleteTask.progress = if (task.isDone) 1f else 0f
            }

            // Listener: Controla la animación visual Y el cambio de datos
            lottieCompleteTask.setOnClickListener {
                val newState = !task.isDone
                if (newState) {
                    // Animación normal (0 -> 1)
                    lottieCompleteTask.speed = 1.5f
                    lottieCompleteTask.playAnimation()
                } else {
                    // Animación inversa (1 -> 0) para desmarcar suavemente
                    lottieCompleteTask.speed = -1.5f
                    lottieCompleteTask.playAnimation()
                }
                viewModel.toggleTaskCompletion(newState)
            }

            // 2. PIN
            val pinIcon = if (task.isPinned) R.drawable.ic_mark_check else R.drawable.ic_mark
            btnPin.setImageResource(pinIcon)
            // Si quieres tintar el icono activo:
            if (task.isPinned) btnPin.setColorFilter(requireContext().getColor(R.color.md_theme_onPrimary))
            else btnPin.clearColorFilter() // O poner color por defecto

            // 3. Chips de Metadatos
            val startCal = task.start.toCalendar()
            chipDate.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(startCal.time)

            // PRIORIDAD: Color del punto y texto
            chipPriority.text = task.priority
            val priorityColor = getPriorityColor(task.priority)
            chipPriority.chipIconTint = ColorStateList.valueOf(priorityColor)

            // CATEGORIA: Icono y texto
            chipCategory.text = task.typeTask

            // Icono y Tint
            chipCategory.setChipIconResource(task.typeTask.getCategoryIcon())
            chipCategory.backgroundTintList = ContextCompat.getColorStateList(requireContext(), task.typeTask.getCategoryColor())



            // 4. Ubicación
            if (task.location.isNullOrEmpty()) {
                tvLocation.text = "Sin ubicación"
                // Opcional: layoutLocation.isVisible = false si prefieres ocultarlo
            } else {
                tvLocation.text = task.location
                layoutLocation.isVisible = true
            }

            // 5. Hora y Duración
            val startTime = task.start.toHourString()
            val endTime = task.end.toHourString()
            tvTimeRange.text = "$startTime - $endTime"

            val durationMin = getDurationInMinutes(task.start, task.end)
            if (durationMin > 0) {
                tvDurationText.isVisible = true
                tvDurationText.text = durationMin.toDurationString()
            } else {
                tvDurationText.isVisible = false
            }

            // 6. Descripción
            tvDescription.text = if (task.description.isNullOrBlank()) "Sin descripción" else task.description

            // 7. Configuración

            val hasReminders = task.reminders.overrides.isNotEmpty()


            // Pasamos el callback de borrado al adapter
            setupRemindersRecycler(task.reminders.overrides)

            val showSettingsSection =  hasReminders
            layoutSettings.isVisible = showSettingsSection

            // Truco visibilidad titulo
            try {
                val parentLayout = layoutSettings.parent as? ViewGroup
                if (parentLayout != null) {
                    val index = parentLayout.indexOfChild(layoutSettings)
                    if (index > 0) {
                        val labelView = parentLayout.getChildAt(index - 1)
                        if (labelView is TextView && labelView.text == "Avisos") {
                            labelView.isVisible = showSettingsSection
                        }
                    }
                }
            } catch (e: Exception) { }

            // 8. Subtareas
            renderSubtasks(task)
        }
    }

    private fun renderSubtasks(task: TaskDomain) {
        binding.containerSubtasks.removeAllViews()

        // Mostrar/Ocultar cabecera
        binding.tvSubtasksHeader.isVisible = task.subTasks.isNotEmpty()
        binding.dividerSubtask.isVisible = task.subTasks.isNotEmpty()


        task.subTasks.forEach { subTask ->
            val view = layoutInflater.inflate(R.layout.item_subtask, binding.containerSubtasks, false)
            val check = view.findViewById<CheckBox>(R.id.cbSubtask)
            val title = view.findViewById<TextView>(R.id.tvSubtaskTitle)

            title.text = subTask.title
            check.isChecked = subTask.isDone

            if (subTask.isDone) {
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.6f
            } else {
                title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
            }

            check.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleSubtaskCompletion(subTask, isChecked)
            }
            binding.containerSubtasks.addView(view)
        }
    }

    private fun setupRemindersRecycler(reminders: List<GoogleEventReminder>) {

        binding.dividerRecurrence.isVisible = reminders.isNotEmpty()
        binding.tvRecurrence.isVisible = reminders.isNotEmpty()


        if (reminders.isNotEmpty()) {

            binding.rvReminders.layoutManager = LinearLayoutManager(context)
            binding.rvReminders.adapter = ReminderSimpleAdapter(reminders) { reminder ->
                viewModel.deleteReminder(reminder)
            }
        } else {
            binding.rvReminders.isVisible = false
        }
    }

    private fun updateTitleStyle(isDone: Boolean) {
        binding.tvTaskTitle.apply {
            if (isDone) {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                alpha = 0.6f
            } else {
                paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                alpha = 1f
            }
        }
    }

    private fun getPriorityColor(priority: String): Int {
        return when (priority.lowercase(Locale.ROOT)) {
            "alta", "high" -> Color.parseColor("#F44336") // Rojo Material
            "media", "medium" -> Color.parseColor("#FF9800") // Naranja
            "baja", "low" -> Color.parseColor("#4CAF50") // Verde
            else -> Color.GRAY
        }
    }


    private fun setupListeners() {
        binding.btnPin.setOnClickListener { viewModel.togglePin() }

        binding.btnEdit.setOnClickListener {
            viewModel.task.value?.let { task ->
                val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
                    putExtra(TASK_TO_EDIT_KEY, task)
                }
                startActivity(intent)
                dismiss()
            }
        }

        binding.btnDelete.setOnClickListener {
            viewModel.task.value?.let { task -> viewModel.deleteTask(task) }
        }

        binding.btnShare.setOnClickListener {
            viewModel.task.value?.let { task -> shareTask(task) }
        }
    }


    private fun shareTask(task: TaskDomain) {
        val shareText = generateShareText(task)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
    }

    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()
        val startDateCalendar = task.start.toCalendar()
        val durationInMinutes = getDurationInMinutes(task.start, task.end)

        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.summary.uppercase()}*\n\n")

        val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(startDateCalendar.time)
        builder.append("🗓️ *Cuándo:* ${dateText.replaceFirstChar { it.titlecase(Locale.getDefault()) }}\n")
        builder.append("⏰ *Hora:* ${task.start.toHourString()}\n")

        if (durationInMinutes > 0) {
            builder.append("⏳ *Duración:* ${durationInMinutes.toDurationString()}\n")
        }
        if (!task.location.isNullOrEmpty()) {
            builder.append("📍 *Lugar:* ${task.location}\n")
        }
        if (task.typeTask.isNotEmpty()) {
            builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
        }
        builder.append("\n")

        if (!task.description.isNullOrEmpty()) {
            builder.append("🧐 *El plan:*\n")
            builder.append("${task.description}\n\n")
        }

        if (task.subTasks.isNotEmpty()) {
            builder.append("📋 *Los pasos a seguir:*\n")
            task.subTasks.forEach { subtask ->
                val subtaskStatus = if (subtask.isDone) "✔️" else "◻️"
                builder.append("   $subtaskStatus ${subtask.title}\n")
            }
            builder.append("\n")
        }
        try {
            val calendarLink = createGoogleCalendarLink(task)
            builder.append("➕ *¡Añádelo a tu calendario!:*\n")
            builder.append("$calendarLink\n\n")
        } catch (e: Exception) {}

        builder.append("--------------------------------\n")
        builder.append("¡Gestionando mi caos con *Synkrón*! 🚀")

        return builder.toString()
    }

    private fun createGoogleCalendarLink(task: TaskDomain): String {
        val startCalendar = task.start.toCalendar()
        val endCalendar = task.end.toCalendar()

        val startTimeMillis = startCalendar.timeInMillis
        val endTimeMillis = endCalendar.timeInMillis

        val isoFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startTimeUtc = isoFormatter.format(Date(startTimeMillis))
        val endTimeUtc = isoFormatter.format(Date(endTimeMillis))

        val title = URLEncoder.encode(task.summary, "UTF-8")
        val dates = URLEncoder.encode("$startTimeUtc/$endTimeUtc", "UTF-8")
        val details = URLEncoder.encode(task.description ?: "", "UTF-8")
        val location = URLEncoder.encode(task.location ?: "", "UTF-8")

        return "https://www.google.com/calendar/render?action=TEMPLATE" +
                "&text=$title" +
                "&dates=$dates" +
                "&details=$details" +
                "&location=$location"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskDetailBottomSheet"
        fun newInstance(task: TaskDomain) = TaskDetailBottomSheet().apply {
            arguments = bundleOf("TASK_ID" to task.id)
        }
    }

    // Adaptador interno para Avisos (Sin crear archivo extra)
    inner class ReminderSimpleAdapter(
        private val items: List<GoogleEventReminder>,
        private val onDeleteClick: (GoogleEventReminder) -> Unit
    ) : RecyclerView.Adapter<ReminderSimpleAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvType: TextView = view.findViewById(R.id.tvTypeLabel)
            val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Usamos TU layout 'item_reminder_row'
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val timeText = when {
                item.minutes < 60 -> "${item.minutes} min antes"
                item.minutes == 60 -> "1 hora antes"
                item.minutes < 1440 -> "${item.minutes / 60} horas antes"
                else -> "${item.minutes / 1440} días antes"
            }
            holder.tvTime.text = timeText
            holder.tvType.text = "Notificación (${item.method})" // O solo "Notificación"

            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount() = items.size
    }
}