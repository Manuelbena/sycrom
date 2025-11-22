package com.manuelbena.synkron.presentation.task

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.R // Asegúrate de que este import sea correcto
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNewTaskBinding
import com.manuelbena.synkron.domain.models.*
import com.manuelbena.synkron.presentation.dialogs.AddReminderDialog
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.dialogs.ReminderManagerDialog
import com.manuelbena.synkron.presentation.dialogs.ReminderSelectionDialog
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.models.ReminderSelection
import com.manuelbena.synkron.presentation.models.ReminderType
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
    private var remindersList: MutableList<ReminderItem> = mutableListOf()
    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Estado de Fecha y Hora (Inicio)
    private var startCalendar: Calendar = Calendar.getInstance()

    // Categoría seleccionada (Por defecto: Personal)
    private var selectedCategory: CategoryType = CategoryType.PERSONAL

    // Estado del Recordatorio (Nuevo)
    private var currentReminder: ReminderSelection? = null

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
        // Las llamadas a setUI(), setListener() y observe() las maneja el BaseFragment
    }

    // endregion

    // region --- Configuración Inicial (Set UI) ---

    override fun setUI() {
        super.setUI()
        setupDefaultDateTime()
        setupSubtaskManager()
        updateCategoryUI(selectedCategory)
        currentReminder = ReminderSelection(isEnabled = false, label = "Sin recordatorio")
        setupReminderInlineList()
        updateReminderUI()
    }

    private fun setupDefaultDateTime() {
        startCalendar = Calendar.getInstance()
        updateDateButtonText()
        updateTimeButtonText()
    }

    // 1. CONFIGURAR EL RECYCLERVIEW EN LINEA
    private fun setupReminderInlineList() {
        // Inicializamos el adapter.
        // Al pulsar borrar (X) en una fila, lo quitamos de la lista y actualizamos la UI.
        inlineRemindersAdapter = ReminderManagerAdapter { itemToDelete ->
            remindersList.remove(itemToDelete)
            updateReminderUI() // Refresca la vista
        }

        binding.layoutnotifications.rvInlineReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = inlineRemindersAdapter
            // Importante para que no pelee con el ScrollView principal
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

    // 4. Actualizar UI con Badge real
    // 3. ACTUALIZAR LA UI (Adiós contador, Hola lista)
    private fun updateReminderUI() {
        val reminderView = binding.layoutnotifications

        if (remindersList.isNotEmpty()) {
            // A. Hay recordatorios

            // Mostramos la lista
            reminderView.rvInlineReminders.isVisible = true
            inlineRemindersAdapter.submitList(remindersList.toList())

            // Cambiamos el estilo de la cabecera para que se vea "activo"
            reminderView.tvHeaderTitle.text = "Recordatorios activos"
            reminderView.tvHeaderTitle.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.md_theme_onSurfaceVariant
                )
            ) // Ajusta tu color
            reminderView.ivHeaderIcon.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.md_theme_onSurfaceVariant
            ))

        } else {
            // B. No hay recordatorios

            // Ocultamos la lista
            reminderView.rvInlineReminders.isVisible = false

            // Estilo por defecto
            reminderView.tvHeaderTitle.text = "Añadir recordatorio"
            reminderView.tvHeaderTitle.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.md_theme_onSurfaceVariant
                )
            ) // Ajusta tu color
            reminderView.ivHeaderIcon.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.md_theme_onSurfaceVariant
                )
            )
        }
    }

    // 5. RECORDATORIOS (Corregido)
    // Mapeamos tu lista local de recordatorios al formato de Google
    val reminderOverrides = remindersList.map { item ->
        GoogleEventReminder(
            // Convertimos tu Enum (POPUP/EMAIL) al String que pide Google ("popup"/"email")
            method = if (item.method == ReminderMethod.EMAIL) "email" else "popup",
            // Usamos los minutos directamente (ya no existe timeOffset)
            minutes = item.minutes
        )
    }

    val reminders = GoogleEventReminders(
        useDefault = false,
        overrides = reminderOverrides
    )

    override fun observe() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.btnGuardar.isEnabled = state !is TaskContract.TaskState.Loading
            if (state is TaskContract.TaskState.Loading) {
                // Opcional: Mostrar loading
            }
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

            layoutnotifications.root.setOnClickListener {

                // CORRECCIÓN: Usamos 'AddReminderDialog' (el de crear) en lugar de 'ReminderManagerDialog' (el de listar).
                // No le pasamos 'currentReminder' porque es un recordatorio nuevo desde cero.

                val dialog = AddReminderDialog { newReminder ->

                    // 1. Añadir el nuevo recordatorio a tu lista local
                    remindersList.add(newReminder)

                    // 2. (Opcional) Ordenar por tiempo (minutos)
                    remindersList.sortBy { it.minutes }

                    // 3. Actualizar la UI
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
            .build()

        timePicker.addOnPositiveButtonClickListener {
            if (isStart) {
                startCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                startCalendar.set(Calendar.MINUTE, timePicker.minute)
                updateTimeButtonText()
            } else {
                // Lógica para hora fin si la necesitas guardar en otra variable
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
        // Por simplicidad, añadimos 1 hora si no se define fin
        val endCalendar = (startCalendar.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
        val endDateTime = endCalendar.toGoogleEventDateTime()

        // 3. Recursos de Categoría
        val iconNameStr = try {
            resources.getResourceEntryName(selectedCategory.iconRes)
        } catch (e: Exception) {
            "ic_label"
        }
        val colorNameStr = try {
            resources.getResourceEntryName(selectedCategory.colorRes)
        } catch (e: Exception) {
            "category_default"
        }
        val googleColorId = getGoogleColorId(selectedCategory)

        // 4. Subtareas
        val subTasksToSave = subtaskList.map {
            SubTaskDomain(id = UUID.randomUUID().toString(), title = it, isDone = false)
        }

        // 5. RECORDATORIOS (Lógica Dinámica)
        val reminderOverrides = mutableListOf<GoogleEventReminder>()

        if (currentReminder?.isEnabled == true) {
            // Convertimos millis a minutos para Google Calendar API
            val minutes = (currentReminder!!.timeOffset / 60000).toInt()

            // 'popup' es el método estándar para notificaciones en móvil
            reminderOverrides.add(GoogleEventReminder(minutes = minutes))
        }

        val reminders = GoogleEventReminders(
            useDefault = false,
            overrides = reminderOverrides
        )

        // 6. Crear Objeto Final
        val taskToSave = TaskDomain(
            id = 0L,
            summary = summary,
            description = description,
            location = location,
            colorId = googleColorId,
            start = startDateTime,
            end = endDateTime,
            reminders = reminders, // Usamos el dinámico
            transparency = "opaque",
            subTasks = subTasksToSave,
            typeTask = selectedCategory.title,
            priority = getSelectedPriority(),
            isActive = true,
            isDone = false,
            categoryIcon = iconNameStr,
            categoryColor = colorNameStr,
            // Campos vacíos requeridos por el modelo
            attendees = emptyList(),
            recurrence = emptyList(),
            conferenceLink = null
        )

        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))
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