package com.manuelbena.synkron.presentation.util

import android.graphics.Color
import android.view.View
import android.widget.TextView
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
    private val onDaySelected: (LocalDate) -> Unit
) {

    private var selectedDate: LocalDate = LocalDate.now()
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    // Esta función reemplaza a tu antiguo "generateWeekDays"
    fun generateWeekDays() {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate = currentMonth.minusMonths(100).atStartOfMonth()
        val endDate = currentMonth.plusMonths(100).atEndOfMonth()
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        calendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.day = data
                container.bind(data)
            }
        }

        calendarView.setup(startDate, endDate, firstDayOfWeek)
        calendarView.scrollToWeek(currentDate)

        // Seleccionar hoy por defecto al iniciar
        selectDate(currentDate)
    }

    // Método público para cambiar la selección desde fuera (si lo necesitas)
    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            // Notificamos a la librería para que redibuje las celdas afectadas
            calendarView.notifyDateChanged(oldDate)
            calendarView.notifyDateChanged(date)
        }
        // Llamamos al callback
        onDaySelected(date)
    }

    // Método de compatibilidad por si lo usas en HomeFragment para setup inicial
    fun setupCalendar(initialDate: LocalDate) {
        // Si ya está generado, solo movemos la selección
        if (calendarView.adapter != null) {
            selectDate(initialDate)
            calendarView.scrollToWeek(initialDate)
        } else {
            generateWeekDays()
            selectDate(initialDate)
        }
    }

    // --- CLASE INTERNA PARA CADA CELDA DEL DÍA ---
    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.tvDayText)
        val selectionBg: View = view.findViewById(R.id.selectionBg)
        lateinit var day: WeekDay

        init {
            view.setOnClickListener {
                // Al hacer click, actualizamos la selección interna y notificamos
                val oldDate = selectedDate
                selectedDate = day.date
                calendarView.notifyDateChanged(oldDate)
                calendarView.notifyDateChanged(selectedDate)
                onDaySelected(selectedDate)
            }
        }

        fun bind(data: WeekDay) {
            this.day = data
            textView.text = data.date.dayOfMonth.toString()

            if (data.date == selectedDate) {
                // ESTILO SELECCIONADO (Fondo negro, texto blanco)
                textView.setTextColor(Color.WHITE)
                selectionBg.visibility = View.VISIBLE
                // Asegúrate de que el background tint sea el color primario o negro según tu diseño
                // selectionBg.background.setTint(...)
            } else {
                // ESTILO NORMAL
                if (data.date == LocalDate.now()) {
                    // Hoy (sin seleccionar): Color acento
                    textView.setTextColor(view.context.getColor(R.color.black)) // O tu color primario
                } else {
                    // Otro día
                    textView.setTextColor(Color.BLACK)
                }
                selectionBg.visibility = View.INVISIBLE
            }
        }
    }
}