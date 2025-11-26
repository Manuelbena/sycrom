package com.manuelbena.synkron.presentation.activitys

import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.findNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseActivity
import com.manuelbena.synkron.databinding.ActivityContainerBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.ADD_MONEY
import com.manuelbena.synkron.presentation.util.ADD_TASK
// Asegúrate de importar tu constante o defínela aquí si prefieres
// import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContainerActivity : BaseActivity<ActivityContainerBinding>(){

    companion object {
        const val TASK_TO_EDIT_KEY = "task_to_edit_key" // Defínela aquí o en Constants
    }

    override fun inflateView(inflater: LayoutInflater) = ActivityContainerBinding.inflate(inflater)

    override fun setUI() {
        val taskToEdit = intent.getParcelableExtra<TaskDomain>(TASK_TO_EDIT_KEY)

        if (taskToEdit != null) {
            // FLUJO EDICIÓN: Si hay tarea, abrimos el fragmento de "Nueva Tarea" pero con datos
            setEditTaskFragment(taskToEdit)
        } else {
            // FLUJO CREACIÓN
            intent.getStringExtra(ADD_TASK)?.let {
                setAddTaskFragment() // Corregí el typo 'Fragmetn'
            } ?: run {
                intent.getStringExtra(ADD_MONEY)?.let {
                    setAddMoneyFragment()
                }
            }
        }
    }

    private fun setEditTaskFragment(task: TaskDomain) {
        val navController = binding.fragmentContainerView.findNavController()

        // Preparamos los argumentos para el fragmento de destino
        val bundle = Bundle().apply {
            putParcelable(TASK_TO_EDIT_KEY, task)
        }

        // Iniciamos el grafo pasándole el bundle.
        // Nota: Asegúrate que tu TaskFragment sepa recibir argumentos o lo manejemos en el onViewCreated
        navController.setGraph(R.navigation.new_task, bundle)
    }

    private fun setAddTaskFragment(){
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_task)
    }

    private fun setAddMoneyFragment(){
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_money)
    }
}