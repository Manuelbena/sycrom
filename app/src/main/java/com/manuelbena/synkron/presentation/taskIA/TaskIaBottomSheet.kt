package com.manuelbena.synkron.presentation.taskIA

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetTaskIaBinding
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
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
        setObservers()
        setupListeners()
    }

    private fun setObservers() {
        // Asegúrate de importar tu sealed class IaUiState correctamente si está anidada o en otro archivo
        viewModel.iaResponseState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TaskIaViewModel.IaUiState.Loading -> {
                    // Muestra feedback de que Synkrom está pensando 🤖
                    binding.progressBar.isVisible = true // Asumiendo que tienes un progressBar en el XML
                    binding.btnSend.isEnabled = false
                }

                is TaskIaViewModel.IaUiState.Success -> {
                    binding.progressBar.isVisible = false
                    binding.btnSend.isEnabled = true

                    // ✅ CORRECCIÓN CRÍTICA AQUÍ:
                    // Pasamos 'state.task' (el objeto TaskDomain), NO 'state' (el wrapper).
                    val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
                        putExtra(ContainerActivity.TASK_TO_EDIT_KEY, state.task)
                    }

                    startActivity(intent)
                    dismiss()

                    // Opcional: Limpiar estado en ViewModel si fuera necesario
                }

                is TaskIaViewModel.IaUiState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnSend.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    // Idle state
                    binding.progressBar.isVisible = false
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessageToIa(text)
            } else {
                binding.etInput.error = "Escribe algo para Synkrom..."
            }
        }

        binding.btnMic.setOnClickListener {
            Toast.makeText(requireContext(), "Próximamente... 🎙️", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}