package com.manuelbena.synkron.presentation.task

import TaskDomain
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
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
import com.manuelbena.synkron.domain.models.GoogleEventAttendee
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.SubTaskDomain

import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.util.toGoogleEventDateTime
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Fragment para la creación (y futura edición) de una Tarea.
 *
 * Responsabilidades:
 * - Mostrar el formulario [fragment_new_task.xml] usando View Binding.
 * - Gestionar los selectores de fecha, hora y duración.
 * - Recopilar los datos y construir el objeto [TaskDomain] unificado con Google Calendar.
 * - Enviar el evento [TaskContract.TaskEvent.OnSaveTask] al [TaskViewModel].
 * - Observar [TaskViewModel.state] y [TaskViewModel.action].
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

    /**
     * Objeto Calendar que almacena el estado de la FECHA y HORA de INICIO
     * seleccionadas por el usuario.
     */
    private var startCalendar: Calendar = Calendar.getInstance()

    /** Almacena la duración en minutos si el usuario la introduce manualmente. */
    private var selectedDurationInMinutes: Int = 0

    // --- Ciclo de Vida y View Binding ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        // View Binding en acción
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setUI(), setListener() y observe() se llaman desde BaseFragment
    }

    /**
     * Configura el estado inicial de la UI (valores por defecto).
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
            // Usamos binding para acceder al botón
            binding.btnGuardar.isEnabled = state !is TaskContract.TaskState.Loading
        }

        // Observa las ACCIONES (Navegación, SnackBar)
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is TaskContract.TaskAction.NavigateBack -> {
                    requireActivity().finish()
                }
                is TaskContract.TaskAction.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, action.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Configura todos los listeners de la UI usando View Binding.
     */
    override fun setListener() {
        super.setListener()
        // Usamos binding.apply para acceder a todas las vistas
        binding.apply {
            btnHora.setOnClickListener { setupTimePicker() }
            btnFecha.setOnClickListener { setupDatePicker() }
            btnDuracion.setOnClickListener { setupDurationPicker() }

            // Limpia el botón de duración si se selecciona un chip
            chipGroupDuration.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    btnDuracion.text = getString(R.string.duration_button_default)
                    selectedDurationInMinutes = 0 // Resetea duración custom
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

    // --- Lógica de Guardado (ACTUALIZADA) ---

    /**
     * Recopila todos los datos del formulario, los valida
     * y envía el evento [TaskContract.TaskEvent.OnSaveTask] al ViewModel.
     */
    private fun gatherDataAndSave() {
        // 1. Recoger datos simples (usando IDs de binding)
        val summary = binding.tietTitle.text.toString().trim()
        if (summary.isEmpty()) {
            binding.tilTitle.error = "El título no puede estar vacío"
            return
        } else {
            binding.tilTitle.error = null
        }

        val description = binding.tietDescription.text.toString().trim()
        val location = binding.tietLocation.text.toString().trim()

        // 2. Calcular Duración y Fechas
        val (taskType, colorId) = getTaskTypeAndColorId()
        val duration = getDurationInMinutes()

        // 3. Crear objetos de Fecha/Hora de Google
        // Usamos nuestra nueva extensión .toGoogleEventDateTime()
        val startDateTime = startCalendar.toGoogleEventDateTime()

        // Calculamos la hora de fin
        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MINUTE, duration)
        }
        val endDateTime = endCalendar.toGoogleEventDateTime()

        // 4. Mapear Subtareas
        val subTasksToSave = subtaskList.map { subtaskTitle ->
            SubTaskDomain(id = UUID.randomUUID().toString(), title = subtaskTitle, isDone = false)
        }

        // 5. Crear objetos de Google (por ahora vacíos o por defecto)
        val attendees = emptyList<GoogleEventAttendee>()
        val reminders = GoogleEventReminders(
            useDefault = false,
            overrides = listOf(GoogleEventReminder(minutes = 10)) // Por defecto 10 min
        )
        val recurrence = emptyList<String>()

        // 6. Construir el TaskDomain (El nuevo modelo anidado)
        val taskToSave = TaskDomain(
            id = 0L,
            summary = summary,
            description = description,
            location = location,
            colorId = colorId,
            start = startDateTime,
            end = endDateTime,
            attendees = attendees,
            recurrence = recurrence,
            reminders = reminders,
            transparency = "opaque", // Ocupado
            conferenceLink = null,
            // Campos internos de Synkrón
            subTasks = subTasksToSave,
            typeTask = taskType,
            priority = getSelectedPriority(),
            isActive = true,
            isDone = false
        )

        // 7. Enviar al ViewModel
        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))
    }

    // --- Configuración de UI (Helpers) ---

    /**
     * Establece la fecha y hora actuales como valores por defecto en [startCalendar].
     */
    private fun setupDefaultDateTime() {
        startCalendar = Calendar.getInstance()

        // Actualizar los botones de la UI (usando binding)
        binding.btnFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startCalendar.time)
        binding.btnHora.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            startCalendar.get(Calendar.HOUR_OF_DAY),
            startCalendar.get(Calendar.MINUTE)
        )
    }

    /**
     * Configura el RecyclerView de subtareas (usando binding).
     */
    private fun setupSubtaskManager() {
        subtaskAdapter = TaskCreationSubtaskAdapter(subtaskList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        binding.rvSubtareas.adapter = subtaskAdapter
        binding.rvSubtareas.layoutManager = LinearLayoutManager(requireContext())

        val callback = SubtaskTouchHelperCallback(subtaskAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        binding.btnAddSubTask.setOnClickListener {
            val subtaskText = binding.tietSubTask.text.toString().trim()
            if (subtaskText.isNotEmpty()) {
                subtaskList.add(subtaskText)
                subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
                binding.tietSubTask.text?.clear()
                binding.tilSubTask.error = null
            } else {
                binding.tilSubTask.error = "La subtarea no puede estar vacía"
            }
        }
    }

    /**
     * Muestra el [MaterialDatePicker] y actualiza [startCalendar].
     */
    private fun setupDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(startCalendar.timeInMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Corrección UTC
            val selectedUtcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }

            // Actualizamos SÓLO la fecha en 'startCalendar'
            startCalendar.set(
                selectedUtcCalendar.get(Calendar.YEAR),
                selectedUtcCalendar.get(Calendar.MONTH),
                selectedUtcCalendar.get(Calendar.DAY_OF_MONTH)
            )

            // Actualizar botón (usando binding)
            binding.btnFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startCalendar.time)
        }
        datePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
    }

    /**
     * Muestra el [MaterialTimePicker] y actualiza [startCalendar].
     */
    private fun setupTimePicker() {
        val currentHour = startCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = startCalendar.get(Calendar.MINUTE)

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Selecciona una hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            // Actualizamos SÓLO la hora y minuto en 'startCalendar'
            startCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            startCalendar.set(Calendar.MINUTE, timePicker.minute)

            // Actualizar botón (usando binding)
            val horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", timePicker.hour, timePicker.minute)
            binding.btnHora.text = horaFormateada
        }
        timePicker.show(childFragmentManager, "MATERIAL_TIME_PICKER")
    }

    /**
     * Muestra un diálogo para insertar una duración personalizada (usando binding).
     */
    private fun setupDurationPicker() {
        binding.chipGroupDuration.clearCheck()
        val context = requireContext()

        // (Creación de vistas del diálogo...)
        val container = LinearLayout(context) //...
        val textInputLayout = TextInputLayout(context) //...
        val editText = TextInputEditText(context) //...
        val radioGroup = RadioGroup(context) //...
        val radioMinutos = RadioButton(context) //...
        val radioHoras = RadioButton(context) //...
        // ... (configuración de vistas del diálogo) ...

        MaterialAlertDialogBuilder(context)
            .setTitle("Ingresar Duración")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                val duracionStr = editText.text.toString()
                if (duracionStr.isNotEmpty()) {
                    try {
                        val valor = duracionStr.toInt()
                        val minutosTotales = if (radioGroup.checkedRadioButtonId == radioHoras.id) {
                            valor * 60
                        } else {
                            valor
                        }
                        selectedDurationInMinutes = minutosTotales
                        // Actualizar botón (usando binding)
                        binding.btnDuracion.text = "Duración: $minutosTotales min"
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Número inválido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // --- Métodos 'Getter' de UI (usando binding) ---

    /**
     * Obtiene la duración en minutos, ya sea de los Chips o de la variable de estado.
     */
    private fun getDurationInMinutes(): Int {
        val checkedChipId = binding.chipGroupDuration.checkedChipId
        if (checkedChipId != View.NO_ID) {
            return when (checkedChipId) {
                R.id.chip_15_min -> 15
                R.id.chip_30_min -> 30
                R.id.chip_60_min -> 45
                R.id.chip_1_hour -> 60
                R.id.chip_5_hour -> 300
                R.id.chip_8_hour -> 480
                else -> 0
            }
        }
        if (selectedDurationInMinutes > 0) {
            return selectedDurationInMinutes
        }
        return 0
    }

    /**
     * Obtiene el string del tipo de tarea (Categoría) Y su
     * 'colorId' de Google Calendar correspondiente.
     */
    private fun getTaskTypeAndColorId(): Pair<String, String> {
        return when (binding.chipGroupTypeTask.checkedChipId) {
            R.id.chip_work -> "Trabajo" to "9"
            R.id.chip_studen -> "Estudios" to "11"
            R.id.chip_salud -> "Salud" to "10"
            R.id.chip_other -> "Otro" to "8"
            R.id.chip_personal -> "Personal" to "2"
            else -> "Personal" to "2" // Default
        }
    }

    /**
     * Obtiene el string de la prioridad seleccionada.
     */
    private fun getSelectedPriority(): String {
        return when {
            binding.chipHight.isChecked -> "Alta"
            binding.chipMedium.isChecked -> "Media"
            binding.chipSmall.isChecked -> "Baja"
            else -> "Media" // Default
        }
    }
}