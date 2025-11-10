package com.manuelbena.synkron.presentation.util

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.util.TimeUtils.formatDuration
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetTaskDetailBinding // <-- IMPORTANTE: El binding cambiará de nombre
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.util.adapters.SubtaskAdapter
import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import com.manuelbena.synkron.presentation.util.extensions.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BottomSheet para mostrar el detalle de una Tarea.
 *
 * Responsabilidades:
 * - Mostrar los detalles de la Tarea (título, desc, subtareas).
 * - Permitir al usuario marcar la tarea o subtareas como completadas.
 * - Enviar eventos de acciones (Editar, Borrar, Compartir) al [TaskDetailViewModel].
 * - Reaccionar a las acciones del ViewModel (Navegar, Cerrar).
 */
@AndroidEntryPoint
class TaskDetailBottomSheet : BottomSheetDialogFragment() {

    // --- Propiedades ---

    // Inyectamos el ViewModel dedicado
    private val viewModel: TaskDetailViewModel by viewModels()

    // Usamos el nombre de binding generado por el XML renombrado
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

    /**
     * Inicializa los componentes de la UI (principalmente el RecyclerView).
     */
    private fun setupUI() {
        subtaskAdapter = SubtaskAdapter { subtask, isDone ->
            // Envía el evento al ViewModel
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnSubtaskChecked(subtask, isDone))
        }

        binding.recyclerViewTaskDetailSubtasks.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subtaskAdapter
        }
    }

    /**
     * Configura los listeners de los botones.
     */
    private fun setupListeners() {
        // 1. Listeners del Toggle de Estado
        binding.toggleButtonStatus.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val isDone = (checkedId == binding.buttonStatusDone.id)
                viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnTaskChecked(isDone))
                updateTitleStrikeThrough(isDone) // Actualización visual inmediata
            }
        }

        // 2. Listener del FAB (Borrar)
        binding.fabDelete.setOnClickListener {
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnDeleteClicked)
        }

        // 3. Listeners del BottomAppBar (Compartir y Editar)
        binding.bottomAppBar.setNavigationOnClickListener {
            // Ícono de Navegación (Compartir)
            viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnShareClicked)
        }

        binding.buttonCloseSheet.setOnClickListener {
            dismiss() // Simplemente cierra el BottomSheet
        }

        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    viewModel.onEvent(TaskDetailContract.TaskDetailEvent.OnEditClicked)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Observa el Estado (State) y las Acciones (Action) del ViewModel.
     */
    private fun observeViewModel() {
        // Observa el ESTADO (los datos a pintar)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            bindTaskData(state.task)
        }

        // Observa las ACCIONES (eventos de un solo uso)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.action.observe(viewLifecycleOwner) { action ->
                when (action) {
                    is TaskDetailContract.TaskDetailAction.NavigateToEdit -> {
                        navigateToContainerActivity(action.task)
                        dismiss() // Cierra el sheet después de navegar
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
    }

    // --- Métodos de UI ---

    /**
     * Rellena todas las vistas con los datos de la [TaskDomain].
     */
    private fun bindTaskData(task: TaskDomain) {

        // 1. Sincronizar el Toggle de Estado
        if (task.isDone) {
            binding.toggleButtonStatus.check(R.id.button_status_done)
        } else {
            binding.toggleButtonStatus.check(R.id.button_status_pending)
        }

        // 2. Título y Tachado
        binding.textViewTaskDetailTitle.text = task.title
        updateTitleStrikeThrough(task.isDone)

        // 3. Rellenar los Chips de Contexto
        binding.chipContextDate.text = SimpleDateFormat("dd MMMM", Locale.getDefault()).format(
            Date(
                task.date
            )
        )
        binding.chipContextTime.text = task.hour.toHourString()
        binding.chipContextDuration.text = task.duration.toDurationString()

        // Ocultar chips si no hay info
        binding.chipContextLocation.isVisible = task.place.isNotEmpty()
        binding.chipContextLocation.text = task.place

        binding.chipContextPriority.isVisible = task.priority.isNotEmpty()
        binding.chipContextPriority.text = task.priority


        // 4. Descripción
        binding.textViewTaskDetailDescription.text = task.description

        binding.textViewTaskDetailDescription.text = task.description.ifEmpty { "Sin descripción" }


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
    /**
     * Aplica o quita el tachado del título.
     */
    private fun updateTitleStrikeThrough(isDone: Boolean) {
        if (isDone) {
            binding.textViewTaskDetailTitle.paintFlags =
                binding.textViewTaskDetailTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.textViewTaskDetailTitle.paintFlags =
                binding.textViewTaskDetailTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    // --- Métodos de Navegación/Acción ---

    /**
     * Navega a [ContainerActivity] para editar la tarea.
     */
    private fun navigateToContainerActivity(task: TaskDomain) {
        val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
            putExtra(EDIT_TASK, task) // Usamos la constante
        }
        startActivity(intent)
    }

    /**
     * Crea un Intent para compartir el contenido de una tarea.
     */
    private fun shareTask(task: TaskDomain) {
        val shareText = generateShareText(task)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        // WhatsApp (si está instalado)
        sendIntent.setPackage("com.whatsapp") // Descomenta esto si quieres ir directo a WhatsApp

        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
    }

    /**
     * Genera el texto formateado con emojis para compartir.
     */
    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()

        // --- Encabezado y Título ---
        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.title.uppercase()}*\n\n")

        // --- Contexto (Fecha, Hora, Lugar...) ---
        val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale.getDefault()).format(Date(task.date))
        builder.append("🗓️ *Cuándo:* ${dateText.capitalize(Locale.getDefault())}\n")
        builder.append("⏰ *Hora:* ${task.hour.toHourString()}\n")

        if (task.duration > 0) {
            builder.append("⏳ *Duración:* ${task.duration.toDurationString()}\n")
        }
        if (task.place.isNotEmpty()) {
            builder.append("📍 *Lugar:* ${task.place}\n")
        }
        if (task.typeTask.isNotEmpty()) {
            builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
        }
        builder.append("\n") // Separador

        // --- Descripción (si existe) ---
        if (task.description.isNotEmpty()) {
            builder.append("🧐 *El plan:*\n")
            builder.append("${task.description}\n\n")
        }

        // --- Subtareas (si existen) ---
        if (task.subTasks.isNotEmpty()) {
            builder.append("📋 *Los pasos a seguir:*\n")
            task.subTasks.forEach { subtask ->
                val subtaskStatus = if (subtask.isDone) "✔️" else "◻️" // O "🔘"
                builder.append("   $subtaskStatus ${subtask.title}\n")
            }
            builder.append("\n")
        }

        // --- Footer de Synkrón ---
        builder.append("--------------------\n")
        builder.append("¡Gestionando mi caos con *Synkrón*! 🚀")

        return builder.toString()
    }

    // --- Companion Object ---

    companion object {
        const val TAG = "TaskDetailBottomSheet"

        /**
         * Método factoría para crear una nueva instancia de este BottomSheet.
         * Pasamos la Tarea completa como argumento Parcelable.
         *
         * @param task La Tarea a mostrar.
         */
        fun newInstance(task: TaskDomain): TaskDetailBottomSheet {
            return TaskDetailBottomSheet().apply {
                arguments = bundleOf("task" to task)
            }
        }
    }
}