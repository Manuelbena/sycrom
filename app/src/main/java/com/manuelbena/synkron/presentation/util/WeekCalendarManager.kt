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
    private val daysOfWeek = getWeekDays()
    private val dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale("es", "ES"))
    private val dayNumberFormatter = DateTimeFormatter.ofPattern("d", Locale("es", "ES"))

    /**
     * Infla y configura los 7 días de la semana actual.
     */
    fun setupCalendar() {
        container.removeAllViews()
        daysOfWeekViews.clear()

        val inflater = LayoutInflater.from(context)
        for (day in daysOfWeek) {
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

            if (day == LocalDate.now()) {
                selectDay(view) // Selecciona el día de hoy por defecto
            }
        }
    }

    /**
     * Obtiene la lista de 7 días (LocalDate) para la semana actual.
     */
    private fun getWeekDays(): List<LocalDate> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
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

    // --- INICIO DE LA CORRECCIÓN ---
    /**
     * Actualiza la apariencia visual de un día (seleccionado o no).
     */
    private fun updateDayViewState(view: View, isSelected: Boolean) {
        val container = view.findViewById<LinearLayout>(R.id.dayContainer)
        val tvDayName = view.findViewById<TextView>(R.id.tvDayName)
        val tvDayNumber = view.findViewById<TextView>(R.id.tvDayNumber)

        val backgroundColorRes: Int
        val textColorPrimary: Int
        val textColorSecondary: Int

        if (isSelected) {
            backgroundColorRes = R.drawable.day_selected_backgorund
            // Usamos los colores 'onPrimary' para el día seleccionado (texto blanco)
            textColorPrimary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
            textColorSecondary = ContextCompat.getColor(context, R.color.md_theme_onPrimary)
        } else {
            // Usamos 'transparent' de Android para el fondo
            backgroundColorRes = android.R.color.transparent

            // ¡ESTA ES LA CORRECCIÓN!
            // Usamos colores del tema que existen en 'values' y 'values-night'
            // Tus `colors.xml` SÍ tienen estos valores.
            textColorPrimary = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant) // Color "gris"
            textColorSecondary = ContextCompat.getColor(context, R.color.md_theme_onSurface) // Color "normal"
        }

        // Usamos setBackgroundResource en lugar de .background
        container.setBackgroundResource(backgroundColorRes)
        tvDayName.setTextColor(textColorPrimary)
        tvDayNumber.setTextColor(textColorSecondary)
    }
    // --- FIN DE LA CORRECCIÓN ---
}