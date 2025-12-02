package com.manuelbena.synkron.presentation.activitys

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.findNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseActivity
import com.manuelbena.synkron.databinding.ActivityContainerBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.ADD_MONEY
import com.manuelbena.synkron.presentation.util.ADD_TASK
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContainerActivity : BaseActivity<ActivityContainerBinding>() {

    companion object {
        // Clave utilizada para pasar la tarea a editar entre Activities
        const val TASK_TO_EDIT_KEY = "task_to_edit_key"
    }

    override fun inflateView(inflater: LayoutInflater) = ActivityContainerBinding.inflate(inflater)

    override fun setUI() {
        val taskToEdit = intent.getParcelableExtra<TaskDomain>(TASK_TO_EDIT_KEY)

        if (taskToEdit != null) {
            // FLUJO EDICIÓN: Abrimos el fragmento con los datos de la tarea
            setEditTaskFragment(taskToEdit)
        } else {
            // FLUJO CREACIÓN: Verificamos qué tipo de creación se solicita
            intent.getStringExtra(ADD_TASK)?.let {
                setAddTaskFragment()
            } ?: run {
                intent.getStringExtra(ADD_MONEY)?.let {
                    setAddMoneyFragment()
                }
            }
        }
    }

    /**
     * 🔥 NUEVO: Método helper para cerrar la actividad indicando ÉXITO.
     * Los Fragments (TaskFragment) deben llamar a esto cuando el ViewModel confirme el guardado.
     */
    fun closeWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun setEditTaskFragment(task: TaskDomain) {
        val navController = binding.fragmentContainerView.findNavController()

        // Pasamos la tarea como argumento al grafo de navegación
        val bundle = Bundle().apply {
            putParcelable(TASK_TO_EDIT_KEY, task)
        }

        // Navegamos al grafo de "nueva tarea" (que se reutiliza para edición)
        navController.setGraph(R.navigation.new_task, bundle)
    }

    private fun setAddTaskFragment() {
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_task)
    }

    private fun setAddMoneyFragment() {
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_money)
    }
}