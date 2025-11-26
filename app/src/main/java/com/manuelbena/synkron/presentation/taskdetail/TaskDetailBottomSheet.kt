package com.manuelbena.synkron.presentation.taskdetail


import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetTaskDetailBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY

import com.manuelbena.synkron.presentation.util.TaskDetailContract
import com.manuelbena.synkron.presentation.util.TaskDetailViewModel
import com.manuelbena.synkron.presentation.util.adapters.SubtaskAdapter

import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toCalendar
import com.manuelbena.synkron.presentation.util.toDurationString
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class TaskDetailBottomSheet : BottomSheetDialogFragment() {

    // --- Propiedades ---

    private val viewModel: TaskDetailViewModel by viewModels()
    private var _binding: BottomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var subtaskAdapter: SubtaskAdapter

    // --- Ciclo de Vida ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Configuración ---

    private fun setupUI() {
        subtaskAdapter = SubtaskAdapter { subtask, isDone ->
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnSubtaskChecked(subtask, isDone))
        }

        binding.recyclerViewTaskDetailSubtasks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subtaskAdapter
        }
    }

    private fun setupListeners() {
        // 1. Listeners del Toggle de Estado
        binding.toggleButtonStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isDone = (checkedId == binding.buttonStatusDone.id)
                viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnTaskChecked(isDone))

            }
        }

        // 2. Listener del FAB (Borrar)
        binding.fabDelete.setOnClickListener {
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnDeleteClicked)
        }

        // 3. Listeners del BottomAppBar (Compartir)
        binding.bottomAppBar.setNavigationOnClickListener {
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnShareClicked)
        }

        // 4. Listener del botón 'X'
        binding.buttonCloseSheet.setOnClickListener {
            dismiss()
        }

        // 5. Listener del Menú (Editar)
        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // ¡ID CORREGIDO!
                R.id.menu_edit_task -> {
                    viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnEditClicked)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        // Observa el ESTADO (los datos a pintar)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Se llama cada vez que el ViewModel actualiza la Tarea
            bindTaskData(state.task)
        }

        // Observa las ACCIONES (eventos de un solo uso)
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is TaskDetailContract.TaskDetailAction.NavigateToEdit -> {
                    navigateToContainerActivity(action.task)
                    dismiss()
                }
                is TaskDetailContract.TaskDetailAction.ShareTask -> {
                    shareTask(action.task)
                }
                is TaskDetailContract.TaskDetailAction.DismissSheet -> {
                    dismiss()
                }
            }
        }
    }

    // --- Métodos de UI ---

    /**
     * Rellena todas las vistas con los datos de la [TaskDomain].
     * (¡COMPLETAMENTE ACTUALIZADO A TaskDomain NUEVO!)
     */
    private fun bindTaskData(task: TaskDomain) {

        // 1. Sincronizar el Toggle de Estado
        if (task.isDone) {
            binding.toggleButtonStatus.check(R.id.button_status_done)
        } else {
            binding.toggleButtonStatus.check(R.id.button_status_pending)
        }

        // 2. Título y Tachado (CAMBIO: summary)
        binding.textViewTaskDetailTitle.text = task.summary
        updateTitleStrikeThrough(task.isDone)

        // 3. Rellenar los Chips de Contexto (CAMBIOS)
        val startDateCalendar = task.start.toCalendar() // Helper para obtener un Calendar
        val durationInMinutes = getDurationInMinutes(task.start, task.end) // Helper

        binding.chipContextDate.text = SimpleDateFormat("dd MMMM", Locale.getDefault()).format(startDateCalendar.time)
        binding.chipContextTime.text = task.start.toHourString()
        binding.chipContextDuration.text = durationInMinutes.toDurationString()

        // CAMBIO: location
        binding.chipContextLocation.isVisible = !task.location.isNullOrEmpty()
        binding.chipContextLocation.text = task.location

        // CAMBIO: priority
        binding.chipContextPriority.isVisible = task.priority.isNotEmpty()
        binding.chipContextPriority.text = "Prioridad ${task.priority}"

        // 4. Descripción
        binding.textViewTaskDetailDescription.text = task.description.takeIf { !it.isNullOrEmpty() } ?: "Sin descripción"

        // 5. Progreso de Subtareas
        val totalSubtasks = task.subTasks.size
        val completedSubtasks = task.subTasks.count { it.isDone }
        val hasSubtasks = totalSubtasks > 0

        binding.layoutSubtasks.isVisible = hasSubtasks
        if (hasSubtasks) {
            val progress = (completedSubtasks * 100) / totalSubtasks
            binding.progressBarTaskDetailProgress.progress = progress
            binding.textViewTaskDetailProgressText.text = "$completedSubtasks de $totalSubtasks completadas"
            subtaskAdapter.submitList(task.subTasks)
        }
    }

    private fun updateTitleStrikeThrough(isDone: Boolean) {
        if (isDone) {
            binding.textViewTaskDetailTitle.paintFlags =
                binding.textViewTaskDetailTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.textViewTaskDetailTitle.paintFlags =
                binding.textViewTaskDetailTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    // --- Métodos de Navegación/Acción (ACTUALIZADOS) ---

    /**
     * Crea un enlace universal de "Añadir a Google Calendar" a partir de una tarea.
     * (¡COMPLETAMENTE ACTUALIZADO A TaskDomain NUEVO!)
     */
    private fun createGoogleCalendarLink(task: TaskDomain): String {
        // 1. Calcular las fechas de inicio y fin (CAMBIO)
        val startCalendar = task.start.toCalendar()
        val endCalendar = task.end.toCalendar()

        val startTimeMillis = startCalendar.timeInMillis
        val endTimeMillis = endCalendar.timeInMillis

        // 2. Formatear las fechas a ISO 8601 en UTC
        val isoFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startTimeUtc = isoFormatter.format(Date(startTimeMillis))
        val endTimeUtc = isoFormatter.format(Date(endTimeMillis))

        // 3. Codificar los parámetros (CAMBIO)
        val title = URLEncoder.encode(task.summary, "UTF-8")
        val dates = URLEncoder.encode("$startTimeUtc/$endTimeUtc", "UTF-8")
        val details = URLEncoder.encode(task.description ?: "", "UTF-8")
        val location = URLEncoder.encode(task.location ?: "", "UTF-8")

        // 4. Construir la URL final
        return "https://www.google.com/calendar/render?action=TEMPLATE" +
                "&text=$title" +
                "&dates=$dates" +
                "&details=$details" +
                "&location=$location"
    }

    private fun navigateToContainerActivity(task: TaskDomain) {
        val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
            putExtra(TASK_TO_EDIT_KEY, task)
        }
        startActivity(intent)
    }

    private fun shareTask(task: TaskDomain) {
        val shareText = generateShareText(task)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND // CAMBIO: 'action' en minúscula
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain" // CAMBIO: 'type' en minúscula
        }
         sendIntent.setPackage("com.whatsapp") // (Opcional)

        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
    }

    /**
     * Genera el texto formateado con emojis para compartir.
     * (¡COMPLETAMENTE ACTUALIZADO A TaskDomain NUEVO!)
     */
    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()
        val startDateCalendar = task.start.toCalendar()
        val durationInMinutes = getDurationInMinutes(task.start, task.end)

        // --- Encabezado y Título ---
        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.summary.uppercase()}*\n\n") // CAMBIO

        // --- Contexto (Fecha, Hora, Lugar...) ---
        val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(startDateCalendar.time)
        builder.append("🗓️ *Cuándo:* ${dateText.replaceFirstChar { it.titlecase(Locale.getDefault()) }}\n")
        builder.append("⏰ *Hora:* ${task.start.toHourString()}\n") // CAMBIO

        if (durationInMinutes > 0) { // CAMBIO
            builder.append("⏳ *Duración:* ${durationInMinutes.toDurationString()}\n")
        }
        if (!task.location.isNullOrEmpty()) { // CAMBIO
            builder.append("📍 *Lugar:* ${task.location}\n")
        }
        if (task.typeTask.isNotEmpty()) {
            builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
        }
        builder.append("\n") // Separador

        // --- Descripción (si existe) ---
        if (!task.description.isNullOrEmpty()) { // CAMBIO
            builder.append("🧐 *El plan:*\n")
            builder.append("${task.description}\n\n")
        }

        // --- Subtareas (si existen) ---
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

        // --- Footer de Synkrón ---
        builder.append("--------------------------------\n")
        builder.append("¡Gestionando mi caos con *Synkrón*! 🚀")

        return builder.toString()
    }

    // --- Companion Object ---

    companion object {
        const val TAG = "TaskDetailBottomSheet"

        fun newInstance(task: TaskDomain): TaskDetailBottomSheet {
            return TaskDetailBottomSheet().apply {
                arguments = bundleOf("task" to task)
            }
        }
    }
}