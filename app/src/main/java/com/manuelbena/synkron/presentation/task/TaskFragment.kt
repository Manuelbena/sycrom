package com.manuelbena.synkron.presentation.task

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNewTaskBinding
import com.manuelbena.synkron.databinding.ItemCategoryRowSelectorBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter

    // NUEVO: Declaramos el adaptador de recordatorios aquí
    private lateinit var reminderAdapter: ReminderManagerAdapter

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var categoryBinding: ItemCategoryRowSelectorBinding

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()

        // 1. Verificar si estamos en modo edición recuperando el argumento del Bundle
        arguments?.getParcelable<TaskDomain>(ContainerActivity.TASK_TO_EDIT_KEY)?.let { task ->
            // Si existe la tarea, le decimos al ViewModel que cargue los datos
            viewModel.onEvent(TaskEvent.OnLoadTaskForEdit(task))
        }

        categoryBinding = ItemCategoryRowSelectorBinding.bind(binding.layoutCategorySelect.root)

        categoryBinding = ItemCategoryRowSelectorBinding.bind(binding.layoutCategorySelect.root)

        setupRecyclers()
        setupListeners()
        setupDateTimePickers()
        setupTabsAndChips()
    }

    private fun setupRecyclers() {
        // 1. Subtareas
        subtaskAdapter = TaskCreationSubtaskAdapter(
            items = mutableListOf(),
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) },
            onRemove = { item -> viewModel.onEvent(TaskEvent.OnRemoveSubTask(item)) }
        )

        binding.rvSubtareas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtaskAdapter
        }

        val callback = SubtaskTouchHelperCallback(subtaskAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        // 2. NUEVO: Configurar Adaptador de Recordatorios (rvReminders)
        reminderAdapter = ReminderManagerAdapter { reminderItem ->
            // Lógica para eliminar un recordatorio al hacer click en la papelera
            val domainMethod = when (reminderItem.method) {
                ReminderMethod.WHATSAPP -> "email"
                ReminderMethod.ALARM -> "alarm"
                else -> "popup"
            }

            val domainReminder = com.manuelbena.synkron.domain.models.GoogleEventReminder(
                method = domainMethod,
                minutes = reminderItem.offsetMinutes
            )

            viewModel.onEvent(TaskEvent.OnRemoveReminder(domainReminder))
        }

        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reminderAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            tietTitle.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnTitleChange(it.toString())) }
            tietDescription.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnDescriptionChange(it.toString())) }
            tietLocation.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnLocationChange(it.toString())) }

            // NUEVO: Limpiar error de subtarea cuando el usuario escriba
            tietSubTask.doAfterTextChanged {
                if (!it.isNullOrBlank()) tilSubTask.error = null
            }

            // CORREGIDO: Validación al añadir Subtarea
            btnAddSubTask.setOnClickListener {
                val text = tietSubTask.text.toString()
                if (text.isNotBlank()) {
                    viewModel.onEvent(TaskEvent.OnAddSubTask(text))
                    tietSubTask.text?.clear()
                    tilSubTask.error = null // Asegurar que se borra el error
                } else {
                    // Mostrar error visual en el TextInputLayout
                    tilSubTask.error = "Escribe algo primero"
                }
            }

            btnGuardar.setOnClickListener { viewModel.onEvent(TaskEvent.OnSaveClicked) }
            btnCancelar.setOnClickListener { viewModel.onEvent(TaskEvent.OnCancelClicked) }

            btnAddReminder.setOnClickListener { viewModel.onEvent(TaskEvent.OnAddReminderClicked) }

            binding.btnRecurrenceSelector.setOnClickListener {
                viewModel.onEvent(TaskEvent.OnRecurrenceSelectorClicked)
            }

            categoryBinding.containerCategorySelector.setOnClickListener {
                viewModel.onEvent(TaskEvent.OnCategorySelectorClicked)
            }
        }
    }

    // ... (setupDateTimePickers y setupTabsAndChips se mantienen igual) ...
    private fun setupDateTimePickers() {
        binding.btnFecha.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Fecha").build()
            picker.addOnPositiveButtonClickListener { selection ->
                viewModel.onEvent(TaskEvent.OnDateSelected(selection))
            }
            picker.show(childFragmentManager, "DATE")
        }
        binding.btnHoraStart.setOnClickListener { showTimePicker { h, m -> viewModel.onEvent(TaskEvent.OnStartTimeSelected(h, m)) } }
        binding.btnHoraEnd.setOnClickListener { showTimePicker { h, m -> viewModel.onEvent(TaskEvent.OnEndTimeSelected(h, m)) } }
    }

    private fun setupTabsAndChips() {
        binding.tabLayoutTaskType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { tab?.position?.let { viewModel.onEvent(TaskEvent.OnTaskTypeChanged(it)) } }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.chipGroupPriorityTask.setOnCheckedStateChangeListener { _, checkedIds ->
            val priority = when (checkedIds.firstOrNull()) {
                com.manuelbena.synkron.R.id.chip_hight -> "Alta"
                com.manuelbena.synkron.R.id.chip_small -> "Baja"
                else -> "Media"
            }
            viewModel.onEvent(TaskEvent.OnPrioritySelected(priority))
        }

        for (i in 0 until binding.chipGroupDays.childCount) {
            val chip = binding.chipGroupDays.getChildAt(i) as? Chip
            chip?.setOnCheckedChangeListener { _, isChecked ->
                val dayId = chip.tag.toString().toIntOrNull() ?: 0
                viewModel.onEvent(TaskEvent.OnRecurrenceDayToggled(dayId, isChecked))
            }
        }
    }

    override fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.effect.observe(viewLifecycleOwner) { effect ->
            when (effect) {
                is TaskEffect.NavigateBack -> requireActivity().finish()
                is TaskEffect.ShowMessage -> Snackbar.make(binding.root, effect.msg, Snackbar.LENGTH_SHORT).show()

                is TaskEffect.ShowCategoryDialog -> {
                    CategorySelectionDialog { selectedCategory ->
                        viewModel.onEvent(TaskEvent.OnCategorySelected(selectedCategory.title, selectedCategory.googleColorId))
                    }.show(childFragmentManager, CategorySelectionDialog.TAG)
                }

                is TaskEffect.ShowReminderDialog -> {
                    val currentState = viewModel.state.value ?: TaskState()

                    // 1. Fecha/Hora INICIO completa
                    val fullTaskStart = (currentState.selectedDate.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, currentState.startTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, currentState.startTime.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                    }

                    // 2. Fecha/Hora FIN completa (NUEVO)
                    // Usamos la misma fecha base, pero la hora de fin del estado
                    val fullTaskEnd = (currentState.selectedDate.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, currentState.endTime.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, currentState.endTime.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                    }

                    // 3. Pasamos ambas fechas al diálogo
                    AddReminderDialog(fullTaskStart, fullTaskEnd) { reminderItem ->

                        val methodString = when (reminderItem.method) {
                            ReminderMethod.WHATSAPP -> "email"
                            ReminderMethod.ALARM -> "alarm"
                            else -> "popup"
                        }

                        val domainReminder = com.manuelbena.synkron.domain.models.GoogleEventReminder(
                            method = methodString,
                            minutes = reminderItem.offsetMinutes, // Si es negativo, Google entiende "después del inicio"
                            message = reminderItem.message
                        )

                        viewModel.onEvent(TaskEvent.OnAddReminder(domainReminder))

                    }.show(childFragmentManager, "AddReminderDialog")
                }

                is TaskEffect.ShowRecurrenceDialog -> showRecurrenceOptionsDialog()
            }
        }
    }

    private fun renderState(state: TaskState) {
        val dateFormatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.apply {
            if (state.id != 0) {
                // ESTAMOS EDITANDO
                try {
                    toolbar.title = "Editar Tarea"
                } catch (e: Exception) {
                    // Si no encuentras el ID, no pasa nada
                }
                btnGuardar.text = "Actualizar"
            } else {
                // ESTAMOS CREANDO
                try {
                    toolbar.title = "Nueva Tarea"
                } catch (e: Exception) { }
                btnGuardar.text = "Guardar"
            }

            // --- 1. CAMPOS DE TEXTO (NUEVO) ---
            // Usamos una comprobación para evitar bucles infinitos al escribir y que el cursor no salte
            if (tietTitle.text.toString() != state.title) {
                tietTitle.setText(state.title)
            }
            if (tietDescription.text.toString() != state.description) {
                tietDescription.setText(state.description)
            }
            if (tietLocation.text.toString() != state.location) {
                tietLocation.setText(state.location)
            }

            // --- 2. TABS: Evento / Todo el día / Sin fecha (NUEVO) ---
            // Calculamos el índice según el estado
            val expectedTabIndex = when {
                state.isNoDate -> 2
                state.isAllDay -> 1
                else -> 0
            }
            if (tabLayoutTaskType.selectedTabPosition != expectedTabIndex) {
                tabLayoutTaskType.getTabAt(expectedTabIndex)?.select()
            }

            // --- 3. PRIORIDAD (NUEVO) ---
            val chipIdToSelect = when (state.priority) {
                "Alta" -> com.manuelbena.synkron.R.id.chip_hight
                "Baja" -> com.manuelbena.synkron.R.id.chip_small
                else -> com.manuelbena.synkron.R.id.chip_medium
            }
            if (chipGroupPriorityTask.checkedChipId != chipIdToSelect) {
                chipGroupPriorityTask.check(chipIdToSelect)
            }

            // --- 4. FECHAS Y HORAS (YA LO TENÍAS) ---
            btnFecha.text = dateFormatter.format(state.selectedDate.time)
            btnHoraStart.text = timeFormatter.format(state.startTime.time)
            btnHoraEnd.text = timeFormatter.format(state.endTime.time)

            // Control de visibilidad de contenedores
            containerDuration.isVisible = !state.isNoDate
            containerDatetime.isVisible = !state.isAllDay && !state.isNoDate

            // --- 5. CATEGORÍA (YA LO TENÍAS) ---
            val currentCategoryType = CategoryType.getAll()
                .find { it.title == state.category }
                ?: CategoryType.PERSONAL

            with(categoryBinding) {
                tvSelectedCategoryName.text = currentCategoryType.title
                ivSelectedCategoryIcon.setImageResource(currentCategoryType.iconRes)
                val colorInt = androidx.core.content.ContextCompat.getColor(requireContext(), currentCategoryType.colorRes)
                viewSelectedCategoryBackground.background.setTint(colorInt)
            }

            // --- 6. LISTAS (YA LO TENÍAS) ---
            subtaskAdapter.updateItems(state.subTasks)

            // Recordatorios
            val uiReminders = state.reminders.map { domainReminder ->
                // Nota: Asegúrate de que esta lógica de conversión coincide con la que definimos antes
                val displayTimeCalendar = (state.startTime.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -domainReminder.minutes)
                }

                // Mapeo inverso de método para pintar el icono correcto
                val methodEnum = when (domainReminder.method) {
                    "email" -> ReminderMethod.WHATSAPP
                    "alarm" -> ReminderMethod.ALARM
                    else -> ReminderMethod.NOTIFICATION
                }

                ReminderItem(
                    id = UUID.randomUUID().toString(),
                    hour = displayTimeCalendar.get(Calendar.HOUR_OF_DAY),
                    minute = displayTimeCalendar.get(Calendar.MINUTE),
                    method = methodEnum,
                    offsetMinutes = domainReminder.minutes,
                    displayTime = timeFormatter.format(displayTimeCalendar.time),
                    message = domainReminder.message ?: "Recordatorio"
                )
            }
            reminderAdapter.submitList(uiReminders)

            // --- 7. RECURRENCIA (YA LO TENÍAS) ---
            chipGroupDays.isVisible = state.recurrenceType == RecurrenceType.CUSTOM
            tvRecurrenceStatus.text = state.recurrenceType.name // O un texto más amigable si tienes un mapeo

            // Si es custom, marcar los chips de los días
            if (state.recurrenceType == RecurrenceType.CUSTOM) {
                for (i in 0 until chipGroupDays.childCount) {
                    val chip = chipGroupDays.getChildAt(i) as? Chip
                    val dayId = chip?.tag.toString().toIntOrNull() ?: 0
                    chip?.isChecked = state.selectedRecurrenceDays.contains(dayId)
                }
            }
        }
    }

    private fun showRecurrenceOptionsDialog() {
        val options = arrayOf("No se repite", "Todos los días", "Semanalmente", "Personalizado")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Repetir")
            .setItems(options) { _, which ->
                val type = when(which) {
                    1 -> RecurrenceType.DAILY
                    2 -> RecurrenceType.WEEKLY
                    3 -> RecurrenceType.CUSTOM
                    else -> RecurrenceType.NONE
                }
                viewModel.onEvent(TaskEvent.OnRecurrenceTypeSelected(type))
            }
            .show()
    }

    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m -> onTimeSelected(h, m) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }
}