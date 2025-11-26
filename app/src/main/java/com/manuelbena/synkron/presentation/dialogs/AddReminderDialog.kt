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
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class AddReminderDialog(
    private val taskStartTime: Calendar, // 1. Recibimos la hora de inicio de la tarea
    private val onReminderAdded: (ReminderItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddCustomReminderBinding? = null
    private val binding get() = _binding!!

    // Iniciamos el reloj con la hora actual o la hora de la tarea (opcional)
    private var selectedHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute = Calendar.getInstance().get(Calendar.MINUTE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        val sheet = dialog as? BottomSheetDialog
        sheet?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupUI() {
        updateTimeDisplay()
        binding.chipNotification.isChecked = true
        // Si habías borrado el etOffset del XML, borra cualquier referencia aquí si la tenías
    }

    private fun setupListeners() {
        binding.cardTime.setOnClickListener { showTimePicker() }

        binding.btnSave.setOnClickListener {
            val message = binding.etMessage.text.toString()

            // 2. CÁLCULO MATEMÁTICO: Hora Tarea - Hora Recordatorio

            // Creamos un calendario para la hora del aviso (usando el mismo día de la tarea)
            val reminderTime = (taskStartTime.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, selectedHour)
                set(Calendar.MINUTE, selectedMinute)
                set(Calendar.SECOND, 0)
            }

            // Calculamos la diferencia en milisegundos
            val diffInMillis = taskStartTime.timeInMillis - reminderTime.timeInMillis

            // Convertimos a minutos
            val minutesOffset = TimeUnit.MILLISECONDS.toMinutes(diffInMillis).toInt()

            // 3. Validación: El aviso no puede ser posterior a la tarea
            if (minutesOffset < 0) {
                Toast.makeText(requireContext(), "El aviso debe ser antes de la hora de la tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val method = when (binding.chipGroupType.checkedChipId) {
                R.id.chipAlarm -> ReminderMethod.ALARM
                R.id.chipWhatsapp -> ReminderMethod.WHATSAPP
                else -> ReminderMethod.NOTIFICATION
            }

            val timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)

            val reminder = ReminderItem(
                id = UUID.randomUUID().toString(),
                hour = selectedHour,
                minute = selectedMinute,
                method = method,
                offsetMinutes = minutesOffset, // ¡Aquí va el cálculo automático!
                displayTime = timeString,
                message = message.ifEmpty { "Recordatorio" }
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

        picker.show(childFragmentManager, "time_picker_tag")
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}