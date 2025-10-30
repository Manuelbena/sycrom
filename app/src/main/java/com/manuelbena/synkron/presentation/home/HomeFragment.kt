package com.manuelbena.synkron.presentation.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by viewModels()
    // Inicializar TaskAdapter aquí
    private val taskAdapter = TaskAdapter { task ->
        // Acción al hacer clic: Mostrar BottomSheet
        TaskDetailBottomSheet.newInstance(task).show(
            parentFragmentManager,
            TaskDetailBottomSheet.TAG
        )
    }
    private lateinit var weekCalendarManager: WeekCalendarManager



    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        // No llamamos a viewModel.getTaskToday() aquí, el ViewModel carga los datos en su init
        setupRecyclerView()
        setupWeekCalendar()
        // updateFinanceData(1500f, 500f) // Considera mover esto a observe si los datos son dinámicos
    }

    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                }

                is HomeEvent.NavigateToTaskDetail -> {
                    // Lógica de navegación si es necesaria
                }

                is HomeEvent.ListTasksToday -> {
                    // El Flow emitió una nueva lista
                    binding.recyclerViewTasks.visibility = if (event.list.isEmpty()) View.INVISIBLE else View.VISIBLE
                    // Usar submitList para que ListAdapter calcule diferencias y anime
                    taskAdapter.submitList(event.list)
                }
            }
        }
    }

    override fun setListener() {
        binding.apply {
            buttonAddTask.setOnClickListener {
                val intent = Intent(requireContext(), ContainerActivity::class.java)
                intent.putExtra(ADD_TASK, "true")
                startActivity(intent)
            }
            // Listener para el botón de sugerencia si es necesario
            buttonAddTaskSuggestion.setOnClickListener {
                // Lógica para añadir la tarea sugerida
            }
        }
    }


    private fun setupRecyclerView() {
        // El adaptador ya se inicializó como propiedad de la clase

        val snapHelper = PagerSnapHelper()

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter // Asignar el adaptador inicializado
            applyCarouselPadding()
            // Quitar el addOnScrollListener si ya estás usando ListAdapter,
            // a menos que CarouselScrollListener haga algo más que animaciones básicas.
            // Si solo es para escala/alpha/blur, mantenlo.
            // Si era para lógica de carga/paginación, ya no es necesario con Flow.
            addOnScrollListener(CarouselScrollListener())
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupWeekCalendar() {
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate ->
            viewModel.onDateSelected(selectedDate) // Notificar al ViewModel
        }
        weekCalendarManager.setupCalendar()
    }

    // Eliminado updateFinanceData si no se usa dinámicamente o se mueve a observe

    /**
     * Calcula y aplica el padding horizontal necesario para que el primer y último ítem
     * del RecyclerView puedan centrarse en la pantalla.
     */
    private fun RecyclerView.applyCarouselPadding() {
        // Asegúrate de que el ancho del item (300dp) sea correcto
        val itemWidthDp = 250 // Ancho definido en item_task_today.xml
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt().coerceAtLeast(0) // Asegurar que no sea negativo

        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }
}
