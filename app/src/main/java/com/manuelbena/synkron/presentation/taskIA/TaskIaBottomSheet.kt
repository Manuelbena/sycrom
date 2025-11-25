package com.manuelbena.synkron.presentation.task_ia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetTaskIaBinding
import com.manuelbena.synkron.presentation.taskIA.TaskIaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskIaBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskIaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TaskIaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskIaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()

    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessageToIa(text)
            }
        }

        binding.btnMic.setOnClickListener {
            Toast.makeText(requireContext(), "Próximamente...", Toast.LENGTH_SHORT).show()
        }
    }


}