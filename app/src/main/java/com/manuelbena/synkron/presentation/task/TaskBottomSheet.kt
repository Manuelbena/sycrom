package com.manuelbena.synkron.presentation.task

import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.FragmentTaskBottomSheetBinding
import com.manuelbena.synkron.databinding.ItemCategoryRowSelectorBinding
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.adapter.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapter.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.util.extensions.formatDate
import com.manuelbena.synkron.presentation.util.extensions.formatTime
import com.manuelbena.synkron.presentation.util.getCategoryColor
import com.manuelbena.synkron.presentation.util.getCategoryIcon
import com.manuelbena.synkron.presentation.util.getName
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class TaskBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: TaskViewModel by viewModels()
    private var _binding: FragmentTaskBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var reminderAdapter: ReminderManagerAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var categoryBinding: ItemCategoryRowSelectorBinding

    companion object {
        const val ARG_TASK = "arg_task"
        fun newInstance(task: TaskDomain? = null): TaskBottomSheet {
            val fragment = TaskBottomSheet()
            val args = Bundle()
            if (task != null) args.putParcelable(ARG_TASK, task)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTaskBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                // NOTA: Hemos quitado la altura forzada MATCH_PARENT para que se ajuste al contenido (WRAP_CONTENT)
            }
        }
        dialog.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewFlipperSteps.displayedChild > 0) {
                    navigateWizard(false)
                } else {
                    isEnabled = false
                    dialog.dismiss()
                }
            }
        })
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUI()
        observe()
    }

    private fun setUI() {
        val taskToEdit = arguments?.getParcelable<TaskDomain>(ARG_TASK)
        if (taskToEdit != null) {
            viewModel.onEvent(TaskEvent.OnLoadTaskForEdit(taskToEdit))
        }

        categoryBinding = ItemCategoryRowSelectorBinding.bind(binding.layoutCategorySelect.root)
        setupRecyclers()
        setupListeners()
        setupDateTimePickers()
        setupTabsAndChips()

        updateWizardUi(0)
    }

    private fun navigateWizard(next: Boolean) {
        hideKeyboard()
        val current = binding.viewFlipperSteps.displayedChild
        if (next) {
            if (current < 2) {
                if (current == 0 && binding.tietTitle.text.isNullOrBlank()) {
                    binding.tilTitle.error = "El título es obligatorio"
                    return
                }
                binding.tilTitle.error = null
                binding.viewFlipperSteps.setInAnimation(requireContext(), android.R.anim.slide_in_left)
                binding.viewFlipperSteps.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
                binding.viewFlipperSteps.showNext()
                updateWizardUi(binding.viewFlipperSteps.displayedChild)
            } else {
                viewModel.onEvent(TaskEvent.OnSaveClicked)
            }
        } else {
            if (current > 0) {
                binding.viewFlipperSteps.setInAnimation(requireContext(), android.R.anim.slide_in_left)
                binding.viewFlipperSteps.setOutAnimation(requireContext(), android.R.anim.slide_out_right)
                binding.viewFlipperSteps.showPrevious()
                updateWizardUi(binding.viewFlipperSteps.displayedChild)
            } else {
                dismiss()
            }
        }
    }

    private fun updateWizardUi(step: Int) {
        binding.apply {
            indicatorStep1.setBackgroundColor(if (step >= 0) Color.parseColor("#B388FF") else Color.parseColor("#E0E0E0"))
            indicatorStep2.setBackgroundColor(if (step >= 1) Color.parseColor("#B388FF") else Color.parseColor("#E0E0E0"))
            indicatorStep3.setBackgroundColor(if (step >= 2) Color.parseColor("#B388FF") else Color.parseColor("#E0E0E0"))

            btnCancelar.isVisible = step > 0
            if (step == 2) {
                btnGuardar.text = if (viewModel.state.value?.id != 0) "Actualizar" else "Guardar"
            } else {
                btnGuardar.text = "Siguiente"
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnCloseWizard.setOnClickListener { dismiss() }
            tietTitle.doAfterTextChanged { if(it.toString() != viewModel.state.value?.title) viewModel.onEvent(TaskEvent.OnTitleChange(it.toString())) }
            tietDescription.doAfterTextChanged { if(it.toString() != viewModel.state.value?.description) viewModel.onEvent(TaskEvent.OnDescriptionChange(it.toString())) }
            tietLocation.doAfterTextChanged { if(it.toString() != viewModel.state.value?.location) viewModel.onEvent(TaskEvent.OnLocationChange(it.toString())) }

            tietSubTask.doAfterTextChanged { if (!it.isNullOrBlank()) tilSubTask.error = null }
            btnAddSubTask.setOnClickListener {
                val text = tietSubTask.text.toString()

                if (text.isNotBlank()) {
                    // 1. Añadimos la tarea
                    viewModel.onEvent(TaskEvent.OnAddSubTask(text))

                    // 2. Limpiamos el campo
                    tietSubTask.text?.clear()
                    tilSubTask.error = null

                    // 3. CERRAR EL TECLADO AQUÍ
                    hideKeyboard()

                    // Opcional: Quitar el foco del campo de texto para que no parpadee el cursor
                    tietSubTask.clearFocus()

                } else {
                    tilSubTask.error = "Escribe algo primero"
                }
            }

            btnGuardar.setOnClickListener { navigateWizard(true) }
            btnCancelar.setOnClickListener { navigateWizard(false) }

            btnAddReminder.setOnClickListener { viewModel.onEvent(TaskEvent.OnAddReminderClicked) }
            btnRecurrenceSelector.setOnClickListener { viewModel.onEvent(TaskEvent.OnRecurrenceSelectorClicked) }
            categoryBinding.containerCategorySelector.setOnClickListener { viewModel.onEvent(TaskEvent.OnCategorySelectorClicked) }

            // --- LISTENERS DE PRIORIDAD (CUSTOM CARDS) ---
            cardPriorityHigh.setOnClickListener { viewModel.onEvent(TaskEvent.OnPrioritySelected("Alta")) }
            cardPriorityMedium.setOnClickListener { viewModel.onEvent(TaskEvent.OnPrioritySelected("Media")) }
            cardPriorityLow.setOnClickListener { viewModel.onEvent(TaskEvent.OnPrioritySelected("Baja")) }
        }
    }

    // --- LÓGICA VISUAL DE PRIORIDAD ---
    private fun updatePriorityUI(priority: String) {
        binding.apply {
            // Resetear estilos (por defecto deseleccionado: borde gris claro, fondo blanco)
            val defaultStroke = Color.parseColor("#EEEEEE")

            val highStroke = Color.parseColor("#D32F2F")

            val mediumStroke = Color.parseColor("#FBC02D")


            val lowStroke = Color.parseColor("#4CAF50")

            // Aplicar estilos según selección

            cardPriorityHigh.strokeColor = if (priority == "Alta") highStroke else defaultStroke

            cardPriorityMedium.strokeColor = if (priority == "Media") mediumStroke else defaultStroke

            cardPriorityLow.strokeColor = if (priority == "Baja") lowStroke else defaultStroke
        }
    }

    private fun renderState(state: TaskState) {
        binding.apply {
            if (state.id != 0) {
                try { toolbarTitle.text = "Editar Tarea" } catch (_: Exception) {}
            }
            if (viewFlipperSteps.displayedChild == 2) {
                btnGuardar.text = if (state.id != 0) "Actualizar" else "Guardar"
            }

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
            categoryBinding.tvSelectedCategoryName.text = state.category.getName()
            categoryBinding.ivSelectedCategoryIcon.setImageResource(state.category.getCategoryIcon())
            categoryBinding.viewSelectedCategoryBackground.backgroundTintList = ContextCompat.getColorStateList(requireContext(), state.category.getCategoryColor())

            subtaskAdapter.updateItems(state.subTasks)
            binding.rvSubtareas.isVisible = state.subTasks.isNotEmpty()

            val uiReminders = state.reminders.map {
                val dTime = (state.startTime.clone() as Calendar).apply { add(Calendar.MINUTE, -it.minutes) }
                val mEnum = when(it.method?.lowercase()) { "email" -> ReminderMethod.WHATSAPP; "alarm" -> ReminderMethod.ALARM; else -> ReminderMethod.NOTIFICATION }
                ReminderItem(UUID.randomUUID().toString(), dTime.get(Calendar.HOUR_OF_DAY), dTime.get(Calendar.MINUTE), mEnum, it.minutes, dTime.timeInMillis.formatTime(), it.message?:"Recordatorio")
            }
            reminderAdapter.submitList(uiReminders)
            binding.rvReminders.isVisible = uiReminders.isNotEmpty()

            val recurrenceText = when (state.recurrenceType) {
                RecurrenceType.NONE -> "Sin repetir"
                RecurrenceType.DAILY -> "Todos los días"
                RecurrenceType.WEEKLY -> "Semanalmente"
                RecurrenceType.CUSTOM -> "Personalizado"
                // Añade otros casos si tienes MONTHLY o YEARLY
                else -> "Sin repetir"
            }
            tvRecurrenceStatus.text = recurrenceText

            chipGroupDays.isVisible = state.recurrenceType == RecurrenceType.CUSTOM


            // --- LLAMADA A LA LÓGICA DE PRIORIDAD VISUAL ---
            updatePriorityUI(state.priority)
        }
    }

    // ... Resto de métodos (setupRecyclers, setupDateTimePickers, etc.) permanecen igual ...
    private fun setupRecyclers() {
        subtaskAdapter = TaskCreationSubtaskAdapter(
            onStartDrag = { holder -> itemTouchHelper.startDrag(holder) },
            onRemove = { item -> viewModel.onEvent(TaskEvent.OnRemoveSubTask(item)) },
            onReorder = { newItems -> viewModel.onEvent(TaskEvent.OnReorderSubTasks(newItems)) }
        )
        binding.rvSubtareas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subtaskAdapter
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
            val domainReminder = com.manuelbena.synkron.domain.models.GoogleEventReminder(method = methodString, minutes = reminderItem.offsetMinutes)
            viewModel.onEvent(TaskEvent.OnRemoveReminder(domainReminder))
        }
        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reminderAdapter
        }
    }

    private fun setupDateTimePickers() {
        // --- FECHA (DatePicker) ---
        binding.btnFecha.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener

            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(currentState.selectedDate.timeInMillis)
                .setTitleText("Seleccionar fecha")
                .setTheme(R.style.ThemeOverlay_App_DatePicker) // <--- APLICAMOS EL TEMA AQUÍ
                .build()

            picker.addOnPositiveButtonClickListener { viewModel.onEvent(TaskEvent.OnDateSelected(it)) }
            picker.show(childFragmentManager, "DATE")
        }

        // --- HORA INICIO (TimePicker) ---
        binding.btnHoraStart.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener
            showMaterialTimePicker(currentState.startTime) { h, m ->
                viewModel.onEvent(TaskEvent.OnStartTimeSelected(h, m))
            }
        }

        // --- HORA FIN (TimePicker) ---
        binding.btnHoraEnd.setOnClickListener {
            val currentState = viewModel.state.value ?: return@setOnClickListener
            showMaterialTimePicker(currentState.endTime) { h, m ->
                viewModel.onEvent(TaskEvent.OnEndTimeSelected(h, m))
            }
        }
    }

    // Nueva función auxiliar que usa MaterialTimePicker en lugar del antiguo TimePickerDialog
    private fun showMaterialTimePicker(cal: Calendar, onTimeSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(cal.get(Calendar.HOUR_OF_DAY))
            .setMinute(cal.get(Calendar.MINUTE))
            .setTitleText("Seleccionar hora")
            .setTheme(R.style.ThemeOverlay_App_TimePicker) // <--- APLICAMOS EL TEMA AQUÍ
            .setInputMode(com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "TIME")
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

    private fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state -> renderState(state) }
        viewModel.effect.observe(viewLifecycleOwner) { effect ->
            when (effect) {
                is TaskEffect.NavigateBack -> dismiss()
                is TaskEffect.ShowMessage -> Snackbar.make(binding.root, effect.msg, Snackbar.LENGTH_SHORT).show()
                is TaskEffect.ShowCategoryDialog -> CategorySelectionDialog { sel -> viewModel.onEvent(TaskEvent.OnCategorySelected(sel.title, sel.googleColorId)) }.show(childFragmentManager, "CAT")
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

    private fun showRecurrenceOptionsDialog() {
        val ops = arrayOf("No se repite", "Todos los días", "Semanalmente", "Personalizado")

        // AÑADIDO: Pasamos el tema personalizado en el constructor
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
            .setTitle("Repetir")
            .setItems(ops) { _, w ->
                viewModel.onEvent(
                    TaskEvent.OnRecurrenceTypeSelected(
                        when (w) {
                            1 -> RecurrenceType.DAILY
                            2 -> RecurrenceType.WEEKLY
                            3 -> RecurrenceType.CUSTOM
                            else -> RecurrenceType.NONE
                        }
                    )
                )
            }
            .show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        val currentFocus = view?.findFocus() ?: View(requireContext())
        imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        currentFocus.clearFocus() // Opcional: quita el foco para que no parpadee el cursor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}