package com.manuelbena.synkron.presentation.taskIA

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
                    showLoadingAnimation(true)
                }

                is TaskIaViewModel.IaUiState.Success -> {

                    showLoadingAnimation(false)
                    val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
                        putExtra(ContainerActivity.TASK_TO_EDIT_KEY, state.task)
                    }

                    startActivity(intent)
                    dismiss()

                    // Opcional: Limpiar estado en ViewModel si fuera necesario
                }

                is TaskIaViewModel.IaUiState.Error -> {
                    showLoadingAnimation(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    // Idle state
                    showLoadingAnimation(false)
                }
            }
        }
    }
    private fun showLoadingAnimation(isLoading: Boolean) {
        if (_binding == null) return

        if (isLoading) {
            // PASO CRÍTICO: Ocultar teclado y quitar foco antes de animar
            hideKeyboard()

            // 1. Ocultar Inputs
            binding.groupInput.visibility = View.INVISIBLE

            // 2. Mostrar Loading con animación
            binding.layoutLoadingIa.apply {
                alpha = 0f
                scaleX = 0.8f
                scaleY = 0.8f
                isVisible = true
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setListener(null)
                    .start()
            }
            binding.lottieThinking.playAnimation()
        } else {
            // Restaurar vista (código corregido del NPE anterior)
            binding.layoutLoadingIa.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        _binding?.let { safeBinding ->
                            safeBinding.layoutLoadingIa.isVisible = false
                            safeBinding.groupInput.visibility = View.VISIBLE
                            // Opcional: Si quieres que el teclado vuelva a salir al fallar, puedes solicitar foco aquí
                        }
                    }
                })
                .start()
        }
    }

    // Función auxiliar para ocultar el teclado de forma segura
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        // Buscamos la vista que tiene el foco actualmente, o usamos el etInput por defecto
        val currentFocusView = dialog?.currentFocus ?: binding.etInput

        // Ocultamos el teclado
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)

        // Quitamos el foco para que no se quede el cursor parpadeando o la línea del EditText resaltada
        currentFocusView.clearFocus()
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