package com.manuelbena.synkron.presentation.taskIA

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetTaskIaBinding
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

        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotEmpty()) {
                viewModel.sendInput(text)
            }
        }

        binding.btnMic.setOnClickListener {
            // TODO: Implementar SpeechRecognizer aquí
            Toast.makeText(context, "Escuchando... (Próximamente)", Toast.LENGTH_SHORT).show()
        }

        viewModel.onSuccess.observe(viewLifecycleOwner) {
            Toast.makeText(context, "¡Recibido! Synkrón está procesando...", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}