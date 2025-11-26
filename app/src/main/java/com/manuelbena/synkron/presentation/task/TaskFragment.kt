package com.manuelbena.synkron.presentation.task

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNewTaskBinding
import com.manuelbena.synkron.databinding.ItemCategoryRowSelectorBinding // Para el include
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter // Asumiendo que existe o reutilizas uno
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    // Adaptadores
    private val subtaskAdapter by lazy { TaskCreationSubtaskAdapter(mutableListOf()) { /* Drag handle logic */ } }
    // Necesitarás un adaptador simple para recordatorios
    // private val reminderAdapter by lazy { ... }

    // Binding para el include de categorías
    private lateinit var categoryBinding: ItemCategoryRowSelectorBinding

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        // Vincular el include
        categoryBinding = ItemCategoryRowSelectorBinding.bind(binding.layoutCategorySelect.root)

        setupRecyclers()
        setupInputs()
        setupDateTimePickers()
        setupTabsAndChips()
    }

    private fun setupRecyclers() {
        // Subtareas
        binding.rvSubtareas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtaskAdapter
        }
        // Recordatorios
        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // adapter = reminderAdapter
        }
    }

    private fun setupInputs() {
        binding.apply {
            // Text Watchers
            tietTitle.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnTitleChange(it.toString())) }
            tietDescription.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnDescriptionChange(it.toString())) }
            tietLocation.doAfterTextChanged { viewModel.onEvent(TaskEvent.OnLocationChange(it.toString())) }

            // Subtarea con botón
            btnAddSubTask.setOnClickListener {
                val text = tietSubTask.text.toString()
                if (text.isNotBlank()) {
                    viewModel.onEvent(TaskEvent.OnAddSubTask(text))
                    tietSubTask.text?.clear()
                }
            }

            // Acciones principales
            btnGuardar.setOnClickListener { viewModel.onEvent(TaskEvent.OnSaveClicked) }
            btnCancelar.setOnClickListener { viewModel.onEvent(TaskEvent.OnCancelClicked) }

            // Recordatorio
            btnAddReminder.setOnClickListener { viewModel.onEvent(TaskEvent.OnAddReminderClicked) }

            // Recurrencia
            btnRecurrenceSelector.setOnClickListener { viewModel.onEvent(TaskEvent.OnRecurrenceSelectorClicked) }
        }
    }

    private fun setupDateTimePickers() {
        binding.btnFecha.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Fecha de tarea").build()
            picker.addOnPositiveButtonClickListener { selection ->
                viewModel.onEvent(TaskEvent.OnDateSelected(selection))
            }
            picker.show(childFragmentManager, "DATE")
        }

        binding.btnHoraStart.setOnClickListener {
            showTimePicker { h, m -> viewModel.onEvent(TaskEvent.OnStartTimeSelected(h, m)) }
        }

        binding.btnHoraEnd.setOnClickListener {
            showTimePicker { h, m -> viewModel.onEvent(TaskEvent.OnEndTimeSelected(h, m)) }
        }
    }

    private fun setupTabsAndChips() {
        // Tabs: Planificado, Todo el día, Sin fecha
        binding.tabLayoutTaskType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { viewModel.onEvent(TaskEvent.OnTaskTypeChanged(it)) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Prioridad
        binding.chipGroupPriorityTask.setOnCheckedStateChangeListener { _, checkedIds ->
            val priority = when (checkedIds.firstOrNull()) {
                R.id.chip_hight -> "Alta"
                R.id.chip_small -> "Baja"
                else -> "Media"
            }
            viewModel.onEvent(TaskEvent.OnPrioritySelected(priority))
        }

        // Días de Recurrencia (L, M, X...)
        // Iteramos por los hijos del ChipGroup para asignar listeners
        for (i in 0 until binding.chipGroupDays.childCount) {
            val chip = binding.chipGroupDays.getChildAt(i) as? Chip
            chip?.setOnCheckedChangeListener { _, isChecked ->
                val dayId = chip.tag.toString().toInt() // Asegúrate de poner tag="2" para Monday en XML, etc
                viewModel.onEvent(TaskEvent.OnRecurrenceDayToggled(dayId, isChecked))
            }
        }
    }

    // --- State Rendering ---

    override fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
        viewModel.effect.observe(viewLifecycleOwner) { effect ->
            when(effect) {
                is TaskEffect.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
                is TaskEffect.ShowMessage -> Snackbar.make(binding.root, effect.msg, Snackbar.LENGTH_SHORT).show()
                is TaskEffect.ShowReminderDialog -> { /* Mostrar tu diálogo de recordatorios */ }
                is TaskEffect.ShowRecurrenceDialog -> { /* Mostrar diálogo selección recurrencia */ }
            }
        }
    }

    private fun renderState(state: TaskState) {
        val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.apply {
            // Textos Botones Fecha/Hora
            btnFecha.text = dateFormatter.format(state.selectedDate.time)
            btnHoraStart.text = timeFormatter.format(state.startTime.time)
            btnHoraEnd.text = timeFormatter.format(state.endTime.time)

            // Visibilidad según TABS
            // Si es "No Fecha" (tab 2), ocultamos todo el contenedor de planificación
            containerDuration.isVisible = !state.isNoDate

            // Si es "Todo el día" (tab 1), ocultamos las horas, solo dejamos la fecha
            containerDatetime.isVisible = !state.isAllDay && !state.isNoDate

            // Si es "Todo el día", cambiamos el texto del botón fecha para que ocupe todo o se centre
            // (Opcional, según diseño visual)

            // Recurrencia: Mostrar chips si es Custom (ejemplo)
            chipGroupDays.isVisible = state.recurrenceType == RecurrenceType.CUSTOM
            tvRecurrenceStatus.text = when(state.recurrenceType) {
                RecurrenceType.NONE -> "No se repite"
                RecurrenceType.DAILY -> "Todos los días"
                RecurrenceType.WEEKLY -> "Semanalmente"
                RecurrenceType.CUSTOM -> "Personalizado"
            }
        }
    }

    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m ->
            onTimeSelected(h, m)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }
}