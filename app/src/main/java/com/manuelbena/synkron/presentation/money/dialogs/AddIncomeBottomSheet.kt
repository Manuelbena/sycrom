package com.manuelbena.synkron.presentation.money.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.BottomSheetAddIncomeBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSelectorAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeBottomSheet(
    private val budgets: List<BudgetPresentationModel>,
    private val onSaveIncome: (budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddIncomeBinding? = null
    private val binding get() = _binding!!

    private var selectedBudget: BudgetPresentationModel? = null
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            if (bottomSheet != null) {
                val layoutParams = bottomSheet.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                bottomSheet.layoutParams = layoutParams

                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { dismiss() }

        // Configurar la lista horizontal de presupuestos (categorías de ingresos)
        binding.rvBudgets.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
            adapter = BudgetSelectorAdapter(budgets) { budget ->
                selectedBudget = budget
            }
        }

        binding.etAmount.requestFocus()

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.btnDateSelector.text = "Hoy (${dateFormat.format(calendar.time)})"

        binding.btnDateSelector.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDateMillis)
                .setTitleText("Seleccionar fecha")
                .setTheme(R.style.ThemeOverlay_App_DatePicker)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDateMillis = selection
                val date = Calendar.getInstance().apply { timeInMillis = selection }
                binding.btnDateSelector.text = dateFormat.format(date.time)
            }
            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        binding.btnSaveIncome.setOnClickListener {
            val amountStr = binding.etAmount.text.toString().trim()
            val note = binding.etNote.text.toString().trim()
            val amount = amountStr.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "Introduce un importe válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedBudget == null) {
                Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSaveIncome(selectedBudget!!, amount, note, selectedDateMillis)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}