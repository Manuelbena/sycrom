package com.manuelbena.synkron.presentation.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.DialogReminderManagerBinding
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod
import java.util.UUID

class ReminderManagerDialog(
    private val initialReminders: List<ReminderItem>,
    private val onRemindersConfirmed: (List<ReminderItem>) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogReminderManagerBinding? = null
    private val binding get() = _binding!!

    // Lista mutable local para gestionar los cambios
    private val currentReminders = initialReminders.toMutableList()

    private lateinit var adapter: ReminderManagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogReminderManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        // 1. CORRECCIÓN: Inicializamos pasando SOLO el lambda, sin la lista.
        adapter = ReminderManagerAdapter { reminderToRemove ->
            // Callback de eliminar
            currentReminders.remove(reminderToRemove)
            updateAdapterList() // Actualizamos el adaptador
        }

        binding.rvReminders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ReminderManagerDialog.adapter
        }

        // 2. Cargar datos iniciales
        updateAdapterList()
    }

    private fun setupListeners() {
        with(binding) {
            btnAddAction.setOnClickListener {
                val isVisible = layoutAddSection.isVisible
                layoutAddSection.isVisible = !isVisible
            }

            // Chips rápidos
            chip10Min.setOnClickListener { addQuickReminder(10, "10 min antes") }
            chip1Hour.setOnClickListener { addQuickReminder(60, "1 hora antes") }
            chip1Day.setOnClickListener { addQuickReminder(1440, "1 día antes") }

            btnDone.setOnClickListener {
                onRemindersConfirmed(currentReminders)
                dismiss()
            }
        }
    }

    private fun addQuickReminder(minutes: Int, label: String) {
        val newItem = ReminderItem(
            id = UUID.randomUUID().toString(),
            hour = 0,
            minute = 0,
            method = ReminderMethod.NOTIFICATION,
            offsetMinutes = minutes,
            displayTime = label,
            message = "Recordatorio"
        )

        currentReminders.add(newItem)
        updateAdapterList() // 3. Actualizamos usando submitList

        binding.layoutAddSection.isVisible = false
    }

    // Helper para actualizar el ListAdapter correctamente
    private fun updateAdapterList() {
        // submitList necesita una NUEVA instancia de lista para detectar cambios,
        // por eso usamos .toList() para crear una copia inmutable.
        adapter.submitList(currentReminders.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}