package com.manuelbena.synkron.presentation.money.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetAddMoneyGoalBinding
import com.manuelbena.synkron.presentation.models.GoalPresentationModel

class AddMoneyToGoalBottomSheet(
    private val goal: GoalPresentationModel,
    private val onAdd: (Double) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddMoneyGoalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddMoneyGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGoalName.text = "Añadir a: ${goal.title}"

        binding.chip10.setOnClickListener { onAdd(10.0); dismiss() }
        binding.chip50.setOnClickListener { onAdd(50.0); dismiss() }
        binding.chip100.setOnClickListener { onAdd(100.0); dismiss() }

        binding.btnAddCustom.setOnClickListener {
            val amount = binding.etCustomAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (amount > 0) {
                onAdd(amount)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
