package com.manuelbena.synkron.presentation.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BotoomSheetTaskDetailBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.adapters.SubtaskAdapter

// ... otras importaciones

private const val ARG_TASK = "arg_task"

@AndroidEntryPoint
class TaskDetailBottomSheet (
    val task: TaskDomain,
) : BottomSheetDialogFragment() {

    private var _binding: BotoomSheetTaskDetailBinding? = null
    private val binding get() = _binding!!


    private val subtaskAdapter = SubtaskAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BotoomSheetTaskDetailBinding.inflate(inflater, container, false)
        bindTaskData(task)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.subtasksRecyclerView.adapter = subtaskAdapter
    }

    private fun bindTaskData(task: TaskDomain) {
        binding.titleTextView.text = task.title
        binding.typeChip.text = task.typeTask
        binding.descriptionTextView.text = task.description
        binding.placeTextView.text = task.place
        // Aquí podrías formatear la fecha y hora de forma más elegante
        binding.dateTimeTextView.text = "${task.date} - ${task.hour}"

        subtaskAdapter.submitList(task.subTasks)
    }

    override fun onStart() {
        super.onStart()

        // --- AQUÍ EMPIEZA LA MAGIA ---
        val dialog = dialog as? BottomSheetDialog
        val behavior = dialog?.behavior

        if (behavior != null) {
            // 1. Permitimos que se oculte al arrastrarlo hasta abajo.
            behavior.isHideable = true

            // 2. Hacemos que al arrastrar, pase de EXPANDED a HIDDEN directamente,
            // sin pasar por el estado intermedio COLLAPSED.
            behavior.skipCollapsed = true

            // 3. Forzamos a que siempre aparezca completamente expandido.
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskDetailBottomSheet"

        // Patrón de Fábrica: La forma correcta de crear fragmentos con argumentos
        fun newInstance(task: TaskDomain): TaskDetailBottomSheet {
            val args = Bundle().apply {
                putParcelable(ARG_TASK, task)
            }
            return TaskDetailBottomSheet(task = task).apply {
                arguments = args
            }
        }
    }
}