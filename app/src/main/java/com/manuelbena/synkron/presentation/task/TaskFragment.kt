package com.manuelbena.synkron.presentation.task

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNewTaskBinding
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Fragment para la creación (y futura edición) de una Tarea.
 *
 * Responsabilidades:
 * - Mostrar el formulario [fragment_new_task.xml].
 * - Configurar los selectores de fecha, hora y duración.
 * - Gestionar la lista de creación de subtareas (añadir, eliminar, reordenar).
 * - Recopilar todos los datos del formulario.
 * - Enviar el evento [TaskContract.TaskEvent.OnSaveTask] al [TaskViewModel].
 * - Observar [TaskViewModel.state] para mostrar (Loading, Error).
 * - Observar [TaskViewModel.action] para (NavigateBack, ShowErrorSnackbar).
 */
@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    // --- Estado Local de la UI ---

    /** Lista de strings para los títulos de las subtareas. */
    private val subtaskList = mutableListOf<String>()

    /** Adapter para el RecyclerView de subtareas. */
    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter

    /** Helper para gestionar el drag-and-drop. */
    private lateinit var itemTouchHelper: ItemTouchHelper

    /** Timestamp (en ms) de la fecha seleccionada. */
    private var selectedTimestamp: Long = System.currentTimeMillis()

    /** Hora seleccionada (en minutos desde medianoche). */
    private var selectedTimeInMinutes: Int = 0

    // --- Ciclo de Vida ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Nota: setUI(), setListener() y observe() se llaman desde BaseFragment
    }

    /**
     * Configura el estado inicial de la UI.
     */
    override fun setUI() {
        super.setUI()
        setupDefaultDateTime()
        setupSubtaskManager()
    }

    /**
     * Observa los cambios de Estado (State) y Acciones (Action) del ViewModel.
     */
    override fun observe() {
        // Observa el ESTADO (Loading, Error, Success)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Mostramos/Ocultamos un ProgressBar (necesitarías añadirlo al XML)
            // binding.progressBar.isVisible = state is TaskContract.TaskState.Loading

            // Habilitamos/Deshabilitamos el botón de guardar
            binding.btnGuardar.isEnabled = state !is TaskContract.TaskState.Loading

            if (state is TaskContract.TaskState.Error) {
                // El error ya se muestra con la Acción, aquí solo reseteamos el estado
                // O podríamos mostrar un error persistente si quisiéramos.
            }
        }

        // Observa las ACCIONES (Navegación, SnackBar)
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is TaskContract.TaskAction.NavigateBack -> {
                    // El VM nos ordena navegar atrás
                    requireActivity().finish()
                }
                is TaskContract.TaskAction.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, action.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Configura todos los listeners de la UI.
     */
    override fun setListener() {
        super.setListener()
        binding.apply {
            btnHora.setOnClickListener { setupTimePicker() }
            btnFecha.setOnClickListener { setupDatePicker() }
            btnDuracion.setOnClickListener { setupDurationPicker() }

            // Limpia el botón de duración si se selecciona un chip
            chipGroupDuration.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    binding.btnDuracion.text = getString(R.string.duration_button_default)
                }
            }

            // Listener principal de guardado
            btnGuardar.setOnClickListener {
                gatherDataAndSave()
            }

            // Listener para el botón de cancelar
            btnCancelar.setOnClickListener {
                requireActivity().finish() // Cierra la actividad
            }
        }
    }

    // --- Lógica de Guardado ---

    /**
     * Recopila todos los datos del formulario, los valida
     * y envía el evento [TaskContract.TaskEvent.OnSaveTask] al ViewModel.
     */
    private fun gatherDataAndSave() {
        val title = binding.tietTitle.text.toString().trim() // ID Refactorizado

        // Validación de campos
        if (title.isEmpty()) {
            binding.tilTitle.error = "El título no puede estar vacío" // ID Refactorizado
            return
        } else {
            binding.tilTitle.error = null
        }

        val description = binding.tietDescription.text.toString().trim()
        val place = binding.tietLocation.text.toString().trim()
        val durationInMinutes = getDurationInMinutes()
        val taskType = getSelectedTaskType()
        val taskPriority = getSelectedTaskPriority()

        // Mapea la lista de strings a [SubTaskDomain]
        val subTasksToSave = subtaskList.map { subtaskTitle ->
            SubTaskDomain(id = UUID.randomUUID().toString(), title = subtaskTitle, isDone = false)
        }

        // Crea el objeto de dominio
        val taskToSave = TaskDomain(
            id = 0L, // Room generará el ID
            title = title,
            description = description,
            date = selectedTimestamp, // Usa la variable de estado
            hour = selectedTimeInMinutes, // Usa la variable de estado
            duration = durationInMinutes,
            typeTask = taskType,
            place = place,
            subTasks = subTasksToSave,
            isDone = false,
            isActive = true,
            isDeleted = false,
            isArchived = false,
            isPinned = false,
            priority = getSelectedTaskPriority()
        )

        // Envía el evento al ViewModel
        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))
    }

    // --- Configuración de UI (Helpers) ---

    /**
     * Establece la fecha y hora actuales como valores por defecto.
     */
    private fun setupDefaultDateTime() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        // Fecha por defecto
        selectedTimestamp = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.btnFecha.text = dateFormat.format(Date(selectedTimestamp))

        // Hora por defecto
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        selectedTimeInMinutes = (currentHour * 60) + currentMinute
        val timeFormat = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)
        binding.btnHora.text = timeFormat
    }

    /**
     * Configura el RecyclerView de subtareas con su Adapter y el ItemTouchHelper.
     */
    private fun setupSubtaskManager() {
        // 1. Instancia el NUEVO adapter externo
        subtaskAdapter = TaskCreationSubtaskAdapter(subtaskList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder) // Callback para iniciar el drag
        }

        binding.rvSubtareas.adapter = subtaskAdapter
        binding.rvSubtareas.layoutManager = LinearLayoutManager(requireContext())

        // 2. Instancia el NUEVO Callback externo
        val callback = SubtaskTouchHelperCallback(subtaskAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        // 3. Listener para añadir subtarea
        binding.btnAddSubTask.setOnClickListener {
            val subtaskText = binding.tietSubTask.text.toString().trim()
            if (subtaskText.isNotEmpty()) {
                subtaskList.add(subtaskText)
                subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
                binding.tietSubTask.text?.clear()
                binding.tilSubTask.error = null // Limpia error
            } else {
                binding.tilSubTask.error = "La subtarea no puede estar vacía"
            }
        }
    }

    /**
     * Muestra el [MaterialDatePicker] y actualiza [selectedTimestamp].
     */
    private fun setupDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(selectedTimestamp) // Usa el estado actual
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedTimestamp = selection // Actualiza el estado

            val date = Date(selection)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // Importante para que coincida con el picker
            binding.btnFecha.text = format.format(date)
        }
        datePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
    }

    /**
     * Muestra el [MaterialTimePicker] y actualiza [selectedTimeInMinutes].
     */
    private fun setupTimePicker() {
        val currentHour = selectedTimeInMinutes / 60
        val currentMinute = selectedTimeInMinutes % 60

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Selecciona una hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val horaSeleccionada = timePicker.hour
            val minutoSeleccionado = timePicker.minute

            selectedTimeInMinutes = (horaSeleccionada * 60) + minutoSeleccionado // Actualiza el estado

            val horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", horaSeleccionada, minutoSeleccionado)
            binding.btnHora.text = horaFormateada
        }
        timePicker.show(childFragmentManager, "MATERIAL_TIME_PICKER")
    }

    /**
     * Muestra un diálogo para insertar una duración personalizada.
     */
    private fun setupDurationPicker() {
        binding.chipGroupDuration.clearCheck()
        val context = requireContext()

        // --- Creación de Vistas por Código ---
        // (Esto es correcto, ya que es una vista de diálogo temporal)
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        container.setPadding(padding, 0, padding, 0)

        val textInputLayout = TextInputLayout(context)
        val editText = TextInputEditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.hint = "Valor"
        textInputLayout.addView(editText)
        container.addView(textInputLayout)

        val radioGroup = RadioGroup(context)
        radioGroup.orientation = RadioGroup.HORIZONTAL
        val radioMinutos = RadioButton(context).apply { text = "Minutos"; id = View.generateViewId(); isChecked = true }
        val radioHoras = RadioButton(context).apply { text = "Horas"; id = View.generateViewId() }
        radioGroup.addView(radioMinutos)
        radioGroup.addView(radioHoras)
        container.addView(radioGroup)
        // --- Fin Creación Vistas ---

        MaterialAlertDialogBuilder(context)
            .setTitle("Ingresar Duración")
            .setMessage("Escribe la duración y selecciona la unidad.")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                val duracionStr = editText.text.toString()
                if (duracionStr.isNotEmpty()) {
                    try {
                        val valor = duracionStr.toInt()
                        val minutosTotales = if (radioGroup.checkedRadioButtonId == radioHoras.id) {
                            valor * 60 // Horas a minutos
                        } else {
                            valor // Minutos
                        }
                        binding.btnDuracion.text = "Duración: $minutosTotales min"
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Por favor ingresa un número válido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // --- Métodos 'Getter' de UI ---

    /**
     * Obtiene la duración en minutos, ya sea de los Chips o del botón.
     */
    private fun getDurationInMinutes(): Int {
        val checkedChipId = binding.chipGroupDuration.checkedChipId
        if (checkedChipId != View.NO_ID) {
            return when (checkedChipId) {
                R.id.chip_15_min -> 15
                R.id.chip_30_min -> 30
                R.id.chip_60_min -> 45 // ¡Cuidado! Este chip dice 60 min en el XML pero da 45. Lo mantengo.
                R.id.chip_1_hour -> 60
                R.id.chip_5_hour -> 300
                R.id.chip_8_hour -> 480
                else -> 0
            }
        }

        // Si no hay chip, lee el botón
        val durationText = binding.btnDuracion.text.toString()
        if (durationText.startsWith("Duración:", ignoreCase = true)) {
            return durationText.removePrefix("Duración:").trim().split(" ")[0].toIntOrNull() ?: 0
        }
        return 0
    }

    /**
     * Obtiene el string del tipo de tarea (Categoría) seleccionado.
     */
    private fun getSelectedTaskType(): String {
        val checkedChipId = binding.chipGroupTypeTask.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            view?.findViewById<com.google.android.material.chip.Chip>(checkedChipId)?.text?.toString() ?: "Personal"
        } else {
            "Personal" // Valor por defecto
        }
    }

    /**
     * Obtiene el string del tipo de tarea (Categoría) seleccionado.
     */
    private fun getSelectedTaskPriority(): String {
        val checkedChipId = binding.chipGroupPriorityTask.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            view?.findViewById<com.google.android.material.chip.Chip>(checkedChipId)?.text?.toString() ?: "Media"
        } else {
            "Personal" // Valor por defecto
        }
    }
}