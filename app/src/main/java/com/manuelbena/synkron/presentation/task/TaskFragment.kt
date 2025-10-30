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
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.home.HomeEvent
import com.manuelbena.synkron.presentation.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import kotlin.getValue

@AndroidEntryPoint // <-- 2. ANOTA LA CLASE
class TaskFragment : BaseFragment<FragmentNewTaskBinding, TaskViewModel>() {

    override val viewModel: TaskViewModel by viewModels()
    // Lista de datos y Adaptador
    private val subtaskList = mutableListOf<String>()
    private lateinit var subtaskAdapter: SubtaskAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNewTaskBinding {
        return FragmentNewTaskBinding.inflate(inflater, container, false)
    }

    override fun observe() {

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is TaskEvent.InsertTask -> {
                    requireActivity().finish()
                }

                is TaskEvent.ErrorSavingTask -> {
                    // Mostrar error al usuario
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
                // checkedIds es una lista de los IDs de los chips seleccionados.
                // Si la lista no está vacía, significa que el usuario ha seleccionado un chip.
                if (checkedIds.isNotEmpty()) {
                    // Restablecer el texto del botón de duración a su estado original
                    btnDuracion.text = "Duracion"
                }
            }
            binding.btnGuardar.setOnClickListener {
                // Validación básica del título
                val title = binding.tvTitle.text.toString().trim()
                if (title.isEmpty()) {
                    binding.tvTitle.error = "El título no puede estar vacío"
                    return@setOnClickListener
                }

                // 1. Recolectar todos los datos del formulario
                val description = binding.tvDescription.text.toString().trim()
                val dateTimestamp = getDateTimestamp()
                val timeInMinutes = getTimeInMinutes()
                val durationInMinutes = getDurationInMinutes()
                val taskType = getSelectedTaskType()
                val place = binding.tvLocation.text.toString().trim()

                // --- INICIO DE LA CORRECCIÓN ---
                // Convertir la lista de Strings a List<SubTaskPresentation>
                val subTasksToSave = subtaskList.map { subtaskTitle ->
                    SubTaskPresentation(title = subtaskTitle, isDone = false)
                }
                // --- FIN DE LA CORRECCIÓN ---

                // 2. Crear el objeto TaskDomain con los datos reales
                val taskToSave = TaskDomain(
                    title = title,
                    description = description,
                    date = dateTimestamp,
                    hour = timeInMinutes,
                    duration = durationInMinutes,
                    typeTask = taskType,
                    place = place,
                    subTasks = subTasksToSave, // <-- Usar la lista convertida
                    isActive = true, // O la lógica que corresponda
                    isDone = false
                )

                // 3. Enviar el objeto al ViewModel
                viewModel.onSaveTask(taskToSave)
            }
            setupSubtaskManager()
        }
    }

    /**
     * Obtiene el timestamp en milisegundos de la fecha seleccionada en el botón.
     * Devuelve 0 si no se ha seleccionado ninguna fecha.
     */
    private fun getDateTimestamp(): Long {
        val dateText = binding.btnFecha.text.toString()
        if (dateText.equals("Fecha", ignoreCase = true)) {
            // Si no se selecciona fecha, usar la de hoy por defecto
            return MaterialDatePicker.todayInUtcMilliseconds()
        }
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.parse(dateText)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
        } catch (e: Exception) {
            MaterialDatePicker.todayInUtcMilliseconds()
        }
    }

    /**
     * Obtiene la hora seleccionada en el botón y la convierte a minutos totales desde la medianoche.
     * Devuelve 0 si no se ha seleccionado ninguna hora.
     */
    private fun getTimeInMinutes(): Int {
        val timeText = binding.btnHora.text.toString()
        if (timeText.equals("Hora", ignoreCase = true)) {
            return 0 // 0 minutos = 00:00
        }
        return try {
            val parts = timeText.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            (hours * 60) + minutes
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Obtiene la duración en minutos, ya sea de los chips o del diálogo personalizado.
     * Devuelve 0 si no se ha establecido ninguna duración.
     */
    private fun getDurationInMinutes(): Int {
        // Primero, revisamos si hay un chip seleccionado
        val checkedChipId = binding.chipGroupDuration.checkedChipId
        if (checkedChipId != View.NO_ID) {
            return when (checkedChipId) {
                R.id.chip_15_min -> 15
                R.id.chip_30_min -> 30
                R.id.chip_60_min -> 45 // <-- OJO: Este chip dice 60_min pero el texto es 45 Minitos
                R.id.chip_1_hour -> 60
                R.id.chip_5_hour -> 300 // 5 * 60
                R.id.chip_8_hour -> 480 // 8 * 60
                else -> 0
            }
        }

        // Si no hay chip, revisamos el texto del botón del diálogo personalizado
        val durationText = binding.btnDuracion.text.toString()
        if (durationText.startsWith("Duración:", ignoreCase = true)) {
            // Extrae el número del texto "Duración: 120 min"
            return durationText.removePrefix("Duración:").trim().split(" ")[0].toIntOrNull() ?: 0
        }

        return 0
    }

    /**
     * Obtiene el tipo de tarea del ChipGroup correspondiente.
     * Devuelve "Personal" si no hay nada seleccionado (coincide con chip_personal checked=true).
     */
    private fun getSelectedTaskType(): String {
        val checkedChipId = binding.chipGroupTypeTask.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            view?.findViewById<com.google.android.material.chip.Chip>(checkedChipId)?.text?.toString() ?: "Personal"
        } else {
            "Personal" // Valor por defecto si nada está seleccionado
        }
    }

    private fun setupDatePicker() {

        // Crear el MaterialDatePicker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona una fecha")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Fecha por defecto
            .build()

        // Listener para cuando el usuario presiona "OK"
        datePicker.addOnPositiveButtonClickListener { selection ->
            // 'selection' es un Long (timestamp en milisegundos UTC)
            // Lo convertimos a una fecha legible
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFormateada = format.format(calendar.time)

            // Actualizamos el texto del botón
            binding.btnFecha.text = fechaFormateada
        }

        // Mostrar el DatePicker
        datePicker.show(childFragmentManager, "MATERIAL_DATE_PICKER")

    }

    private fun setupTimePicker() {

        // Obtenemos la hora y minuto actuales para el valor por defecto
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Crear el MaterialTimePicker
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H) // Formato 24h (o CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Selecciona una hora")
            .build()

        // Listener para cuando el usuario presiona "OK"
        timePicker.addOnPositiveButtonClickListener {
            val horaSeleccionada = timePicker.hour
            val minutoSeleccionado = timePicker.minute

            // Formateamos la hora (ej: "14:30")
            val horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", horaSeleccionada, minutoSeleccionado)

            // Actualizamos el texto del botón
            binding.btnHora.text = horaFormateada
        }

        // Mostrar el TimePicker
        timePicker.show(childFragmentManager, "MATERIAL_TIME_PICKER")

    }

    private fun setupDurationPicker() {
        binding.chipGroupDuration.clearCheck()
        // 1. Crear el layout y el EditText para el input numérico
        val context = requireContext()

        // Contenedor principal
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        val padding = (20 * context.resources.displayMetrics.density).toInt() // 20dp
        container.setPadding(padding, 0, padding, 0)

        // Campo de texto para el número
        val textInputLayout = TextInputLayout(context)
        val editText = TextInputEditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.hint = "Valor" // Hint genérico
        textInputLayout.addView(editText)
        container.addView(textInputLayout)

        // RadioGroup para seleccionar la unidad (Minutos / Horas)
        val radioGroup = RadioGroup(context)
        radioGroup.orientation = RadioGroup.HORIZONTAL

        val radioMinutos = RadioButton(context)
        radioMinutos.text = "Minutos"
        radioMinutos.id = View.generateViewId() // ID único
        radioMinutos.isChecked = true // Opción por defecto

        val radioHoras = RadioButton(context)
        radioHoras.text = "Horas"
        radioHoras.id = View.generateViewId() // ID único

        radioGroup.addView(radioMinutos)
        radioGroup.addView(radioHoras)
        container.addView(radioGroup)

        // 2. Crear el MaterialAlertDialog
        MaterialAlertDialogBuilder(context)
            .setTitle("Ingresar Duración")
            .setMessage("Escribe la duración y selecciona la unidad.")
            // Usamos setView para añadir el layout personalizado
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { dialog, _ ->
                val duracionStr = editText.text.toString()
                if (duracionStr.isNotEmpty()) {
                    try {
                        val valor = duracionStr.toInt()
                        val minutosTotales: Int

                        if (radioGroup.checkedRadioButtonId == radioHoras.id) {
                            // Si seleccionó "Horas", convertimos a minutos
                            minutosTotales = valor * 60
                        } else {
                            // Si seleccionó "Minutos" (o por defecto)
                            minutosTotales = valor
                        }

                        // 3. Actualizar el texto del botón
                        binding.btnDuracion.text = "Duración: $minutosTotales min"

                    } catch (e: NumberFormatException) {
                        // Manejar input inválido
                        Toast.makeText(context, "Por favor ingresa un número válido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()


    }


    // 10. NUEVA SECCIÓN: Lógica para manejar las subtareas
    private fun setupSubtaskManager() {
        // 1. Configurar el adaptador y el RecyclerView
        subtaskAdapter = SubtaskAdapter(subtaskList,
            // Lambda para manejar la eliminación
            onRemoveClick = { position ->
                subtaskList.removeAt(position)
                subtaskAdapter.notifyItemRemoved(position)
                // Notificamos el cambio en el rango para actualizar posiciones
                subtaskAdapter.notifyItemRangeChanged(position, subtaskList.size)
            },
            // Lambda para iniciar el drag
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            }
        )
        binding.rvSubtareas.adapter = subtaskAdapter
        binding.rvSubtareas.layoutManager = LinearLayoutManager(requireContext())

        // 2. Configurar el ItemTouchHelper para drag-and-drop
        val callback = SubtaskTouchHelperCallback()
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.rvSubtareas)

        // 3. Configurar el botón de "Agregar subtarea"
        binding.btnAddSubTask.setOnClickListener {
            val subtaskText = binding.tvSubTask.text.toString().trim()
            if (subtaskText.isNotEmpty()) {
                // Añadir a la lista
                subtaskList.add(subtaskText)
                // Notificar al adaptador
                subtaskAdapter.notifyItemInserted(subtaskList.size - 1)
                // Limpiar el campo de texto
                binding.tvSubTask.text?.clear()
                // Opcional: Ocultar el teclado
                // (Implementación de ocultar teclado omitida por brevedad)
            } else {
                binding.tvSubTask.error = "La subtarea no puede estar vacía"
            }
        }

        // Quitar el error cuando el usuario empieza a escribir
        binding.tvSubTask.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvSubTask.error = null // Limpiar error al escribir
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    // 11. NUEVO: Adaptador para el RecyclerView de Subtareas
    private inner class SubtaskAdapter(
        private val items: MutableList<String>,
        private val onRemoveClick: (position: Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

        // ViewHolder que contiene las vistas de 'item_subtask.xml'
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

            // Listener para el botón de eliminar
            holder.removeButton.setOnClickListener {
                // Obtenemos la posición MÁS RECIENTE
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onRemoveClick(currentPosition)
                }
            }

            // Listener para el icono de arrastrar
            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(holder)
                }
                false // Devolvemos false para no consumir el evento
            }
        }

        override fun getItemCount() = items.size
    }

    // 12. NUEVO: Callback para el ItemTouchHelper (Drag and Drop)
    private inner class SubtaskTouchHelperCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Direcciones de drag
        0 // No usamos swipe (dirección 0)
    ) {
        // Deshabilitamos el "long press" para arrastrar, ya que usamos un icono (handle)
        override fun isLongPressDragEnabled() = false

        // No queremos swipe para eliminar (ya tenemos un botón)
        override fun isItemViewSwipeEnabled() = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            // Mover el item en la lista de datos
            Collections.swap(subtaskList, fromPosition, toPosition)

            // Notificar al adaptador del movimiento
            subtaskAdapter.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // No se usa, ya que la dirección de swipe es 0
        }
    }

}
