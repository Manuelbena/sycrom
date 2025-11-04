package com.manuelbena.synkron.presentation.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BotoomSheetTaskDetailBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.SubTaskDomain
// ¡¡ASEGÚRATE DE QUE ESTA IMPORTACIÓN SEA CORRECTA!!
import com.manuelbena.synkron.presentation.util.adapters.SubtaskAdapter
import com.manuelbena.synkron.presentation.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARG_TASK = "arg_task"

@AndroidEntryPoint
class TaskDetailBottomSheet(
    // La tarea inicial. Debe ser 'var' para poder actualizarla.
    private var task: TaskDomain,
) : BottomSheetDialogFragment() {

    private var _binding: BotoomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!

    // Inyectamos el ViewModel compartido con el HomeFragment
    private val viewModel: HomeViewModel by activityViewModels()

    // Inicializamos el adaptador pasando la lambda que llama al ViewModel
    private val subtaskAdapter = SubtaskAdapter { subtask, isDone ->
        viewModel.onSubtaskCheckedChanged(task, subtask, isDone)
    }

    // Formateador para la fecha
    private val dateFormatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BotoomSheetTaskDetailBinding.inflate(inflater, container, false)

        // Recuperar la tarea (ya sea de los argumentos o del constructor)
        // y asegurarnos de que nuestra variable 'task' local esté actualizada.
        val taskToBind = arguments?.getParcelable<TaskDomain>(ARG_TASK) ?: this.task
        this.task = taskToBind // Actualizamos la variable local
        bindTaskData(taskToBind)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.subtasksRecyclerView.adapter = subtaskAdapter
    }

    private fun bindTaskData(task: TaskDomain) {
        binding.titleTextView.text = task.title
        binding.typeChip.text = task.typeTask
        binding.descriptionTextView.text =
            task.description.takeIf { it.isNotEmpty() } ?: "Sin descripción"
        binding.placeTextView.text = task.place.takeIf { it.isNotEmpty() } ?: "Sin ubicación"

        // --- Formateo de fecha y hora ---
        // Fecha (desde timestamp Long)
        val date = Date(task.date)
        val formattedDate = dateFormatter.format(date)

        // Hora (desde Int de minutos)
        val hours = task.hour / 60
        val minutes = task.hour % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)

        binding.dateTimeTextView.text = "$formattedDate - $formattedTime"
        // --- Fin de formateo ---

        // Enviamos la lista (posiblemente actualizada) al adaptador
        subtaskAdapter.submitList(task.subTasks)

        // Ocultar/Mostrar sección de subtareas si está vacía
        val hasSubtasks = task.subTasks.isNotEmpty()
        binding.subtasksRecyclerView.visibility = if (hasSubtasks) View.VISIBLE else View.GONE
        // Referencia al TextView de "Subtareas" (asumiendo que tiene un ID `subtasksTitleTextView` o similar)
        // Por ejemplo, si el TextView "Subtareas" en `botoom_sheet_task_detail.xml` tuviera id `tvSubtasksTitle`:
        // binding.tvSubtasksTitle.visibility = if (hasSubtasks) View.VISIBLE else View.GONE
    }

    /**
     * Función pública llamada por HomeFragment cuando el ViewModel
     * notifica que esta tarea ha sido actualizada.
     */
    fun updateTask(newTask: TaskDomain) {
        // Comprobamos si la tarea actualizada es la que estamos mostrando
        // (Sería mejor usar un ID único si lo tuvieras)
        if (task.title == newTask.title && task.date == newTask.date && task.hour == newTask.hour) {
            this.task = newTask // Actualizamos la variable local
            arguments?.putParcelable(ARG_TASK, newTask) // Actualizamos el bundle por si se recrea

            // Si el binding aún existe, actualizamos la UI
            if (_binding != null) {
                bindTaskData(newTask)
            }
        }
    }


    override fun onStart() {
        super.onStart()

        // --- AQUÍ EMPIEZA LA MAGIA ---
        val dialog = dialog as? BottomSheetDialog
        val behavior = dialog?.behavior

        if (behavior != null) {
            // 1. Permitimos que se oculte al arrastrarlo hasta abajo.
            behavior.isHideable = true

            // 2. Hacemos que al arrastrar, pase de EXPANDED a HIDDEN directamente,
            // sin pasar por el estado intermedio COLLAPSED.
            behavior.skipCollapsed = true

            // 3. Forzamos a que siempre aparezca completamente expandido.
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskDetailBottomSheet"

        // Patrón de Fábrica: La forma correcta de crear fragmentos con argumentos
        fun newInstance(task: TaskDomain): TaskDetailBottomSheet {
            val args = Bundle().apply {
                putParcelable(ARG_TASK, task)
            }
            // Pasamos la tarea también al constructor
            return TaskDetailBottomSheet(task = task).apply {
                arguments = args
            }
        }
    }
}

