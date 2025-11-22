package com.manuelbena.synkron.presentation.task

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.manuelbena.synkron.domain.models.*
import com.manuelbena.synkron.presentation.dialogs.CategorySelectionDialog
import com.manuelbena.synkron.presentation.models.CategoryType
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

    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: TaskCreationSubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Estado de Fecha y Hora (Inicio)
    private var startCalendar: Calendar = Calendar.getInstance()

    // Duración personalizada
    private var selectedDurationInMinutes: Int = 0

    // Categoría seleccionada (Por defecto: Personal)
    private var selectedCategory: CategoryType = CategoryType.PERSONAL

    // endregion

    // region --- Ciclo de Vida & ViewBinding ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
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
        updateCategoryUI(selectedCategory) // Inicializa la vista de categoría
    }

    private fun setupDefaultDateTime() {
        startCalendar = Calendar.getInstance()
        updateDateButtonText()

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
        // Actualizamos el include 'layoutCategorySelect' con los datos de la categoría seleccionada
        binding.layoutCategorySelect.apply {
            tvSelectedCategoryName.text = category.title
            ivSelectedCategoryIcon.setImageResource(category.iconRes)

            // Color dinámico
            val color = ContextCompat.getColor(requireContext(), category.colorRes)
            tvSelectedCategoryName.setTextColor(color) // Opcional: colorear el texto
            viewSelectedCategoryBackground.backgroundTintList = ColorStateList.valueOf(color)
        }
    }

    // endregion

    // region --- Observables (ViewModel) ---

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

    // endregion

    // region --- Listeners ---

    override fun setListener() {
        super.setListener()
        binding.apply {
            // Pickers
            btnHoraStart.setOnClickListener { showTimePicker() }
            btnHoraEnd.setOnClickListener { showTimePicker() }
            btnFecha.setOnClickListener { showDatePicker() }



            // Subtareas
            btnAddSubTask.setOnClickListener { addSubtask() }

            // --- NUEVO: Listener para el diálogo de Categorías ---
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
            startCalendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH)
            )
            updateDateButtonText()
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(startCalendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(startCalendar.get(Calendar.MINUTE))
            .setTitleText("Selecciona una hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            startCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            startCalendar.set(Calendar.MINUTE, timePicker.minute)

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
            binding.tilSubTask.error = "La subtarea no puede estar vacía"
        }
    }

    // endregion

    // region --- Guardado de Datos ---

    private fun gatherDataAndSave() {
        // 1. Validación Básica
        val summary = binding.tietTitle.text.toString().trim()
        if (summary.isEmpty()) {
            binding.tilTitle.error = "El título es obligatorio"
            return
        }
        binding.tilTitle.error = null

        val description = binding.tietDescription.text.toString().trim()
        val location = binding.tietLocation.text.toString().trim()

        // 2. Obtener Fechas y Duración
        val duration = 0
        val startDateTime = startCalendar.toGoogleEventDateTime()
        val endCalendar = (startCalendar.clone() as Calendar).apply { add(Calendar.MINUTE, duration) }
        val endDateTime = endCalendar.toGoogleEventDateTime()

        // 3. Mapear Categoría (AQUÍ ESTÁ LA CLAVE 💡)
        // Usamos 'resources.getResourceEntryName' para convertir el Int (R.drawable.x) a String ("x")
        val iconNameStr = try {
            resources.getResourceEntryName(selectedCategory.iconRes)
        } catch (e: Exception) {
            "ic_label" // Fallback por seguridad
        }

        val colorNameStr = try {
            resources.getResourceEntryName(selectedCategory.colorRes)
        } catch (e: Exception) {
            "category_default" // Fallback por seguridad
        }

        val taskType = selectedCategory.title
        val googleColorId = getGoogleColorId(selectedCategory)

        // 4. Construir Modelos Anidados
        val subTasksToSave = subtaskList.map {
            SubTaskDomain(id = UUID.randomUUID().toString(), title = it, isDone = false)
        }
        val reminders = GoogleEventReminders(
            useDefault = false,
            overrides = listOf(GoogleEventReminder(minutes = 10))
        )

        // 5. Crear Objeto Final
        val taskToSave = TaskDomain(
            id = 0L,
            summary = summary,
            description = description,
            location = location,
            colorId = googleColorId,
            start = startDateTime,
            end = endDateTime,
            attendees = emptyList(),
            recurrence = emptyList(),
            reminders = reminders,
            transparency = "opaque",
            conferenceLink = null,
            subTasks = subTasksToSave,
            typeTask = taskType,
            priority = getSelectedPriority(),
            isActive = true,
            isDone = false,

            // --- NUEVOS CAMPOS ---
            // Pasamos los nombres (Strings) que acabamos de extraer
            categoryIcon = iconNameStr,
            categoryColor = colorNameStr
        )

        viewModel.onEvent(TaskContract.TaskEvent.OnSaveTask(taskToSave))
    }
    private fun updateDateButtonText() {
        binding.btnFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startCalendar.time)
    }


    private fun getSelectedPriority(): String {
        return when {
            binding.chipHight.isChecked -> "Alta"
            binding.chipMedium.isChecked -> "Media"
            binding.chipSmall.isChecked -> "Baja"
            else -> "Media"
        }
    }

    /**
     * Mapea el Enum local CategoryType a los IDs de color de Google Calendar.
     * Estos son los valores que tenías hardcodeados en el 'when' anterior.
     */
    private fun getGoogleColorId(category: CategoryType): String {
        return when (category) {
            CategoryType.WORK -> "9"      // Blueberry
            CategoryType.STUDY -> "11"    // Tomato
            CategoryType.HEALTH -> "10"   // Basil
            CategoryType.FINANCE -> "8"   // Graphite (Usado para 'Otro' antes, ajústalo si quieres)
            CategoryType.PERSONAL -> "2"  // Sage
            else -> "2"
        }
    }

    // endregion
}