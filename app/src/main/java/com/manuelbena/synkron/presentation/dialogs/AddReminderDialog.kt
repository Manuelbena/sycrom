package com.manuelbena.synkron.presentation.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class AddReminderDialog(
    private val taskDueDate: Calendar?,
    private val onReminderAdded: (ReminderItem) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddCustomReminderBinding? = null
    private val binding get() = _binding!!

    // Hora seleccionada por defecto
    private var selectedHour = 9
    private var selectedMinute = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCustomReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        updateTimeView()

        // --- LÓGICA POR DEFECTO ---
        // Marcamos "Siempre activo" al iniciar
        binding.cbAlwaysActive.isChecked = true
        binding.cbWeekDays.isChecked = false
        binding.cbDaysBefore.isChecked = false

        // Ocultamos las vistas dependientes
        binding.toggleWeekDays.isVisible = false
        binding.tilDaysBefore.isVisible = false
    }

    private fun setupListeners() {
        binding.apply {
            // 1. Selector de Hora
            tvTimeSelector.setOnClickListener { showTimePicker() }

            // 2. LÓGICA DE CHECKBOX EXCLUYENTES (Como RadioButtons)

            // A) Opción: Repetir días de la semana
            cbWeekDays.setOnClickListener {
                if (cbWeekDays.isChecked) {
                    // Si se activa, desactivamos los otros dos
                    cbDaysBefore.isChecked = false
                    cbAlwaysActive.isChecked = false

                    // Mostramos su contenido
                    toggleWeekDays.isVisible = true
                    tilDaysBefore.isVisible = false
                } else {
                    // Si el usuario lo desmarca manualmente y no hay otro activo, volvemos al default
                    if (!cbDaysBefore.isChecked && !cbAlwaysActive.isChecked) {
                        cbAlwaysActive.isChecked = true
                        toggleWeekDays.isVisible = false
                    }
                }
            }

            // B) Opción: Recordar días antes
            cbDaysBefore.setOnClickListener {
                if (cbDaysBefore.isChecked) {
                    // Desactivamos los otros
                    cbWeekDays.isChecked = false
                    cbAlwaysActive.isChecked = false

                    // UI visual
                    tilDaysBefore.isVisible = true
                    toggleWeekDays.isVisible = false
                    etDaysBefore.requestFocus()
                } else {
                    // Si intenta quedarse vacío, forzamos el default
                    if (!cbWeekDays.isChecked && !cbAlwaysActive.isChecked) {
                        cbAlwaysActive.isChecked = true
                        tilDaysBefore.isVisible = false
                    }
                }
            }

            // C) Opción: Siempre activo (Default)
            cbAlwaysActive.setOnClickListener {
                if (cbAlwaysActive.isChecked) {
                    // Desactivamos los otros
                    cbWeekDays.isChecked = false
                    cbDaysBefore.isChecked = false

                    // Ocultamos todo
                    toggleWeekDays.isVisible = false
                    tilDaysBefore.isVisible = false
                } else {
                    // No permitimos desmarcar este si es el único activo
                    // (Efecto RadioButton obligatorio)
                    if (!cbWeekDays.isChecked && !cbDaysBefore.isChecked) {
                        cbAlwaysActive.isChecked = true
                    }
                }
            }

            // 3. Guardar y Cancelar
            btnCancel.setOnClickListener { dismiss() }

            btnSave.setOnClickListener {
                if (validateInput()) {
                    val reminder = buildReminder()
                    onReminderAdded(reminder)
                    dismiss()
                }
            }
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Hora del aviso")
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeView()
        }
        picker.show(parentFragmentManager, "timePicker")
    }

    private fun updateTimeView() {
        binding.tvTimeSelector.text = String.format("%02d:%02d", selectedHour, selectedMinute)
    }

    private fun validateInput(): Boolean {
        // Solo validamos el input de días si ese checkbox está activo
        if (binding.cbDaysBefore.isChecked && binding.etDaysBefore.text.isNullOrEmpty()) {
            binding.tilDaysBefore.error = "Indica los días"
            return false
        }
        binding.tilDaysBefore.error = null
        return true
    }

    private fun buildReminder(): ReminderItem {

        val method = when (binding.toggleGroupType.checkedButtonId) {
            R.id.btnTypeEmail -> ReminderMethod.EMAIL
            R.id.btnTypeAlarm -> ReminderMethod.ALARM
            else -> ReminderMethod.POPUP
        }

        // 2. Minutos (Lógica matemática)
        val minutesOffset = calculateMinutesOffset()

        // 3. Textos para la UI (NUEVO)
        val timeText = String.format("%02d:%02d", selectedHour, selectedMinute)
        val messageText = binding.etCustomMessage.text.toString().ifEmpty { "Recordatorio" }

        return ReminderItem(
            minutes = minutesOffset,
            method = method,
            displayTime = timeText,  // Guardamos la hora
            message = messageText    // Guardamos el mensaje
        )
    }

    private fun calculateMinutesOffset(): Int {
        // Si es "Siempre activo" o "Días de semana" (que no tenemos lógica de repetición en ReminderItem aún),
        // calculamos el offset base o 0. Aquí dejo la lógica de "Días antes" que es la que afecta al tiempo fijo.

        val baseDate = taskDueDate ?: Calendar.getInstance()
        val reminderDate = baseDate.clone() as Calendar

        if (binding.cbDaysBefore.isChecked) {
            val daysBefore = binding.etDaysBefore.text.toString().toIntOrNull() ?: 0
            reminderDate.add(Calendar.DAY_OF_YEAR, -daysBefore)
        }
        // NOTA: Para "WeekDays" y "AlwaysActive", la lógica de fecha dependerá de cómo quieras guardarlo.
        // Por ahora, si está en "AlwaysActive", asumimos que es el mismo día de la tarea a la hora seleccionada.

        reminderDate.set(Calendar.HOUR_OF_DAY, selectedHour)
        reminderDate.set(Calendar.MINUTE, selectedMinute)
        reminderDate.set(Calendar.SECOND, 0)

        val diffMillis = baseDate.timeInMillis - reminderDate.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(diffMillis).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddReminderDialog"
    }
}