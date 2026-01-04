package com.manuelbena.synkron.presentation.taskdetail

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
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
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.TaskBottomSheet
import com.manuelbena.synkron.presentation.util.*
import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class TaskDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskDetailViewModel by viewModels()

    // --- CICLO DE VIDA ---

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SYNKRON_DEBUG", "🟢 1. onViewCreated iniciado")

        // 1. Recuperar la tarea de los argumentos
        val taskArg: TaskDomain? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_TASK_OBJ, TaskDomain::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_TASK_OBJ)
        }

        // 2. Diagnóstico del argumento
        if (taskArg != null) {
            Log.d("SYNKRON_DEBUG", "🟢 2. Tarea recibida en argumentos: ${taskArg.summary} (ID: ${taskArg.id})")

            // --- PASO CRÍTICO QUE FALTABA ---
            // Forzamos la actualización del ViewModel aquí mismo
            Log.d("SYNKRON_DEBUG", "🟢 3. Enviando tarea al ViewModel...")
            viewModel.updateTask(taskArg)
        } else {
            Log.e("SYNKRON_DEBUG", "🔴 2. ERROR: Los argumentos llegaron NULL o vacíos.")
        }
        setupRecurrenceListeners()
        setupListeners()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- CONFIGURACIÓN Y OBSERVERS ---

    private fun setupObservers() {
        viewModel.task.observe(viewLifecycleOwner) { task ->
            Log.d("SYNKRON_DEBUG", "🟡 4. Observer disparado. ¿Task es null?: ${task == null}")

            if (task != null) {
                try {
                    Log.d("SYNKRON_DEBUG", "🟡 5. Llamando a updateUI con: ${task.summary}")
                    updateUI(task)
                    Log.d("SYNKRON_DEBUG", "✅ 6. UI actualizada correctamente (supuestamente)")
                } catch (e: Exception) {
                    Log.e("SYNKRON_DEBUG", "🔴 ERROR FATAL en updateUI: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        viewModel.shouldDismiss.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) dismiss()
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnClose.setOnClickListener { dismiss() }

            swTaskCompleted.setOnCheckedChangeListener { view, isChecked ->
                if (view.isPressed) {
                    viewModel.task.value?.let { currentTask ->
                        viewModel.updateTask(currentTask.copy(isDone = isChecked))
                    }
                }
            }

            btnPin.setOnClickListener { viewModel.togglePin() }

            btnEdit.setOnClickListener {
                viewModel.task.value?.let { task ->
                    showTaskBottomSheet(task)
                }
            }

            btnDelete.setOnClickListener {
                viewModel.task.value?.let { task ->
                    confirmDeletion(task)
                }
            }

            btnShare.setOnClickListener {
                viewModel.task.value?.let { task -> shareTask(task) }
            }
        }
    }

    // --- LÓGICA DE UI PRINCIPAL ---

    private fun updateUI(task: TaskDomain) {
        try {
            val context = requireContext()

            setupHeader(task)
            setupTaskTitleAndStatus(task)

            // --- LOGICA SEGURA PARA FECHAS ---
            Log.d("SYNKRON_DEBUG", "🔍 Procesando fecha. Start: ${task.start}")
            setupTimeAndDate(task)

            setupRecurrenceView(task, context)
            setupTags(task, context)
            setupDetailsCard(task)
            setupSubtasks(task)
            setupReminders(task)

        } catch (e: Exception) {
            Log.e("SYNKRON_DEBUG", "🔴 Excepción pintando la UI: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showTaskBottomSheet(task: TaskDomain?) {
        val bottomSheet = TaskBottomSheet.newInstance(task)
        bottomSheet.show(childFragmentManager, "TaskBottomSheet")
    }

    private fun setupHeader(task: TaskDomain) {
        val gradientDrawable = task.typeTask.getCategoryGradientDrawable()
        binding.headerContainer.setBackgroundResource(gradientDrawable)

        // Estado del Pin
        val pinIcon = if (task.isPinned) R.drawable.ic_mark_check else R.drawable.ic_mark
        binding.btnPin.setImageResource(pinIcon)
        if (task.isPinned) binding.btnPin.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimary))
        else binding.btnPin.clearColorFilter()
    }

    private fun setupTaskTitleAndStatus(task: TaskDomain) {
        binding.tvTaskTitle.text = task.summary
        binding.swTaskCompleted.isChecked = task.isDone

        if (task.isDone) {
            binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvTaskTitle.alpha = 0.6f
        } else {
            binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.tvTaskTitle.alpha = 1f
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupTimeAndDate(task: TaskDomain) {
        val startDate = task.start

        // Fecha
        val dateText = if (startDate?.dateTime == null) {
            // Tarea de todo el día: Usar startDate.date o fallback
            "${startDate?.date ?: ""} • Todo el día"
        } else {
            // Tarea con hora
            val calendar = startDate.toCalendar()
            val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
            val dateStr = calendar?.let { dateFormat.format(it.time) } ?: ""
            "$dateStr • ${startDate.toHourString()}"
        }
        binding.tvDateFull.text = dateText

        // Duración (Solo calcular si NO es todo el día para evitar líos o 0 min)
        if (startDate?.dateTime != null) {
            val durationMin = getDurationInMinutes(startDate, task.end)
            if (durationMin > 0) {
                binding.tvDurationText.isVisible = true
                binding.tvDurationText.text = durationMin.toDurationString()
            } else {
                binding.tvDurationText.isVisible = false
            }
        } else {
            // Ocultar duración en tareas de todo el día
            binding.tvDurationText.isVisible = false
        }
    }

    private fun setupTags(task: TaskDomain, context: Context) {
        val iconSize = dpToPx(14)

        // Categoría
        val catName = task.typeTask.ifEmpty { "General" }
        val catColor = ContextCompat.getColor(context, task.typeTask.getCategoryColor())
        val catIcon = ContextCompat.getDrawable(context, task.typeTask.getCategoryIcon())

        binding.chipCategory.apply {
            text = catName.getName()
            setTextColor(catColor)
            background.setTint(catColor)
            background.alpha = 30
            catIcon?.apply {
                setTint(catColor)
                setBounds(0, 0, iconSize, iconSize)
            }
            setCompoundDrawables(catIcon, null, null, null)
        }

        // Prioridad
        if (task.priority.isNotEmpty()) {
            binding.chipPriority.isVisible = true
            val prioColor = getPriorityColor(task.priority)
            val prioIcon = ContextCompat.getDrawable(context, R.drawable.ic_mark) // O ic_flag

            binding.chipPriority.apply {
                text = task.priority
                setTextColor(prioColor)
                background.setTint(prioColor)
                background.alpha = 30
                prioIcon?.apply {
                    setTint(prioColor)
                    setBounds(0, 0, iconSize, iconSize)
                }
                setCompoundDrawables(prioIcon, null, null, null)
            }
        } else {
            binding.chipPriority.isVisible = false
        }
    }

    private fun setupDetailsCard(task: TaskDomain) {
        val hasDesc = !task.description.isNullOrBlank()
        val hasLoc = !task.location.isNullOrBlank()
        val hasLink = !task.conferenceLink.isNullOrBlank()

        if (hasDesc || hasLoc || hasLink) {
            binding.cardDetails.isVisible = true
            binding.tvDescription.isVisible = hasDesc
            binding.tvDescription.text = task.description ?: "Sin descripción"

            binding.tvLocation.isVisible = hasLoc
            binding.tvLocation.text = task.location

            binding.tvLink.isVisible = hasLink
            binding.tvLink.text = task.conferenceLink ?: "Enlace disponible"
        } else {
            binding.cardDetails.isVisible = false
        }
    }

    // --- SECCIONES COMPLEJAS (Recurrencia, Subtareas, Avisos) ---
    private fun setupRecurrenceView(task: TaskDomain, context: Context) {
        // 1. Verificar si hay días configurados
        val recurrenceDays = task.synkronRecurrenceDays
        val isRecurring = recurrenceDays.isNotEmpty()

        if (!isRecurring) {
            binding.layoutRecurrence.isVisible = false
            return
        }

        binding.layoutRecurrence.isVisible = true


        // 3. Configuración de colores (Igual que antes)
        val activeColor = ContextCompat.getColor(context, task.typeTask.getCategoryColor())
        val inactiveBgColor = ContextCompat.getColor(context, R.color.slate_200)
        val activeTextColor = Color.WHITE
        val inactiveTextColor = ContextCompat.getColor(context, R.color.secondary_text)

        val dayViews = listOf(
            binding.dayMon, binding.dayTue, binding.dayWed, binding.dayThu,
            binding.dayFri, binding.daySat, binding.daySun
        )

        dayViews.forEachIndexed { index, textView ->
            // Bloqueamos clicks (Modo solo lectura)
            textView.isClickable = false
            textView.isFocusable = false

            val dayNumber = index + 1 // Lunes=1 ... Domingo=7
            val isActive = recurrenceDays.contains(dayNumber)

            if (isActive) {
                textView.background.setTint(activeColor)
                textView.setTextColor(activeTextColor)
                textView.setTypeface(null, Typeface.BOLD)
            } else {
                textView.background.setTint(inactiveBgColor)
                textView.setTextColor(inactiveTextColor)
                textView.setTypeface(null, Typeface.NORMAL)
            }
        }
    }
    private fun setupSubtasks(task: TaskDomain) {
        if (task.subTasks.isNotEmpty()) {
            binding.cardSubtasks.isVisible = true
            binding.rvSubtasks.layoutManager = LinearLayoutManager(requireContext())
            binding.rvSubtasks.adapter = SubtaskDetailAdapter(task.subTasks) { subTask, isChecked ->
                viewModel.toggleSubtaskCompletion(subTask, isChecked)
            }
        } else {
            binding.cardSubtasks.isVisible = false
        }
    }

    private fun setupReminders(task: TaskDomain) {
        val reminders = task.reminders.overrides ?: emptyList()

        if (reminders.isNotEmpty()) {
            binding.cardSettings.isVisible = true

            val adapter = ReminderManagerAdapter { reminderItem ->
                val originalReminder = reminders.find { it.minutes == reminderItem.offsetMinutes }
                if (originalReminder != null) {
                    viewModel.deleteReminder(originalReminder)
                }
            }

            binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
            binding.rvReminders.adapter = adapter
            adapter.submitList(mapToReminderItems(reminders, task.start))
        } else {
            binding.cardSettings.isVisible = false
        }
    }

    // --- HELPERS Y UTILS ---

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun getPriorityColor(priority: String): Int {
        return when (priority.lowercase(Locale.ROOT)) {
            "alta", "high" -> Color.parseColor("#F44336")
            "media", "medium" -> Color.parseColor("#FF9800")
            "baja", "low" -> Color.parseColor("#4CAF50")
            else -> Color.GRAY
        }
    }

    private fun mapToReminderItems(
        reminders: List<GoogleEventReminder>,
        taskStartDate: com.manuelbena.synkron.domain.models.GoogleEventDateTime?
    ): List<ReminderItem> {
        val startTime = taskStartDate?.toCalendar()?.timeInMillis ?: System.currentTimeMillis()

        return reminders.mapIndexed { index, googleReminder ->
            // ... (cálculo de hora igual que antes) ...
            val reminderTimeMillis = startTime - (googleReminder.minutes * 60 * 1000)
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = reminderTimeMillis
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            // --- AQUÍ ESTÁ LA CLAVE ---
            // Asegúrate de que estos textos coinciden con lo que guardas en la BD
            val (methodType, titleText) = when (googleReminder.method.lowercase()) {
                "alarm", "alarma" -> Pair(ReminderMethod.ALARM, "Alarma")
                "whatsapp", "share" -> Pair(ReminderMethod.WHATSAPP, "WhatsApp")
                // "popup" es el estándar de Google Calendar para notificaciones móviles
                "popup", "notification" -> Pair(ReminderMethod.NOTIFICATION, "Notificación")
                // Caso por defecto
                else -> Pair(ReminderMethod.NOTIFICATION, "Notificación")
            }

            ReminderItem(
                id = index.toString(),
                displayTime = timeFormat.format(cal.time),
                message = titleText, // Esto pondrá "Alarma" o "WhatsApp" en el texto
                method = methodType,
                offsetMinutes = googleReminder.minutes,
                hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                minute = cal.get(java.util.Calendar.MINUTE)
            )
        }
    }

    // --- LÓGICA DE COMPARTIR ---

    private fun shareTask(task: TaskDomain) {
        try {
            val shareText = generateShareText(task)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()
        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.summary.uppercase()}*\n\n")

        val startCal = task.start?.toCalendar()
        if (startCal != null) {
            val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(startCal.time)
            builder.append("🗓️ *Cuándo:* ${dateText.replaceFirstChar { it.titlecase(Locale.getDefault()) }}\n")
            if (task.start.dateTime != null) {
                builder.append("⏰ *Hora:* ${task.start.toHourString()}\n")
            }
        }

        if (!task.location.isNullOrEmpty()) {
            builder.append("📍 *Lugar:* ${task.location}\n")
        }

        if (!task.description.isNullOrEmpty()) {
            builder.append("\n🧐 *El plan:*\n${task.description}\n")
        }

        if (task.subTasks.isNotEmpty()) {
            builder.append("\n📋 *Pasos:*\n")
            task.subTasks.forEach {
                builder.append("   ${if (it.isDone) "✔️" else "◻️"} ${it.title}\n")
            }
        }

        builder.append("\n--------------------------------\n¡Gestionando mi caos con *Synkrón*! 🚀")
        return builder.toString()
    }

    // --- COMPANION & ADAPTERS INTERNOS ---

    companion object {
        const val ARG_TASK_OBJ = "arg_task_obj"
        fun newInstance(task: TaskDomain) = TaskDetailBottomSheet().apply {
            arguments = bundleOf(ARG_TASK_OBJ to task)
        }
    }

    inner class SubtaskDetailAdapter(
        private val items: List<SubTaskDomain>,
        private val onCheckChange: (SubTaskDomain, Boolean) -> Unit
    ) : RecyclerView.Adapter<SubtaskDetailAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.cbSubtask)
            val title: TextView = view.findViewById(R.id.tvSubtaskTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title

            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = item.isDone

            if (item.isDone) {
                holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.title.alpha = 0.6f
            } else {
                holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.title.alpha = 1f
            }

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckChange(item, isChecked)
            }
        }

        override fun getItemCount() = items.size
    }

    private fun setupRecurrenceListeners() {
        // Mapeamos cada TextView a su número de día (Lunes=1 ... Domingo=7)
        val dayMap = mapOf(
            binding.dayMon to 1,
            binding.dayTue to 2,
            binding.dayWed to 3,
            binding.dayThu to 4,
            binding.dayFri to 5,
            binding.daySat to 6,
            binding.daySun to 7
        )

        dayMap.forEach { (textView, dayIndex) ->
            textView.setOnClickListener {
                // 1. Obtenemos la tarea actual
                val currentTask = viewModel.task.value ?: return@setOnClickListener

                // 2. Creamos una lista mutable nueva basada en la actual
                val currentDays = currentTask.synkronRecurrenceDays.toMutableList()

                // 3. Lógica de "Toggle" (Si está, lo quito. Si no está, lo pongo)
                if (currentDays.contains(dayIndex)) {
                    currentDays.remove(dayIndex)
                } else {
                    currentDays.add(dayIndex)
                }

                // 4. Actualizamos el ViewModel inmediatamente
                // Esto dispara el observer -> llama a updateUI -> repinta los colores
                viewModel.updateTask(currentTask.copy(synkronRecurrenceDays = currentDays))
            }
        }
    }

    // --- LÓGICA DE DIÁLOGOS ---

    private fun confirmDeletion(task: TaskDomain) {
        // Detectamos si es parte de una serie mirando el parentId
        val isRecurringSeries = !task.parentId.isNullOrEmpty()

        if (isRecurringSeries) {
            showRecurringDeleteDialog(task)
        } else {
            showSimpleDeleteDialog(task)
        }
    }

    private fun showSimpleDeleteDialog(task: TaskDomain) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("¿Borrar tarea?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Borrar") { _, _ ->
                // Usamos deleteInstance por defecto para tareas simples
                viewModel.deleteTaskInstance(task)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRecurringDeleteDialog(task: TaskDomain) {
        val options = arrayOf("Solo este evento", "Este y los futuros (Serie)")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Evento recurrente")
            .setSingleChoiceItems(options, -1) { dialog, which ->
                when (which) {
                    0 -> { // Solo este
                        viewModel.deleteTaskInstance(task)
                        dialog.dismiss()
                    }
                    1 -> { // Serie completa
                        viewModel.deleteTaskSeries(task)
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()



    }
}