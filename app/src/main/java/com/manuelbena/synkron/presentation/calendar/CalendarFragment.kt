package com.manuelbena.synkron.presentation.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
import com.manuelbena.synkron.presentation.models.CalendarDayPresentation
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.models.SubTaskPresentation
import com.manuelbena.synkron.domain.models.TaskDomain

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(CalendarViewModel::class.java)

        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val calendarGrid = binding.calendarGrid

        viewModel.currentMonthTitle.observe(viewLifecycleOwner) { title ->
            binding.tvMonthTitle.text = title
        }

        val tasks = listOf(
            TaskDomain(
                hour = 20251008,
                date = 20251008, // Formato AAAA MM DD
                title = "Sincronización Semanal del Proyecto",
                description = "Revisar los avances del sprint actual y planificar las próximas tareas.",
                typeTask = "Trabajo",
                place = "Oficina, Sala de Juntas 3",
                subTasks = listOf(
                    SubTaskPresentation(title = "Preparar métricas de rendimiento", isDone = true),
                    SubTaskPresentation(title = "Revisar bloqueos del equipo", isDone = false),
                    SubTaskPresentation(title = "Definir próximos pasos", isDone = false)
                ),
                isActive = true, // La tarea está en curso o es la siguiente
                isDone = false ,  // Aún no ha sido completada
                duration = 9
            ),
            TaskDomain(
                hour = 20251008,
                date = 20251010,
                title = "Cita con el Dentista",
                description = "Revisión y limpieza anual.",
                typeTask = "Salud",
                place = "Clínica Dental 'Sonrisa Sana'",
                subTasks = emptyList(), // No tiene subtareas
                isActive = false,
                isDone = false,
                duration = 9
            ),
            TaskDomain(
                hour = 20251008,
                date = 20251006, // Una fecha pasada
                title = "Estudiar para el examen de Álgebra",
                description = "Repasar los capítulos 4 y 5.",
                typeTask = "Estudio",
                place = "Biblioteca Central",
                subTasks = listOf(
                    SubTaskPresentation(title = "Resumir capítulo 4", isDone = true),
                    SubTaskPresentation(title = "Hacer ejercicios del capítulo 5", isDone = true)
                ),
                isActive = false, // Ya no está activa porque pasó la fecha
                isDone = true ,    // La marcamos como completada
                duration = 9
            )
        )

        // En tu Fragment o Activity (Home)

        val taskAdapter = TaskAdapter({}) // Tu adaptador que hereda de ListAdapter (o tu BaseAdapter)

        val recyclerView: RecyclerView = binding.recyclerViewTasks
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = taskAdapter

        taskAdapter.submitList(tasks) // ListAdapter se encargará de las diferencias y animaciones

        viewModel.calendarDays.observe(viewLifecycleOwner) {
            renderCalendar(calendarGrid, it)
        }

        viewModel.loadMonth()

        return root
    }

    private fun renderCalendar(grid: GridLayout, days: List<CalendarDayPresentation>) {
        // Elimina los días anteriores (dejando los headers fijos)
        grid.removeViews(7, grid.childCount - 7)

        for (day in days) {
            val textView = TextView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(100, 100)
                gravity = Gravity.CENTER
                textSize = 16f

                when (day) {
                    is CalendarDayPresentation.Date -> {
                        text = day.day.toString()
                        if (day.isToday) {
                            setBackgroundResource(R.drawable.bg_selected_day)
                            setTextColor(Color.WHITE)
                        } else {
                            setBackgroundColor(Color.TRANSPARENT)
                            setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                    }

                    is CalendarDayPresentation.Empty -> {
                        text = ""
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                }
            }

            grid.addView(textView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}