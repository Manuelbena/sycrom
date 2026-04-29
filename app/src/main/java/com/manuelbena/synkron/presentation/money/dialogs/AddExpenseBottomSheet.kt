package com.manuelbena.synkron.presentation.money.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetAddExpenseBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSelectorAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseBottomSheet(
    private val budgets: List<BudgetPresentationModel>,
    private val type: String, // "EXPENSE" o "INCOME"
    // 1. AÑADIMOS dateMillis A LA FUNCIÓN DE GUARDADO
    private val onSaveExpense: (budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long, type: String) -> Unit
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

        // Cambiar títulos según el tipo
        if (type == "INCOME") {
            binding.tvTitle.text = "Añadir Ingreso"
            binding.btnSaveExpense.text = "Guardar Ingreso"
            binding.etAmount.hint = "Importe del ingreso"
            binding.btnSaveExpense.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.emerald)
        } else {
            binding.tvTitle.text = "Añadir Gasto"
            binding.btnSaveExpense.text = "Guardar Gasto"
            binding.etAmount.hint = "Importe del gasto"
            binding.btnSaveExpense.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.red_500)
        }

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

            // 4. ENVIAMOS LOS DATOS AL VIEWMODEL (Incluyendo la fecha y el tipo)
            onSaveExpense(selectedBudget!!, amount, note, selectedDateMillis, type)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}