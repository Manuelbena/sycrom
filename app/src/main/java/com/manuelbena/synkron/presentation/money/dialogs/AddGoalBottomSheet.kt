package com.manuelbena.synkron.presentation.money.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetAddGoalBinding

class AddGoalBottomSheet(
    private val onSave: (String, Double, String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddGoalBinding? = null
    private val binding get() = _binding!!
    private var selectedColor = "#8B5CF6" // Default Purple

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveGoal.setOnClickListener {
            val title = binding.etGoalTitle.text.toString()
            val amount = binding.etGoalAmount.text.toString().toDoubleOrNull() ?: 0.0

            if (title.isNotEmpty() && amount > 0) {
                onSave(title, amount, selectedColor)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup color selection logic here if needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
