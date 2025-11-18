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
    private val daysOfWeekViews = mutableListOf<View>()

    // --- CAMBIO: Guardamos la semana que se está mostrando ---
    private var currentWeekDays: List<LocalDate> = emptyList()

    private val dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))
    private val dayNumberFormatter = DateTimeFormatter.ofPattern("d", Locale("es", "ES"))

    /**
     * Infla y configura los 7 días de la semana que contiene 'dateForWeek'.
     */
    // --- CAMBIO: La función ahora acepta una fecha para saber qué semana pintar ---
    fun setupCalendar(dateForWeek: LocalDate) {
        container.removeAllViews()
        daysOfWeekViews.clear()

        // --- CAMBIO: Obtenemos la semana correcta, no solo la de "hoy" ---
        currentWeekDays = getWeekDaysFor(dateForWeek)

        val inflater = LayoutInflater.from(context)
        for (day in currentWeekDays) {
            val view = inflater.inflate(R.layout.item_calendar_dat, container, false)
            val tvDayName = view.findViewById<TextView>(R.id.tvDayName)
            val tvDayNumber = view.findViewById<TextView>(R.id.tvDayNumber)

            tvDayName.text = day.format(dayNameFormatter).replaceFirstChar { it.uppercase() }
            tvDayNumber.text = day.format(dayNumberFormatter)

            view.tag = day // Almacenamos el LocalDate en el tag
            view.setOnClickListener {
                selectDay(it)
                onDateSelected(day)
            }

            container.addView(view)
            daysOfWeekViews.add(view)
        }
    }

    /**
     * Obtiene la lista de 7 días (LocalDate) para la semana que CONTIENE la fecha dada.
     */
    // --- CAMBIO: Nueva función (antes 'getWeekDays') ---
    private fun getWeekDaysFor(date: LocalDate): List<LocalDate> {
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    /**
     * Gestiona la selección visual de un día.
     */
    private fun selectDay(view: View) {
        selectedView?.let { updateDayViewState(it, isSelected = false) }
        updateDayViewState(view, isSelected = true)
        selectedView = view
    }

    // --- ¡¡NUEVA FUNCIÓN MÁGICA!! ---
    /**
     * Permite seleccionar una fecha desde fuera (Fragment).
     * Si la fecha no pertenece a la semana actual, RECONSTRUYE el calendario
     * para esa nueva semana y luego la selecciona.
     */
    fun selectDate(date: LocalDate) {
        // Comprobar si la fecha está en la semana que se muestra actualmente
        if (currentWeekDays.contains(date)) {
            // Sí está: solo la seleccionamos visualmente
            val viewToSelect = daysOfWeekViews.find { it.tag as? LocalDate == date }
            if (viewToSelect != null && viewToSelect != selectedView) {
                selectDay(viewToSelect)
            }
        } else {
            // No está: hay que RECONSTRUIR el calendario para la nueva semana
            setupCalendar(date) // Reconstruye para la semana de 'date'

            // Y ahora que está reconstruido, la seleccionamos
            val viewToSelect = daysOfWeekViews.find { it.tag as? LocalDate == date }
            if (viewToSelect != null) {
                selectDay(viewToSelect)
            }
        }
    }

    /**
     * Actualiza la apariencia visual de un día (seleccionado o no).
     */
    private fun updateDayViewState(view: View, isSelected: Boolean) {
        val containerView = view.findViewById<LinearLayout>(R.id.dayContainer) // Cambié 'container' a 'containerView'
        val tvDayName = view.findViewById<TextView>(R.id.tvDayName)
        val tvDayNumber = view.findViewById<TextView>(R.id.tvDayNumber)

        val backgroundColorRes: Int
        val textColorPrimary: Int
        val textColorSecondary: Int

        if (isSelected) {
            backgroundColorRes = R.drawable.day_selected_backgorund
            textColorPrimary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
            textColorSecondary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
        } else {
            backgroundColorRes = android.R.color.transparent
            textColorPrimary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
            textColorSecondary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
        }

        containerView.setBackgroundResource(backgroundColorRes)
        tvDayName.setTextColor(textColorPrimary)
        tvDayNumber.setTextColor(textColorSecondary)
    }
}