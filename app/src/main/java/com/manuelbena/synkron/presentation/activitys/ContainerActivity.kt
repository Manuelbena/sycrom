package com.manuelbena.synkron.presentation.activitys

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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
        const val EXTRA_FRAGMENT_TYPE = "FRAGMENT_TYPE"
        const val FRAGMENT_TYPE_TASK = "TASK"
        const val TASK_TO_EDIT_KEY = "TASK_TO_EDIT"
        const val EXTRA_START_FRAGMENT = "EXTRA_START_FRAGMENT"

        fun start(context: Context, task: TaskDomain) {
            val intent = Intent(context, ContainerActivity::class.java).apply {
                putExtra(TASK_TO_EDIT_KEY, task)
                putExtra(EXTRA_FRAGMENT_TYPE, FRAGMENT_TYPE_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun inflateView(inflater: LayoutInflater) = ActivityContainerBinding.inflate(inflater)

    // Helper para obtener el NavController de forma segura
    private val navController: NavController
        get() {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragment_container_view) as NavHostFragment
            return navHostFragment.navController
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNavigationIntent()
    }

    override fun setUI() {
        // Nada aquí
    }

    private fun handleNavigationIntent() {
        val taskToEdit = intent.getParcelableExtra<TaskDomain>(TASK_TO_EDIT_KEY)
        val startFragment = intent.getStringExtra(EXTRA_START_FRAGMENT)

        when {
            taskToEdit != null -> setEditTaskFragment(taskToEdit)
            startFragment == "NEW_TASK" -> setAddTaskFragment()
            startFragment == "CALENDAR" -> setCalendarFragment()
            intent.hasExtra(ADD_TASK) -> setAddTaskFragment()
            intent.hasExtra(ADD_MONEY) -> setAddMoneyFragment()
        }
    }

    fun closeWithSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    // --- MÉTODOS DE NAVEGACIÓN ---

    private fun setEditTaskFragment(task: TaskDomain) {
        val bundle = Bundle().apply {
            putParcelable(TASK_TO_EDIT_KEY, task)
        }
        // Usamos la propiedad segura 'navController'
        navController.setGraph(R.navigation.new_task, bundle)
    }

    private fun setAddTaskFragment() {
        navController.setGraph(R.navigation.new_task)
    }

    private fun setAddMoneyFragment() {
        navController.setGraph(R.navigation.new_money)
    }

    private fun setCalendarFragment() {
        // Cargamos el grafo principal y forzamos inicio en Calendario
        val graph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        graph.setStartDestination(R.id.navigation_calendar) // Asegúrate que este ID es correcto en tu XML
        navController.graph = graph
    }
}