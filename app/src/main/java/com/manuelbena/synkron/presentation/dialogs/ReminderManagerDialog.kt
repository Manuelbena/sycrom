package com.manuelbena.synkron.presentation.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.DialogAddCustomReminderBinding
import com.manuelbena.synkron.databinding.DialogReminderManagerBinding
import com.manuelbena.synkron.presentation.dialogs.adapter.ReminderManagerAdapter
import com.manuelbena.synkron.presentation.models.ReminderItem
import com.manuelbena.synkron.presentation.models.ReminderMethod

class ReminderManagerDialog(
    private val initialReminders: List<ReminderItem>,
    private val onRemindersConfirmed: (List<ReminderItem>) -> Unit
) : DialogFragment() {

    private var _binding: DialogReminderManagerBinding? = null
    private val binding get() = _binding!!

    private val currentList = initialReminders.toMutableList()
    private lateinit var adapter: ReminderManagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogReminderManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.btnAddAction.setOnClickListener {
            showAddCustomReminderDialog()
        }

        binding.btnDone.setOnClickListener {
            onRemindersConfirmed(currentList)
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = ReminderManagerAdapter { itemToDelete ->
            currentList.remove(itemToDelete)
            updateListUI()
        }
        binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReminders.adapter = adapter
        updateListUI()
    }

    private fun updateListUI() {
        adapter.submitList(currentList.toList())
        // Opcional: Mostrar un estado vacío si no hay items
    }

    // --- LÓGICA DEL DIÁLOGO PERSONALIZADO ---
    private fun showAddCustomReminderDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAddCustomReminderBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Configurar dropdown de unidades
        val units = resources.getStringArray(R.array.reminder_units)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        dialogBinding.actvUnit.setAdapter(adapter)
        dialogBinding.actvUnit.setText(units[0], false) // Default: minutos

        dialogBinding.btnCancelAdd.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnConfirmAdd.setOnClickListener {
            val amountStr = dialogBinding.etAmount.text.toString()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toInt()
                val unitSelection = dialogBinding.actvUnit.text.toString()

                // Convertir todo a MINUTOS
                val minutes = when {
                    unitSelection.contains("minutos") -> amount
                    unitSelection.contains("horas") -> amount * 60
                    unitSelection.contains("días") -> amount * 1440
                    unitSelection.contains("semanas") -> amount * 10080
                    else -> amount
                }

                val method = if (dialogBinding.chipEmail.isChecked) ReminderMethod.EMAIL else ReminderMethod.POPUP

                // Añadir a la lista
                currentList.add(ReminderItem(minutes = minutes, method = method))
                updateListUI()
                dialog.dismiss()
            } else {
                dialogBinding.etAmount.error = "Introduce un valor"
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object { const val TAG = "ReminderMgrDialog" }
}