package com.manuelbena.synkron.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddReminderDialog(
    private val startTime: Calendar,
    private val endTime: Calendar,
    private val onReminderAdded: (ReminderItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogAddCustomReminderBinding? = null
    private val binding get() = _binding!!

    private var selectedTime = (startTime.clone() as Calendar).apply {
        add(Calendar.MINUTE, -10)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddCustomReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateTimeDisplay()

        binding.cardTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnSave.setOnClickListener {
            val method = when (binding.chipGroupType.checkedChipId) {
                R.id.chipAlarm -> ReminderMethod.ALARM
                R.id.chipWhatsapp -> ReminderMethod.WHATSAPP
                else -> ReminderMethod.NOTIFICATION
            }

            val offsetMinutes = ((startTime.timeInMillis - selectedTime.timeInMillis) / (1000 * 60)).toInt()

            val reminder = ReminderItem(
                id = UUID.randomUUID().toString(),
                displayTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time),
                message = binding.etMessage.text.toString().ifBlank { "Recordatorio" },
                method = method,
                offsetMinutes = offsetMinutes,
                hour = selectedTime.get(Calendar.HOUR_OF_DAY),
                minute = selectedTime.get(Calendar.MINUTE)
            )
            onReminderAdded(reminder)
            dismiss()
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedTime.get(Calendar.HOUR_OF_DAY))
            .setMinute(selectedTime.get(Calendar.MINUTE))
            .setTitleText("Seleccionar hora")
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedTime.set(Calendar.HOUR_OF_DAY, picker.hour)
            selectedTime.set(Calendar.MINUTE, picker.minute)
            updateTimeDisplay()
        }

        picker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun updateTimeDisplay() {
        binding.tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
