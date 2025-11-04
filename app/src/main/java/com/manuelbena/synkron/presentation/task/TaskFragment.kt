package com.manuelbena.synkron.presentation.task

import android.annotation.SuppressLint
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.manuelbena.synkron.presentation.home.HomeEvent
import com.manuelbena.synkron.presentation.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date // <-- IMPORTANTE: Añadir import
import java.util.Locale
import java.util.TimeZone
import java.util.UUID // <-- IMPORTANTE: Añadir import
import kotlin.getValue

@AndroidEntryPoint
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()
    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: SubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    // --- MODIFICACIÓN 1: Guardamos los valores por defecto ---
    // Inicializamos con la fecha/hora actual
    private var selectedTimestamp: Long = System.currentTimeMillis()
    private var selectedTimeInMinutes: Int = 0
    // --- FIN MODIFICACIÓN 1 ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    // --- MODIFICACIÓN 2: Añadimos setUI para poner valores por defecto ---
    override fun setUI() {
        super.setUI()
        setupDefaultDateTime() // <-- Llamamos a la nueva función
        setupSubtaskManager() // Mover la configuración del subtask manager aquí
    }
    // --- FIN MODIFICACIÓN 2 ---

    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TaskEvent.InsertTask -> {
                    requireActivity().finish()
                }

                is TaskEvent.ErrorSavingTask -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                }

                TaskEvent.DeleteTask -> TODO()
                TaskEvent.UpdateTask -> TODO()
            }
        }
    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            btnHora.setOnClickListener {
                setupTimePicker()
            }
            btnFecha.setOnClickListener {
                setupDatePicker()
            }
            btnDuracion.setOnClickListener {
                setupDurationPicker()
            }
            chipGroupDuration.setOnCheckedStateChangeListener { group, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    btnDuracion.text = "Duracion"
                }
            }
            binding.btnGuardar.setOnClickListener {
                val title = binding.tvTitle.text.toString().trim()
                if (title.isEmpty()) {
                    binding.tvTitle.error = "El título no puede estar vacío"
                    return@setOnClickListener
                }

                val description = binding.tvDescription.text.toString().trim()

                // --- MODIFICACIÓN 3: Usamos las variables guardadas ---
                val dateTimestamp = selectedTimestamp
                val timeInMinutes = selectedTimeInMinutes
                // --- FIN MODIFICACIÓN 3 ---

                val durationInMinutes = getDurationInMinutes()
                val taskType = getSelectedTaskType()
                val place = binding.tvLocation.text.toString().trim()

                val subTasksToSave = subtaskList.map { subtaskTitle ->
                    // Usamos el ID de SubTaskDomain
                    SubTaskDomain(id = UUID.randomUUID().toString(), title = subtaskTitle, isDone = false)
                }

                val taskToSave = TaskDomain(
                    id = 0L, // Room generará el ID
                    title = title,
                    description = description,
                    date = dateTimestamp,
                    hour = timeInMinutes,
                    duration = durationInMinutes,
                    typeTask = taskType,
                    place = place,
                    subTasks = subTasksToSave,
                    isDone = false,
                    // --- MODIFICACIÓN 4: Añadido campo 'isActive' que faltaba ---
                    // Asumo que 'isActive' no es parte de tu TaskDomain basado en el zip,
                    // pero si lo es, asegúrate de que TaskDomain lo tiene.
                    // Si 'isActive' no existe en TaskDomain, elimina la línea de abajo.
                    // isActive = true
                    // --- FIN MODIFICACIÓN 4 ---

                    // Mirando tu TaskDomain.kt, sí tienes 'isActive'
                    isActive = true
                )

                viewModel.onSaveTask(taskToSave)
            }
            // setupSubtaskManager() // <- Se mueve a setUI
        }
    }

    // --- MODIFICACIÓN 5: Funciones para establecer y usar los valores por defecto ---

    private fun setupDefaultDateTime() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        // --- Fecha por defecto ---
        selectedTimestamp = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.btnFecha.text = dateFormat.format(Date(selectedTimestamp))

        // --- Hora por defecto ---
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        selectedTimeInMinutes = (currentHour * 60) + currentMinute
        val timeFormat = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)
        binding.btnHora.text = timeFormat
    }

    // Ya no necesitamos getDateTimestamp() ni getTimeInMinutes()
    // --- FIN MODIFICACIÓN 5 ---


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

        val durationText = binding.btnDuracion.text.toString()
        if (durationText.startsWith("Duración:", ignoreCase = true)) {
            return durationText.removePrefix("Duración:").trim().split(" ")[0].toIntOrNull() ?: 0
        }

        return 0
    }

    private fun getSelectedTaskType(): String {
        val checkedChipId = binding.chipGroupTypeTask.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            view?.findViewById<com.google.android.material.chip.Chip>(checkedChipId)?.text?.toString() ?: "Personal"
        } else {
            "Personal"
        }
    }

    private fun setupDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            // --- MODIFICACIÓN 6: Usar la variable guardada ---
            .setSelection(selectedTimestamp)
            // --- FIN MODIFICACIÓN 6 ---
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // --- MODIFICACIÓN 7: Actualizamos la variable guardada ---
            selectedTimestamp = selection
            // --- FIN MODIFICACIÓN 7 ---

            // El 'selection' es UTC, lo formateamos correctamente
            val date = Date(selection)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // Importante para que coincida con el picker
            binding.btnFecha.text = format.format(date)
        }

        datePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun setupTimePicker() {
        // --- MODIFICACIÓN 8: Usar la variable guardada ---
        val currentHour = selectedTimeInMinutes / 60
        val currentMinute = selectedTimeInMinutes % 60
        // --- FIN MODIFICACIÓN 8 ---

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Selecciona una hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val horaSeleccionada = timePicker.hour
            val minutoSeleccionado = timePicker.minute

            // --- MODIFICACIÓN 9: Actualizamos la variable guardada ---
            selectedTimeInMinutes = (horaSeleccionada * 60) + minutoSeleccionado
            // --- FIN MODIFICACIÓN 9 ---

            val horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", horaSeleccionada, minutoSeleccionado)
            binding.btnHora.text = horaFormateada
        }

        timePicker.show(childFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun setupDurationPicker() {
        binding.chipGroupDuration.clearCheck()
        val context = requireContext()

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

        val radioMinutos = RadioButton(context)
        radioMinutos.text = "Minutos"
        radioMinutos.id = View.generateViewId()
        radioMinutos.isChecked = true

        val radioHoras = RadioButton(context)
        radioHoras.text = "Horas"
        radioHoras.id = View.generateViewId()

        radioGroup.addView(radioMinutos)
        radioGroup.addView(radioHoras)
        container.addView(radioGroup)

        MaterialAlertDialogBuilder(context)
            .setTitle("Ingresar Duración")
            .setMessage("Escribe la duración y selecciona la unidad.")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val duracionStr = editText.text.toString()
                if (duracionStr.isNotEmpty()) {
                    try {
                        val valor = duracionStr.toInt()
                        val minutosTotales: Int

                        if (radioGroup.checkedRadioButtonId == radioHoras.id) {
                            minutosTotales = valor * 60
                        } else {
                            minutosTotales = valor
                        }
                        binding.btnDuracion.text = "Duración: $minutosTotales min"

                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Por favor ingresa un número válido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }


    private fun setupSubtaskManager() {
        subtaskAdapter = SubtaskAdapter(subtaskList,
            onRemoveClick = { position ->
                subtaskList.removeAt(position)
                subtaskAdapter.notifyItemRemoved(position)
                subtaskAdapter.notifyItemRangeChanged(position, subtaskList.size)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )
        binding.rvSubtareas.adapter = subtaskAdapter
        binding.rvSubtareas.layoutManager = LinearLayoutManager(requireContext())

        val callback = SubtaskTouchHelperCallback()
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        binding.btnAddSubTask.setOnClickListener {
            val subtaskText = binding.tvSubTask.text.toString().trim()
            if (subtaskText.isNotEmpty()) {
                subtaskList.add(subtaskText)
                subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
                binding.tvSubTask.text?.clear()
            } else {
                binding.tvSubTask.error = "La subtarea no puede estar vacía"
            }
        }

        binding.tvSubTask.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvSubTask.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private inner class SubtaskAdapter(
        private val items: MutableList<String>,
        private val onRemoveClick: (position: Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

        inner class SubtaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.tvSubtaskText)
            val removeButton: ImageButton = view.findViewById(R.id.ibRemoveSubtask)
            val dragHandle: ImageView = view.findViewById(R.id.ivDragHandle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_subtask_new, parent, false)
            return SubtaskViewHolder(view)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item

            holder.removeButton.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(currentPosition)
                }
            }

            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(holder)
                }
                false
            }
        }

        override fun getItemCount() = items.size
    }

    private inner class SubtaskTouchHelperCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun isLongPressDragEnabled() = false
        override fun isItemViewSwipeEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            Collections.swap(subtaskList, fromPosition, toPosition)
            subtaskAdapter.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }
    }
}

