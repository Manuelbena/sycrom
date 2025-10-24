package com.manuelbena.synkron.presentation.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.models.SubTaskPresentation
import com.manuelbena.synkron.presentation.models.TaskPresentation
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var weekCalendarManager: WeekCalendarManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getTaskToday()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupRecyclerView()
        setupWeekCalendar()
    }

    override fun observe() {

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                }

                is HomeEvent.NavigateToTaskDetail -> {

                }

                is HomeEvent.ListTasksToday -> {
                    taskAdapter.submitList(event.list)
                }
            }
        }
    }

    override fun setListener() {
        binding.apply {
            recyclerViewTasks.setOnClickListener {
                val miTarea = TaskPresentation(
                    hour = "10:00 AM",
                    date = 20251008,
                    title = "Reunión de Sincronización",
                    description = "Discutir los avances del sprint.",
                    typeTask = "Trabajo",
                    place = "Oficina",
                    subTasks = listOf(SubTaskPresentation("Preparar slides", true)),
                    isActive = true,
                    isDone = false
                )

                // 2. Muestra el Bottom Sheet
                TaskDetailBottomSheet.newInstance(miTarea).show(
                    parentFragmentManager,
                    TaskDetailBottomSheet.TAG
                )

            }
            buttonAddTask.setOnClickListener {
                val intent = Intent(requireContext(), ContainerActivity::class.java)
                intent.putExtra(ADD_TASK, "true")
                startActivity(intent)
            }
        }



    }


    private fun setupRecyclerView() {
        // 1. Al crear el adaptador, le decimos QUÉ HACER cuando se pulse un ítem.
        taskAdapter = TaskAdapter { task ->
            // 'task' es el objeto TaskPresentation del ítem que se ha pulsado.

            // 2. Muestra el Bottom Sheet con los datos de la tarea pulsada.
            TaskDetailBottomSheet.newInstance(task).show(
                parentFragmentManager,
                TaskDetailBottomSheet.TAG
            )
        }


        val snapHelper = PagerSnapHelper()

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupWeekCalendar() {
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate ->
            viewModel.onDateSelected(selectedDate)
        }
        weekCalendarManager.setupCalendar()
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateFinanceData(income: Float, expenses: Float) {
        binding.financePie.setData(ingresos = income, gastos = expenses)
        binding.textIngresos.text = context?.getString(R.string.currency_format, income)
        binding.textGastos.text = context?.getString(R.string.currency_format_negative, expenses)
    }

    /**
     * Calcula y aplica el padding horizontal necesario para que el primer y último ítem
     * del RecyclerView puedan centrarse en la pantalla.
     */
    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthPx = resources.displayMetrics.density * 300 // Ancho del ítem en dp.
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt()

        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }
}