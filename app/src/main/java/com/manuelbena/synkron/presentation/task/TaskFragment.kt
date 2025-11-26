package com.manuelbena.synkron.presentation.task

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNewTaskBinding
import com.manuelbena.synkron.domain.models.*
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.task.adapters.SubtaskTouchHelperCallback
import com.manuelbena.synkron.presentation.task.adapters.TaskCreationSubtaskAdapter
import com.manuelbena.synkron.presentation.util.toGoogleEventDateTime
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()

    // region --- Variables Locales & Estado ---
    private lateinit var inlineRemindersAdapter: ReminderManagerAdapter

    // ESTA es la lista que manda ahora:
    private var remindersList: MutableList<ReminderItem> = mutableListOf()

    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Estado de Fecha y Hora (Inicio)
    private var startCalendar: Calendar = Calendar.getInstance()

    // Categoría seleccionada (Por defecto: Personal)
    private var selectedCategory: CategoryType = CategoryType.PERSONAL
    // endregion

    // region --- Ciclo de Vida & ViewBinding ---

    override fun inflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // BaseFragment llama a setUI(), setListener() y observe()
    }

    // endregion

    // region --- Configuración Inicial (Set UI) ---

    override fun setUI() {
        super.setUI()
        setupDefaultDateTime()
        setupSubtaskManager()
        updateCategoryUI(selectedCategory)
        setupReminderInlineList()
        updateReminderUI()
        setupRecurrenceLogic()
    }

    private fun setupDefaultDateTime() {
        startCalendar = Calendar.getInstance()
        updateDateButtonText()
        updateTimeButtonText()
    }

    // 1. CONFIGURAR EL RECYCLERVIEW DE RECORDATORIOS
    private fun setupReminderInlineList() {
        // Adapter para pintar la lista visualmente
        inlineRemindersAdapter = ReminderManagerAdapter { itemToDelete ->
            remindersList.remove(itemToDelete)
            updateReminderUI() // Si borramos uno, refrescamos la UI
        }

        // Aseguramos que el include 'layoutnotifications' tenga el RecyclerView 'rvInlineReminders'
        binding.layoutnotifications.rvInlineReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inlineRemindersAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSubtaskManager() {
        subtaskAdapter = TaskCreationSubtaskAdapter(subtaskList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        binding.rvSubtareas.apply {
            adapter = subtaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val callback = SubtaskTouchHelperCallback(subtaskAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)
    }

    private fun updateCategoryUI(category: CategoryType) {
        binding.layoutCategorySelect.apply {
            tvSelectedCategoryName.text = category.title
            ivSelectedCategoryIcon.setImageResource(category.iconRes)

            val color = ContextCompat.getColor(requireContext(), category.colorRes)
            viewSelectedCategoryBackground.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    // 3. ACTUALIZAR LA UI DE RECORDATORIOS
    private fun updateReminderUI() {
        val reminderView = binding.layoutnotifications

        // ¿Tenemos recordatorios en la lista?
        if (remindersList.isNotEmpty()) {
            // SI: Mostramos la lista y actualizamos cabecera
            reminderView.rvInlineReminders.isVisible = true
            inlineRemindersAdapter.submitList(remindersList.toList())

            reminderView.tvHeaderTitle.text = "Recordatorios activos (${remindersList.size})"
            reminderView.tvHeaderTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant))
            reminderView.ivHeaderIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant))



        } else {
            // NO: Ocultamos lista y ponemos cabecera en gris
            reminderView.rvInlineReminders.isVisible = false

            reminderView.tvHeaderTitle.text = "Añadir recordatorio"
            reminderView.tvHeaderTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant))
            reminderView.ivHeaderIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant))
  
        }
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
                else -> {}
            }
        }
    }

    // endregion

    // region --- Listeners ---

    override fun setListener() {
        super.setListener()
        binding.apply {
            // Pickers
            btnHoraStart.setOnClickListener { showTimePicker(true) }
            btnHoraEnd.setOnClickListener { showTimePicker(false) }
            btnFecha.setOnClickListener { showDatePicker() }

            // --- CLICK EN AÑADIR RECORDATORIO ---
            layoutnotifications.root.setOnClickListener {
                val dialog = AddReminderDialog(startCalendar) { reminder ->
                    // 1. Añadir a la lista
                    remindersList.add(reminder)

                    // 2. Actualizar la UI
                    updateReminderUI()


                }
                dialog.show(parentFragmentManager, AddReminderDialog.TAG)
            }

            // Subtareas
            btnAddSubTask.setOnClickListener { addSubtask() }

            // Categorías
            layoutCategorySelect.containerCategorySelector.setOnClickListener {
                showCategorySelectionDialog()
            }

            // Acciones principales
            btnGuardar.setOnClickListener { gatherDataAndSave() }
            btnCancelar.setOnClickListener { requireActivity().finish() }
        }
    }

    // endregion

    // region --- Lógica de Negocio (Pickers & Dialogs) ---

    private fun showCategorySelectionDialog() {
        val dialog = CategorySelectionDialog { newCategory ->
            selectedCategory = newCategory
            updateCategoryUI(newCategory)
        }
        dialog.show(parentFragmentManager, CategorySelectionDialog.TAG)
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(startCalendar.timeInMillis)
            .setTheme(R.style.SynkronDatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }
            startCalendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
            startCalendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
            startCalendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
            updateDateButtonText()
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(isStart: Boolean) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(startCalendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(startCalendar.get(Calendar.MINUTE))
            .setTitleText(if (isStart) "Hora Inicio" else "Hora Fin")
            .setTheme(R.style.SynkronTimePicker)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            if (isStart) {
                startCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                startCalendar.set(Calendar.MINUTE, timePicker.minute)
                updateTimeButtonText()
            } else {
                // Lógica para hora fin
            }
        }
        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun addSubtask() {
        val text = binding.tietSubTask.text.toString().trim()
        if (text.isNotEmpty()) {
            subtaskList.add(text)
            subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
            binding.tietSubTask.text?.clear()
            binding.tilSubTask.error = null
        } else {
            binding.tilSubTask.error = "Escribe algo primero"
        }
    }

    // endregion

    // region --- Guardado de Datos ---

    private fun gatherDataAndSave() {
        // 1. Validación Básica
        val summary = binding.tietTitle.text.toString().trim()
        if (summary.isEmpty()) {
            binding.tilTitle.error = "Título requerido"
            return
        }
        binding.tilTitle.error = null

        val description = binding.tietDescription.text.toString().trim()
        val location = binding.tietLocation.text.toString().trim()

        // 2. Fechas
        val startDateTime = startCalendar.toGoogleEventDateTime()
        val endCalendar = (startCalendar.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
        val endDateTime = endCalendar.toGoogleEventDateTime()

        // 3. Recursos de Categoría
        val iconNameStr = try { resources.getResourceEntryName(selectedCategory.iconRes) } catch (e: Exception) { "ic_label" }
        val colorNameStr = try { resources.getResourceEntryName(selectedCategory.colorRes) } catch (e: Exception) { "category_default" }
        val googleColorId = getGoogleColorId(selectedCategory)

        // 4. Subtareas
        val subTasksToSave = subtaskList.map {
            SubTaskDomain(id = UUID.randomUUID().toString(), title = it, isDone = false)
        }

        // 5. RECORDATORIOS (Lógica limpia basada en LISTA)
        // Si la lista está vacía, podemos mandar null o default.
        // Si tiene items, los transformamos a GoogleEventReminder.
        val remindersToSave = if (remindersList.isNotEmpty()) {
            val overrides = remindersList.map { uiReminder ->
                GoogleEventReminder(
                    method = uiReminder.method.name.lowercase(), // "popup", "email"
                    minutes = uiReminder.minutes
                )
            }
            GoogleEventReminders(useDefault = false, overrides = overrides)
        } else {
            // Si no hay recordatorios manuales, usamos el defecto de Google (10 min antes usualmente)
            GoogleEventReminders(useDefault = true, overrides = emptyList())
        }

        // 6. Crear Objeto Final
        val taskToSave = TaskDomain(
            id = 0L,
            summary = summary,
            description = description,
            location = location,
            colorId = googleColorId,
            start = startDateTime,
            end = endDateTime,
            reminders = remindersToSave, // <--- AQUÍ USAMOS LA VARIABLE NUEVA
            transparency = "opaque",
            subTasks = subTasksToSave,
            typeTask = selectedCategory.title,
            priority = getSelectedPriority(),
            isActive = true,
            isDone = false,
            categoryIcon = iconNameStr,
            categoryColor = colorNameStr,
            attendees = emptyList(),
            recurrence = emptyList(),
            conferenceLink = null,
            synkronRecurrence = viewModel.recurrenceState.value ?: RecurrenceType.NONE,
            synkronRecurrenceDays = if (viewModel.recurrenceState.value == RecurrenceType.WEEKLY) getSelectedDays() else emptyList()
        )

        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))

    }
    // --- LÓGICA DE RECURRENCIA ---
    private fun setupRecurrenceLogic() {
        // 1. Click en el selector
        binding.btnRecurrenceSelector.setOnClickListener { view ->
            showRecurrenceMenu(view)
        }

        // 2. Observar cambios para actualizar UI
        viewModel.recurrenceState.observe(viewLifecycleOwner) { type ->
            binding.tvRecurrenceStatus.text = when(type) {
                RecurrenceType.NONE -> "No se repite"
                RecurrenceType.DAILY -> "Todos los días"
                RecurrenceType.WEEKLY -> "Semanalmente"
                RecurrenceType.MONTHLY -> "Mensualmente"
                RecurrenceType.YEARLY -> "Anualmente"
                else -> "No se repite"
            }

            // Mostrar/Ocultar Chips de días
            if (type == RecurrenceType.WEEKLY) {
                binding.chipGroupDays.visibility = View.VISIBLE
            } else {
                binding.chipGroupDays.visibility = View.GONE
            }
        }
    }

    private fun showRecurrenceMenu(view: View) {
        val popup = PopupMenu(requireContext(), binding.tvRecurrenceStatus)
        // Añadimos las opciones
        popup.menu.add(0, 0, 0, "No se repite")
        popup.menu.add(0, 1, 1, "Todos los días")
        popup.menu.add(0, 2, 2, "Semanalmente") // Esta activa los chips
        popup.menu.add(0, 3, 3, "Mensualmente")
        popup.menu.add(0, 4, 4, "Anualmente")

        popup.setOnMenuItemClickListener { item ->
            val selected = when(item.itemId) {
                1 -> RecurrenceType.DAILY
                2 -> RecurrenceType.WEEKLY
                3 -> RecurrenceType.MONTHLY
                4 -> RecurrenceType.YEARLY
                else -> RecurrenceType.NONE
            }
            viewModel.setRecurrence(selected)
            true
        }
        popup.show()
    }

    // Función auxiliar para sacar los días seleccionados de los Chips
    private fun getSelectedDays(): List<Int> {
        val days = mutableListOf<Int>()
        // checkedChipIds devuelve los IDs de las vistas seleccionadas
        binding.chipGroupDays.checkedChipIds.forEach { id ->
            val chip = binding.chipGroupDays.findViewById<Chip>(id)
            // Convertimos el tag "1" a entero.
            // IMPORTANTE: Definimos android:tag="1" en el XML
            try {
                days.add(chip.tag.toString().toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return days
    }

    private fun updateDateButtonText() {
        val format = SimpleDateFormat("EEE dd MMM", Locale("es", "ES"))
        binding.btnFecha.text = format.format(startCalendar.time).capitalize()
    }

    private fun updateTimeButtonText() {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.btnHoraStart.text = format.format(startCalendar.time)
    }

    private fun getSelectedPriority(): String {
        return when {
            binding.chipHight.isChecked -> "Alta"
            binding.chipMedium.isChecked -> "Media"
            binding.chipSmall.isChecked -> "Baja"
            else -> "Media"
        }
    }

    private fun getGoogleColorId(category: CategoryType): String {
        return when (category) {
            CategoryType.WORK -> "9"
            CategoryType.STUDY -> "11"
            CategoryType.HEALTH -> "10"
            CategoryType.FINANCE -> "8"
            CategoryType.PERSONAL -> "2"
            else -> "2"
        }
    }
    // endregion
}