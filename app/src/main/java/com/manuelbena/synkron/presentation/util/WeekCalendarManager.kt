package com.manuelbena.synkron.presentation.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.manuelbena.synkron.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Gestiona la lógica y la UI de un calendario semanal horizontal.
 * @param container El LinearLayout donde se inflarán los días.
 * @param onDateSelected Callback que se invoca cuando el usuario selecciona una fecha.
 */
class WeekCalendarManager(
    private val container: LinearLayout,
    private val onDateSelected: (LocalDate) -> Unit
) {
    private val context: Context = container.context
    private var selectedView: View? = null
    private val dayNameFormatter = DateTimeFormatter.ofPattern("E", Locale("es", "ES"))

    fun setupCalendar() {
        container.removeAllViews()
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        for (i in 0..6) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayView = createDayView(date)

            if (date.isEqual(today)) {
                selectDay(dayView)
            }

            container.addView(dayView)
        }
    }

    private fun createDayView(date: LocalDate): View {
        val dayView = LayoutInflater.from(context)
            .inflate(R.layout.item_calendar_dat, container, false)

        val tvDayName = dayView.findViewById<TextView>(R.id.tvDayName)
        val tvDayNumber = dayView.findViewById<TextView>(R.id.tvDayNumber)

        val dayName = dayNameFormatter.format(date)
        tvDayName.text = dayName.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        tvDayNumber.text = date.dayOfMonth.toString()

        dayView.tag = date
        dayView.setOnClickListener {
            selectDay(it)
            onDateSelected(it.tag as LocalDate)
        }
        return dayView
    }

    private fun selectDay(view: View) {
        selectedView?.let { updateDayViewState(it, isSelected = false) }
        updateDayViewState(view, isSelected = true)
        selectedView = view
    }

    private fun updateDayViewState(view: View, isSelected: Boolean) {
        val container = view.findViewById<LinearLayout>(R.id.dayContainer)
        val tvDayName = view.findViewById<TextView>(R.id.tvDayName)
        val tvDayNumber = view.findViewById<TextView>(R.id.tvDayNumber)

        val backgroundColor = if (isSelected) R.drawable.day_selected_backgorund else android.R.color.transparent
        val textColorPrimary = if (isSelected) android.R.color.white else R.color.slate
        val textColorSecondary = if (isSelected) android.R.color.white else R.color.black

        container.background = ContextCompat.getDrawable(context, backgroundColor)
        tvDayName.setTextColor(ContextCompat.getColor(context, textColorPrimary))
        tvDayNumber.setTextColor(ContextCompat.getColor(context, textColorSecondary))
    }
}