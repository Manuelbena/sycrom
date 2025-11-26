package com.manuelbena.synkron.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class AddReminderDialog(
    private val taskStartTime: Calendar, // Inicio de la tarea
    private val taskEndTime: Calendar,   // Fin de la tarea (NUEVO)
    private val onReminderAdded: (ReminderItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddCustomReminderBinding? = null
    private val binding get() = _binding!!

    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddCustomReminderBinding.inflate(inflater, container, false)

        // Por defecto, sugerimos la hora de inicio
        selectedHour = taskStartTime.get(Calendar.HOUR_OF_DAY)
        selectedMinute = taskStartTime.get(Calendar.MINUTE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupUI() {
        updateTimeDisplay()
        binding.chipNotification.isChecked = true
    }

    private fun setupListeners() {
        binding.cardTime.setOnClickListener { showTimePicker() }

        binding.btnSave.setOnClickListener {
            val messageInput = binding.etMessage.text.toString()
            val finalMessage = messageInput.ifBlank { "Recordatorio" }

            // 1. Construimos la fecha exacta del recordatorio
            val reminderTime = (taskStartTime.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }

            val now = Calendar.getInstance()

            // VALIDACIÓN 1: El recordatorio NO puede ser en el pasado
            if (reminderTime.before(now)) {
                Toast.makeText(requireContext(), "La hora del aviso ya ha pasado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // VALIDACIÓN 2 (AJUSTADA): El recordatorio NO puede ser después de que acabe la tarea
            // (Permitimos avisos DURANTE la tarea, ej. 14:00 en una tarea de 8 a 17)
            if (reminderTime.after(taskEndTime)) {
                Toast.makeText(requireContext(), "El aviso debe ser antes de que acabe la tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cálculo de antelación (Minutos) respecto al INICIO
            // NOTA: Si es durante la tarea, el resultado será negativo (ej. -120 min).
            // Google Calendar ACEPTA negativos (significa "después del inicio").
            val diffInMillis = taskStartTime.timeInMillis - reminderTime.timeInMillis
            val minutesOffset = TimeUnit.MILLISECONDS.toMinutes(diffInMillis).toInt()

            val method = when (binding.chipGroupType.checkedChipId) {
                com.manuelbena.synkron.R.id.chipAlarm -> ReminderMethod.ALARM
                com.manuelbena.synkron.R.id.chipWhatsapp -> ReminderMethod.WHATSAPP
                else -> ReminderMethod.NOTIFICATION
            }

            val timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)

            val reminder = ReminderItem(
                id = UUID.randomUUID().toString(),
                hour = selectedHour,
                minute = selectedMinute,
                method = method,
                offsetMinutes = minutesOffset, // Puede ser negativo si es "durante"
                displayTime = timeString,
                message = finalMessage
            )

            onReminderAdded(reminder)
            dismiss()
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Hora del aviso")
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeDisplay()
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}