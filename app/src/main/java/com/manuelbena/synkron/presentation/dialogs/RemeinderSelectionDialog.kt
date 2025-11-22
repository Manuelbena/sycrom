package com.manuelbena.synkron.presentation.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.manuelbena.synkron.databinding.DialogReminderSelectorBinding
import com.manuelbena.synkron.presentation.models.ReminderSelection
import com.manuelbena.synkron.presentation.models.ReminderType

class ReminderSelectionDialog(
    private val currentSelection: ReminderSelection?,
    private val onReminderSelected: (ReminderSelection) -> Unit
) : DialogFragment() {

    private var _binding: DialogReminderSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReminderSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Configuración CRÍTICA para que el diálogo se vea flotante y con ancho correcto
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(), // 90% del ancho de pantalla
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Fondo transparente para esquinas redondeadas
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialState()
        setupListeners()
    }

    private fun setupInitialState() {
        currentSelection?.let { sel ->
            binding.switchEnableReminder.isChecked = sel.isEnabled
            binding.containerOptions.isVisible = sel.isEnabled

            if (sel.isEnabled) {
                when (sel.timeOffset) {
                    0L -> binding.chipOnTime.isChecked = true
                    600000L -> binding.chip10Min.isChecked = true
                    3600000L -> binding.chip1Hour.isChecked = true
                    86400000L -> binding.chip1Day.isChecked = true
                }
                if (sel.type == ReminderType.ALARM) binding.rbAlarm.isChecked = true
                else binding.rbNotification.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        binding.switchEnableReminder.setOnCheckedChangeListener { _, isChecked ->
            binding.containerOptions.isVisible = isChecked
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            val isEnabled = binding.switchEnableReminder.isChecked

            if (!isEnabled) {
                onReminderSelected(ReminderSelection(isEnabled = false, label = "Sin recordatorio"))
            } else {
                val offset = when (binding.chipGroupTiming.checkedChipId) {
                    binding.chip10Min.id -> 600000L
                    binding.chip1Hour.id -> 3600000L
                    binding.chip1Day.id -> 86400000L
                    else -> 0L
                }

                val type = if (binding.rbAlarm.isChecked) ReminderType.ALARM else ReminderType.NOTIFICATION

                // Generamos una etiqueta bonita
                val timeText = when(offset) {
                    0L -> "Al momento"
                    600000L -> "10 min antes"
                    3600000L -> "1 h antes"
                    86400000L -> "1 día antes"
                    else -> ""
                }
                val typeText = if(type == ReminderType.ALARM) "Alarma" else "Notif."

                onReminderSelected(
                    ReminderSelection(
                        isEnabled = true,
                        type = type,
                        timeOffset = offset,
                        label = "$timeText ($typeText)"
                    )
                )
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReminderDialog"
    }
}