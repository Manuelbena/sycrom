package com.manuelbena.synkron.presentation.util

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.atStartOfMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.WeekCalendarView
import com.kizitonwose.calendar.view.WeekDayBinder
import com.manuelbena.synkron.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeekCalendarManager(
    private val calendarView: WeekCalendarView,
    private val onDaySelected: (LocalDate) -> Unit,
    private val onMonthChanged: (String) -> Unit,
    private val context: Context
) {

    private var selectedDate: LocalDate = LocalDate.now()
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun generateWeekDays() {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.minusMonths(100).atStartOfMonth()
        val endDate = currentMonth.plusMonths(100).atEndOfMonth()
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.bind(data)
            }
        }

        calendarView.weekScrollListener = { weekDays ->
            val firstDate = weekDays.days.first().date
            val monthTitle = monthFormatter.format(firstDate)
            onMonthChanged(monthTitle.replaceFirstChar { it.uppercase() })
        }

        calendarView.setup(startDate, endDate, firstDayOfWeek)
        calendarView.scrollToWeek(currentDate)

        // Seleccionamos la fecha actual por defecto
        selectDate(currentDate)
    }

    // --- ESTA ES LA FUNCIÓN QUE FALTABA ---
    fun setupCalendar(initialDate: LocalDate) {
        // Si el calendario ya tiene adaptador, significa que ya se generó
        if (calendarView.adapter != null) {
            selectDate(initialDate)
            calendarView.scrollToWeek(initialDate)
        } else {
            // Si no, lo generamos primero
            generateWeekDays()
            // Y luego forzamos la fecha inicial que nos pide el fragmento
            selectDate(initialDate)
            calendarView.scrollToWeek(initialDate)
        }
    }
    // --------------------------------------

    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            calendarView.notifyDateChanged(oldDate)
            calendarView.notifyDateChanged(date)
        }
        onDaySelected(date)
    }

    fun scrollToToday() {
        val today = LocalDate.now()
        selectDate(today)
        calendarView.smoothScrollToWeek(today)
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.tvDayText)
        val selectionBg: View = view.findViewById(R.id.selectionBg)
        lateinit var day: WeekDay

        init {
            view.setOnClickListener {
                if (selectedDate != day.date) {
                    val oldDate = selectedDate
                    selectedDate = day.date
                    calendarView.notifyDateChanged(oldDate)
                    calendarView.notifyDateChanged(selectedDate)
                    onDaySelected(selectedDate)
                }
            }
        }

        fun bind(data: WeekDay) {
            this.day = data
            textView.text = data.date.dayOfMonth.toString()

            val isSelected = (data.date == selectedDate)
            val isToday = (data.date == LocalDate.now())

            selectionBg.visibility = View.VISIBLE

            when {
                // 1. SELECCIONADO (Prioridad máxima)
                isSelected -> {
                    textView.setTextColor(Color.WHITE)
                    selectionBg.background.setTint((ContextCompat.getColor(context, R.color.md_theme_onSecondary)))
                    selectionBg.alpha = 1f
                }

                // 2. ES HOY (Pero no seleccionado) -> Fondo gris suave
                isToday -> {
                    textView.setTextColor(Color.BLACK)
                    selectionBg.background.setTint((ContextCompat.getColor(context, R.color.md_theme_onSecondary)))
                    selectionBg.alpha = 0.5f
                }

                // 3. NORMAL
                else -> {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.md_theme_tertiary))
                    selectionBg.visibility = View.INVISIBLE
                }
            }
        }
    }
}