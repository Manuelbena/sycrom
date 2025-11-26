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
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toGoogleEventDateTime
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    // --- Estado Local de la UI ---

    // SUBTAREAS
    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // RECORDATORIOS (¡NUEVO!)
    private val reminderList = mutableListOf<ReminderItem>()
    private lateinit var reminderAdapter: ReminderManagerAdapter

    /**
     * Objeto Calendar que almacena el estado de la FECHA y HORA de INICIO
     */
    private var startCalendar: Calendar = Calendar.getInstance()

    /** Almacena la duración en minutos si el usuario la introduce manualmente. */
    private var selectedDurationInMinutes: Int = 0

    // --- Ciclo de Vida y View Binding ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupDefaultDateTime()
        setupSubtaskManager()
        setupReminderManager() // <--- ¡Inicializamos los recordatorios!
    }

    override fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.btnGuardar.isEnabled = state !is TaskContract.TaskState.Loading
        }

        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is TaskContract.TaskAction.NavigateBack -> requireActivity().finish()
                is TaskContract.TaskAction.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, action.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            btnHoraStart.setOnClickListener { setupTimePicker() }
            btnFecha.setOnClickListener { setupDatePicker() }


            // Listener para añadir aviso
            btnAddReminder.setOnClickListener { showAddReminderDialog() }



            btnGuardar.setOnClickListener { gatherDataAndSave() }
            btnCancelar.setOnClickListener { requireActivity().finish() }
        }
    }

    // --- Lógica de Guardado (ACTUALIZADA con Recordatorios) ---

    private fun gatherDataAndSave() {
        val summary = binding.tietTitle.text.toString().trim()
        if (summary.isEmpty()) {
            binding.tilTitle.error = "El título no puede estar vacío"
            return
        } else {
            binding.tilTitle.error = null
        }

        val description = binding.tietDescription.text.toString().trim()
        val location = binding.tietLocation.text.toString().trim()
        val (taskType, colorId) = "Trabajo" to "#FF5722"
        val duration = 0

        val startDateTime = startCalendar.toGoogleEventDateTime()
        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MINUTE, duration)
        }
        val endDateTime = endCalendar.toGoogleEventDateTime()

        // Mapear Subtareas
        val subTasksToSave = subtaskList.map { subtaskTitle ->
            SubTaskDomain(id = UUID.randomUUID().toString(), title = subtaskTitle, isDone = false)
        }

        // --- MAPEO DE RECORDATORIOS (NUEVO) ---
        // Convertimos tu lista de ReminderItem (UI) a GoogleEventReminder (Dominio/API)
        val googleRemindersList = reminderList.map { item ->
            // Mapeamos tu Enum interno a los strings que espera Google/N8n
            val methodString = when (item.method) {
                ReminderMethod.NOTIFICATION -> "popup"
                ReminderMethod.WHATSAPP -> "email" // Usamos "email" para interceptarlo en N8n y mandar Whatsapp
                ReminderMethod.ALARM -> "popup"
            }
            // Usamos offsetMinutes para decirle "cuantos minutos antes"
            GoogleEventReminder(minutes = item.offsetMinutes, method = methodString)
        }

        val reminders = GoogleEventReminders(
            useDefault = false,
            overrides = googleRemindersList // ¡Aquí inyectamos la lista dinámica!
        )
        // -------------------------------------

        val taskToSave = TaskDomain(
            id = 0L,
            summary = summary,
            description = description,
            location = location,
            colorId = colorId,
            start = startDateTime,
            end = endDateTime,
            attendees = emptyList(),
            recurrence = emptyList(),
            reminders = reminders, // <--- Pasamos el objeto creado arriba
            transparency = "opaque",
            conferenceLink = null,
            subTasks = subTasksToSave,
            typeTask = taskType,
            priority = "media",
            isActive = true,
            isDone = false
        )

        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))
    }

    // --- Configuración de Managers (Subtareas y Recordatorios) ---

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
            }
        }
    }

    // --- SETUP RECORDATORIOS (NUEVO) ---
    private fun setupReminderManager() {
        // Inicializamos el adapter pasándole la función de borrar
        reminderAdapter = ReminderManagerAdapter { reminderToDelete ->
            val position = reminderList.indexOf(reminderToDelete)
            if (position != -1) {
                reminderList.removeAt(position)
                reminderAdapter.submitList(reminderList.toList()) // Actualizamos la lista
            }
        }

        binding.rvReminders.apply {
            adapter = reminderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Cargar lista inicial vacía (o podrías poner uno por defecto aquí)
        reminderAdapter.submitList(reminderList.toList())
    }

    private fun showAddReminderDialog() {
        val dialog = AddReminderDialog { newReminder ->
            // Callback cuando el usuario guarda en el diálogo
            reminderList.add(newReminder)
            reminderAdapter.submitList(reminderList.toList()) // Refresca el adapter
        }
        dialog.show(childFragmentManager, "AddReminderDialog")
    }
    // -----------------------------------

    // --- Helpers de Fecha y UI (Se mantienen igual) ---

    private fun setupDefaultDateTime() {
        startCalendar = Calendar.getInstance()
        updateDateTimeButtons()
    }

    private fun setupDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(startCalendar.timeInMillis)
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            startCalendar.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH))
            updateDateTimeButtons()
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun setupTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(startCalendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(startCalendar.get(Calendar.MINUTE))
            .setTitleText("Selecciona una hora")
            .build()
        timePicker.addOnPositiveButtonClickListener {
            startCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            startCalendar.set(Calendar.MINUTE, timePicker.minute)
            updateDateTimeButtons()
        }
        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun updateDateTimeButtons() {
        binding.btnFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startCalendar.time)
        binding.btnHoraStart.text = String.format(Locale.getDefault(), "%02d:%02d", startCalendar.get(Calendar.HOUR_OF_DAY), startCalendar.get(Calendar.MINUTE))
    }

    private fun setupDurationPicker() {
        // (Tu código de duración existente se mantiene igual...)
        // He omitido el cuerpo para no alargar la respuesta, pero déjalo como estaba.
        val context = requireContext()
        // ... Lógica del dialog ...
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }
        val textInputLayout = TextInputLayout(context)
        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Valor"
        }
        textInputLayout.addView(editText)
        container.addView(textInputLayout)

        val radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val radioMinutos = RadioButton(context).apply {
            text = "Minutos"; id = View.generateViewId(); isChecked = true
        }
        val radioHoras = RadioButton(context).apply {
            text = "Horas"; id = View.generateViewId()
        }
        radioGroup.addView(radioMinutos)
        radioGroup.addView(radioHoras)
        container.addView(radioGroup)

    }
}