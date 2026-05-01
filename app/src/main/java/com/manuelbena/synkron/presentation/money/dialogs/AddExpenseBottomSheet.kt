package com.manuelbena.synkron.presentation.money.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetAddExpenseBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSelectorAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseBottomSheet(
    private val budgets: List<BudgetPresentationModel>,
    // 1. AÑADIMOS dateMillis A LA FUNCIÓN DE GUARDADO
    private val onSaveExpense: (budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddExpenseBinding? = null
    private val binding get() = _binding!!

    private var selectedBudget: BudgetPresentationModel? = null

    // 2. VARIABLE PARA GUARDAR LA FECHA (Por defecto guarda el momento actual)
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // FUNDAMENTAL: Decirle a la ventana que se redimensione cuando se abra el teclado
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            if (bottomSheet != null) {
                // FORZAR PANTALLA COMPLETA
                val layoutParams = bottomSheet.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                bottomSheet.layoutParams = layoutParams

                // EXPANDIR
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { dismiss() }

        // Configurar la lista horizontal de presupuestos
        binding.rvBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = BudgetSelectorAdapter(budgets) { budget ->
                selectedBudget = budget
            }
        }

        // Auto-abrir teclado numérico
        binding.etAmount.requestFocus()

        // --- 3. LÓGICA DEL CALENDARIO ---
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Poner el texto inicial en el botón (Ej: "Hoy (07 abr 2026)")
        binding.btnDateSelector.text = "Hoy (${dateFormat.format(calendar.time)})"

        binding.btnDateSelector.setOnClickListener {
            // Abrir el selector de fecha nativo de Android
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Cuando el usuario elige una fecha, la guardamos
                    calendar.set(year, month, dayOfMonth)
                    selectedDateMillis = calendar.timeInMillis
                    // Actualizamos el texto del botón con la nueva fecha
                    binding.btnDateSelector.text = dateFormat.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        // --------------------------------

        binding.btnSaveExpense.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val note = binding.etNote.text.toString().trim()

            val amount = amountStr.toDoubleOrNull()

            // Validaciones
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Introduce un importe válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedBudget == null) {
                Toast.makeText(requireContext(), "Selecciona un presupuesto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. ENVIAMOS LOS DATOS AL VIEWMODEL (Incluyendo la fecha)
            onSaveExpense(selectedBudget!!, amount, note, selectedDateMillis)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}