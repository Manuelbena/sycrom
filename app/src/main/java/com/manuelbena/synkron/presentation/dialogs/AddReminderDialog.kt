package com.manuelbena.synkron.presentation.dialogs


import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.util.formatTime

import java.util.Calendar

// Usamos BottomSheetDialogFragment para mejor UX en móviles,
// o puedes mantener DialogFragment si prefieres que flote en el centro.
class AddReminderDialog(
    private val onReminderAdded: (ReminderItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddCustomReminderBinding? = null
    private val binding get() = _binding!!

    // Hora seleccionada (por defecto la actual)
    private var selectedHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCustomReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Asegurar que el diálogo se expanda completamente si es BottomSheet
        val sheet = dialog as? BottomSheetDialog
        sheet?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupUI() {
        // Formatear hora inicial
        updateTimeDisplay()

        // Seleccionar Notificación por defecto
        binding.chipNotification.isChecked = true
    }

    private fun setupListeners() {
        // Listener para el selector de hora
        binding.cardTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnSave.setOnClickListener {
            val message = binding.etMessage.text.toString()
            val offsetText = binding.etOffset.text.toString()
            val offsetMinutes = offsetText.toIntOrNull() ?: 0

            // 1. Mapeamos el Chip al nuevo Enum 'ReminderMethod'
            val method = when (binding.chipGroupType.checkedChipId) {
                R.id.chipAlarm -> ReminderMethod.ALARM
                R.id.chipWhatsapp -> ReminderMethod.WHATSAPP
                else -> ReminderMethod.NOTIFICATION
            }

            // 2. Formateamos la hora para el campo 'displayTime'
            // (Asumo que tienes una función de extensión o utilidad para esto, si no, usa String.format)
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)

            // 3. Creación del objeto CORRECTO coincidiendo con tu nuevo data class
            val reminder = ReminderItem(
                // id = ... (No hace falta pasarlo, se genera solo con UUID en el modelo)
                hour = selectedHour,
                minute = selectedMinute,
                method = method,               // Campo corregido (antes era 'type')
                offsetMinutes = offsetMinutes, // Campo nuevo añadido
                displayTime = timeString,      // Campo necesario para la UI
                message = message.ifEmpty { "Recordatorio" }
            )

            onReminderAdded(reminder)
            dismiss()
        }
    }

    private fun showTimePicker() {
        // 1. Configurar el Picker estilo Material Design
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Selecciona la hora")
            .setPositiveButtonText("Aceptar") // Texto explícito
            .setNegativeButtonText("Cancelar")
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK) // Reloj visual (o KEYBOARD para teclado)
            .build()

        // 2. Escuchar el botón "Aceptar"
        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeDisplay()
        }

        // 3. Mostrarlo usando el ChildFragmentManager para que viva dentro del diálogo
        picker.show(childFragmentManager, "time_picker_tag")
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = formatTime(selectedHour, selectedMinute)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}