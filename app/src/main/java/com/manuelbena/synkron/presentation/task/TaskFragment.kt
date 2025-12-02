package com.manuelbena.synkron.presentation.task

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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

import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.adapter.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.util.extensions.formatDate
import com.manuelbena.synkron.presentation.util.extensions.formatTime
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var reminderAdapter: ReminderManagerAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var categoryBinding: ItemCategoryRowSelectorBinding

    private val args: TaskFragmentArgs by navArgs()

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        if (args.taskId != 0) {
            // viewModel.onEvent(TaskEvent.OnLoadTaskForEdit(args.taskId))
        }
        categoryBinding = ItemCategoryRowSelectorBinding.bind(binding.layoutCategorySelect.root)
        setupRecyclers()
        setupListeners()
        setupDateTimePickers()
        setupTabsAndChips()
    }

    private fun setupRecyclers() {
        // Inicializamos el adaptador con la lista mutable y el callback de reordenamiento
        subtaskAdapter = TaskCreationSubtaskAdapter(
            items = mutableListOf(),
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) },
            onRemove = { item -> viewModel.onEvent(TaskEvent.OnRemoveSubTask(item)) },
            onReorder = { newItems ->
                // Cuando se mueve un item, actualizamos el ViewModel para que guarde el nuevo orden
                viewModel.onEvent(TaskEvent.OnReorderSubTasks(newItems))
            }
        )

        binding.rvSubtareas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtaskAdapter
            isVisible = true
        }

        val callback = SubtaskTouchHelperCallback(subtaskAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        reminderAdapter = ReminderManagerAdapter { reminderItem ->
            val methodString = when (reminderItem.method) {
                ReminderMethod.WHATSAPP -> "email"
                ReminderMethod.ALARM -> "alarm"
                else -> "popup"
            }
            val domainReminder = com.manuelbena.synkron.domain.models.GoogleEventReminder(
                method = methodString,
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
            tietTitle.doAfterTextChanged { if(it.toString() != viewModel.state.value?.title) viewModel.onEvent(TaskEvent.OnTitleChange(it.toString())) }
            tietDescription.doAfterTextChanged { if(it.toString() != viewModel.state.value?.description) viewModel.onEvent(TaskEvent.OnDescriptionChange(it.toString())) }
            tietLocation.doAfterTextChanged { if(it.toString() != viewModel.state.value?.location) viewModel.onEvent(TaskEvent.OnLocationChange(it.toString())) }

            tietSubTask.doAfterTextChanged {
                if (!it.isNullOrBlank()) tilSubTask.error = null
            }

            btnAddSubTask.setOnClickListener {
                val text = tietSubTask.text.toString()
                if (text.isNotBlank()) {
                    viewModel.onEvent(TaskEvent.OnAddSubTask(text))
                    tietSubTask.text?.clear()
                    tilSubTask.error = null
                } else {
                    tilSubTask.error = "Escribe algo primero"
                }
            }

            btnGuardar.setOnClickListener { viewModel.onEvent(TaskEvent.OnSaveClicked) }
            btnCancelar.setOnClickListener { viewModel.onEvent(TaskEvent.OnCancelClicked) }
            btnAddReminder.setOnClickListener { viewModel.onEvent(TaskEvent.OnAddReminderClicked) }
            btnRecurrenceSelector.setOnClickListener { viewModel.onEvent(TaskEvent.OnRecurrenceSelectorClicked) }
            categoryBinding.containerCategorySelector.setOnClickListener { viewModel.onEvent(TaskEvent.OnCategorySelectorClicked) }

            chipGroupPriorityTask.setOnCheckedStateChangeListener { _, checkedIds ->
                val priority = when (checkedIds.firstOrNull()) {
                    com.manuelbena.synkron.R.id.chip_hight -> "Alta"
                    com.manuelbena.synkron.R.id.chip_small -> "Baja"
                    else -> "Media"
                }
                viewModel.onEvent(TaskEvent.OnPrioritySelected(priority))
            }
        }
    }

    private fun setupDateTimePickers() {
        binding.btnFecha.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener
            val picker = MaterialDatePicker.Builder.datePicker().setSelection(currentState.selectedDate.timeInMillis).setTitleText("Fecha").build()
            picker.addOnPositiveButtonClickListener { viewModel.onEvent(TaskEvent.OnDateSelected(it)) }
            picker.show(childFragmentManager, "DATE")
        }
        binding.btnHoraStart.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener
            showTimePicker(currentState.startTime) { h, m -> viewModel.onEvent(TaskEvent.OnStartTimeSelected(h, m)) }
        }
        binding.btnHoraEnd.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener
            showTimePicker(currentState.endTime) { h, m -> viewModel.onEvent(TaskEvent.OnEndTimeSelected(h, m)) }
        }
    }

    private fun showTimePicker(cal: Calendar, onTimeSelected: (Int, Int) -> Unit) {
        TimePickerDialog(requireContext(), { _, h, m -> onTimeSelected(h, m) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun setupTabsAndChips() {
        binding.tabLayoutTaskType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { tab?.position?.let { viewModel.onEvent(TaskEvent.OnTaskTypeChanged(it)) } }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        for (i in 0 until binding.chipGroupDays.childCount) {
            val chip = binding.chipGroupDays.getChildAt(i) as? Chip
            chip?.setOnCheckedChangeListener { _, isChecked -> if (chip.isPressed) viewModel.onEvent(TaskEvent.OnRecurrenceDayToggled(chip.tag.toString().toIntOrNull()?:0, isChecked)) }
        }
    }

    override fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state -> renderState(state) }

        viewModel.effect.observe(viewLifecycleOwner) { effect ->
            when (effect) {
                is TaskEffect.NavigateBack -> {
                    try { if (!findNavController().popBackStack()) requireActivity().finish() }
                    catch (e: Exception) { (requireActivity() as? ContainerActivity)?.closeWithSuccess() }
                }
                is TaskEffect.ShowMessage -> Snackbar.make(binding.root, effect.msg, Snackbar.LENGTH_SHORT).show()
                is TaskEffect.ShowCategoryDialog -> CategorySelectionDialog { sel -> viewModel.onEvent(TaskEvent.OnCategorySelected(sel.title, sel.googleColorId)) }.show(childFragmentManager, "CAT")

                is TaskEffect.ShowPriorityDialog -> { }

                is TaskEffect.ShowReminderDialog -> {
                    val s = viewModel.state.value ?: TaskState()
                    AddReminderDialog(s.startTime.clone() as Calendar, s.endTime.clone() as Calendar) { r ->
                        val m = when(r.method) { ReminderMethod.WHATSAPP -> "email"; ReminderMethod.ALARM -> "alarm"; else -> "popup" }
                        viewModel.onEvent(TaskEvent.OnAddReminder(com.manuelbena.synkron.domain.models.GoogleEventReminder(m, r.offsetMinutes, r.message)))
                    }.show(childFragmentManager, "REMINDER")
                }
                is TaskEffect.ShowRecurrenceDialog -> showRecurrenceOptionsDialog()
                else -> {}
            }
        }
    }

    private fun renderState(state: TaskState) {
        binding.apply {
            if (state.id != 0) { toolbar.title = "Editar Tarea"; btnGuardar.text = "Actualizar" } else { toolbar.title = "Nueva Tarea"; btnGuardar.text = "Guardar" }

            if (tietTitle.text.toString() != state.title) tietTitle.setText(state.title)
            if (tietDescription.text.toString() != state.description) tietDescription.setText(state.description)
            if (tietLocation.text.toString() != state.location) tietLocation.setText(state.location)

            btnFecha.text = state.selectedDate.timeInMillis.formatDate()
            btnHoraStart.text = state.startTime.timeInMillis.formatTime()
            btnHoraEnd.text = state.endTime.timeInMillis.formatTime()

            containerDuration.isVisible = !state.isNoDate
            containerDatetime.isVisible = !state.isAllDay && !state.isNoDate

            val tabIdx = when { state.isNoDate -> 2; state.isAllDay -> 1; else -> 0 }
            if (tabLayoutTaskType.selectedTabPosition != tabIdx) tabLayoutTaskType.getTabAt(tabIdx)?.select()

            val cat = CategoryType.getAll().find { it.title == state.category } ?: CategoryType.PERSONAL
            categoryBinding.tvSelectedCategoryName.text = cat.title
            categoryBinding.ivSelectedCategoryIcon.setImageResource(cat.iconRes)
            categoryBinding.viewSelectedCategoryBackground.background.setTint(androidx.core.content.ContextCompat.getColor(requireContext(), cat.colorRes))

            subtaskAdapter.updateItems(state.subTasks)
            binding.rvSubtareas.isVisible = state.subTasks.isNotEmpty()

            val uiReminders = state.reminders.map {
                val dTime = (state.startTime.clone() as Calendar).apply { add(Calendar.MINUTE, -it.minutes) }
                val mEnum = when(it.method?.lowercase()) { "email" -> ReminderMethod.WHATSAPP; "alarm" -> ReminderMethod.ALARM; else -> ReminderMethod.NOTIFICATION }
                ReminderItem(UUID.randomUUID().toString(), dTime.get(Calendar.HOUR_OF_DAY), dTime.get(Calendar.MINUTE), mEnum, it.minutes, dTime.timeInMillis.formatTime(), it.message?:"Recordatorio")
            }
            reminderAdapter.submitList(uiReminders)
            binding.rvReminders.isVisible = uiReminders.isNotEmpty()

            chipGroupDays.isVisible = state.recurrenceType == RecurrenceType.CUSTOM
            tvRecurrenceStatus.text = state.recurrenceType.name

            val pId = when(state.priority) { "Alta" -> com.manuelbena.synkron.R.id.chip_hight; "Baja" -> com.manuelbena.synkron.R.id.chip_small; else -> com.manuelbena.synkron.R.id.chip_medium }
            if (chipGroupPriorityTask.checkedChipId != pId) chipGroupPriorityTask.check(pId)
        }
    }

    private fun showRecurrenceOptionsDialog() {
        val ops = arrayOf("No se repite", "Todos los días", "Semanalmente", "Personalizado")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Repetir").setItems(ops) { _, w ->
            viewModel.onEvent(TaskEvent.OnRecurrenceTypeSelected(when(w){1->RecurrenceType.DAILY;2->RecurrenceType.WEEKLY;3->RecurrenceType.CUSTOM;else->RecurrenceType.NONE}))
        }.show()
    }
}