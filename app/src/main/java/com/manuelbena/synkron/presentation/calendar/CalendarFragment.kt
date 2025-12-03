package com.manuelbena.synkron.presentation.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekDayBinder
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment : BaseFragment<FragmentCalendarBinding, CalendarViewModel>() {

    override val viewModel: CalendarViewModel by viewModels()

    // CORRECCIÓN: Inicializamos el adaptador con los 3 parámetros requeridos
    private val tasksAdapter by lazy {
        TaskAdapter(
            // 1. Click en la tarea (Editar)
            onItemClick = { task ->
                navigateToEditTask(task)
            },
            // 2. Click en el Checkbox (Marcar hecha)
            onTaskCheckedChange = { task, isChecked ->
                // Aquí puedes llamar al ViewModel para marcarla como hecha
                // viewModel.onTaskStatusChanged(task, isChecked)
            },
            // 3. Acción de menú (Borrar/Editar/etc)
            onMenuAction = { action ->
                // Manejar acción del menú si es necesario
            }
        )
    }

    private var selectedDate: LocalDate = LocalDate.now()
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentCalendarBinding {
        return FragmentCalendarBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()

        setupRecyclerView()
        setupCalendar()

        binding.btnToday.setOnClickListener {
            val today = LocalDate.now()
            if (selectedDate != today) {
                selectDate(today)
                binding.weekCalendarView.scrollToWeek(today)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }
    }

    private fun navigateToEditTask(task: TaskDomain) {
        // Usamos el método estático corregido
        ContainerActivity.start(requireContext(), task)
    }

    private fun setupCalendar() {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.minusMonths(100).atStartOfMonth()
        val endDate = currentMonth.plusMonths(100).atEndOfMonth()
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        binding.weekCalendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.day = data
                container.bind(data)
            }
        }

        binding.weekCalendarView.setup(startDate, endDate, firstDayOfWeek)
        binding.weekCalendarView.scrollToWeek(currentDate)

        binding.weekCalendarView.weekScrollListener = { weekDays ->
            val firstDate = weekDays.days.first().date
            val yearMonth = YearMonth.from(firstDate)
            binding.tvMonthYear.text = monthFormatter.format(yearMonth).replaceFirstChar { it.uppercase() }
        }

        selectDate(currentDate)
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            binding.weekCalendarView.notifyDateChanged(oldDate)
            binding.weekCalendarView.notifyDateChanged(date)
        }
        viewModel.getTasks(date)
    }

    override fun observe() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            // Usamos submitList (asumiendo que es un ListAdapter)
            tasksAdapter.submitList(tasks)

            binding.tvEmptyState.isVisible = tasks.isEmpty()
            binding.rvTasks.isVisible = tasks.isNotEmpty()
        }
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.tvDayText)
        val selectionBg: View = view.findViewById(R.id.selectionBg)
        lateinit var day: WeekDay

        init {
            view.setOnClickListener {
                selectDate(day.date)
            }
        }

        fun bind(data: WeekDay) {
            textView.text = data.date.dayOfMonth.toString()

            if (data.date == selectedDate) {
                textView.setTextColor(resources.getColor(R.color.white, null))
                selectionBg.visibility = View.VISIBLE
            } else {
                if (data.date == LocalDate.now()) {
                    textView.setTextColor(resources.getColor(com.google.android.material.R.color.design_default_color_primary, null))
                } else {
                    textView.setTextColor(resources.getColor(R.color.black, null))
                }
                selectionBg.visibility = View.INVISIBLE
            }
        }
    }
}