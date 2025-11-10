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

import com.manuelbena.synkron.domain.models.SubTaskDomain // <-- MODIFICADO
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

