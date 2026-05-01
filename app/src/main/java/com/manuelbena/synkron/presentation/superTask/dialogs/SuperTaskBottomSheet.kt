package com.manuelbena.synkron.presentation.superTask

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.manuelbena.synkron.databinding.BottomSheetSuperTaskDetailBinding
import com.manuelbena.synkron.domain.models.SubTaskItem
import com.manuelbena.synkron.domain.models.SuperTaskModel

class SuperTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSuperTaskDetailBinding? = null
    private val binding get() = _binding!!

    private var superTask: SuperTaskModel? = null

    // Callback para guardar
    var onSaveClickListener: ((SuperTaskModel) -> Unit)? = null

    companion object {
        private const val ARG_TASK = "arg_super_task"

        fun newInstance(task: SuperTaskModel): SuperTaskBottomSheet {
            val fragment = SuperTaskBottomSheet()
            val args = Bundle()
            args.putParcelable(ARG_TASK, task) // Esto funcionará al ser Parcelable
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Recuperamos el Parcelable
            superTask = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_TASK, SuperTaskModel::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_TASK)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSuperTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        superTask?.let { task -> setupUI(task) }
    }

    private fun setupUI(task: SuperTaskModel) {
        binding.apply {
            tvTitle.text = task.title
            ivMainIcon.setImageResource(task.getIconRes())

            // Inicializar progreso
            updateHeaderProgressInternal(task.subTasks, task.totalCount)

            btnClose.setOnClickListener { dismiss() }

            // Convertimos a MutableList para el adaptador de edición
            val mutableSubTasks = task.subTasks.toMutableList()

            val adapter = SuperTaskDetailAdapter(mutableSubTasks) { position ->
                updateHeaderProgressInternal(mutableSubTasks, task.totalCount)
            }
            android.util.Log.d("SuperTask", "Recibidas ${task.subTasks.size} subtareas")

            rvDetails.layoutManager = LinearLayoutManager(context)
            rvDetails.adapter = adapter

            btnSave.setOnClickListener {
                val updatedSubTasks = adapter.getUpdatedItems()
                val newCompletedCount = updatedSubTasks.count { it.isCompleted }

                val updatedTask = task.copy(
                    subTasks = updatedSubTasks,
                    completedCount = newCompletedCount
                )

                onSaveClickListener?.invoke(updatedTask)
                dismiss()
            }
        }
    }

    private fun updateHeaderProgressInternal(items: List<SubTaskItem>, total: Int) {
        val completed = items.count { it.isCompleted }
        val percentage = if(total > 0) (completed * 100) / total else 0
        binding.progressBar.setProgressCompat(percentage, true)
        binding.tvProgressPercent.text = "$percentage%"
        binding.tvCompletedCount.text = "$completed de $total completados"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}