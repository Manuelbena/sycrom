package com.manuelbena.synkron.presentation.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
// IMPORTANTE: Importamos el binding del NUEVO layout que acabamos de crear
import com.manuelbena.synkron.databinding.ItemCalendarMonthDayBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment : BaseFragment<FragmentCalendarBinding, CalendarViewModel>() {

    override val viewModel: CalendarViewModel by viewModels()

    private val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentCalendarBinding {
        return FragmentCalendarBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        // Llamamos a la configuración inicial
        setupCalendar()
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        // [AJUSTE DINÁMICO DE ALTURA]
        // Usamos 'post' para asegurarnos de que la vista ya tiene dimensiones
        binding.calendarView.post {
            // Verificamos que el binding siga activo (por si el usuario sale rápido)
            if (!isAdded) return@post

            val viewHeight = binding.calendarView.height
            // Dividimos entre 6 filas (estándar mensual) para ocupar todo el alto
            val dayHeight = (viewHeight / 6)

            // Configuramos el calendario con esta altura calculada
            configureCalendarWithHeight(startMonth, endMonth, firstDayOfWeek, currentMonth, dayHeight)
        }
    }

    private fun configureCalendarWithHeight(
        startMonth: YearMonth,
        endMonth: YearMonth,
        firstDayOfWeek: java.time.DayOfWeek,
        currentMonth: YearMonth,
        dayHeight: Int
    ) {
        binding.calendarView.dayBinder = object : MonthDayBinder<MonthDayViewContainer> {

            // 1. CREAR LA CELDA
            override fun create(view: View): MonthDayViewContainer {
                val container = MonthDayViewContainer(view)

                // Aquí aplicamos la "Magia" de la altura
                val layoutParams = container.view.layoutParams
                layoutParams.height = dayHeight
                container.view.layoutParams = layoutParams

                return container
            }

            // 2. PINTAR LA CELDA
            override fun bind(container: MonthDayViewContainer, data: CalendarDay) {
                // Usamos variables locales para facilitar la lectura y evitar errores
                val tvNumber = container.binding.tvDayNumber
                val layoutDots = container.binding.layoutEventIndicators
                val context = container.view.context

                tvNumber.text = data.date.dayOfMonth.toString()

                // Limpiamos puntitos anteriores (reciclaje)
                layoutDots.removeAllViews()

                if (data.position == DayPosition.MonthDate) {
                    // --- DÍA DEL MES ACTUAL ---
                    tvNumber.isVisible = true
                    // Hacemos visible el contenedor principal (la celda)
                    container.view.visibility = View.VISIBLE

                    // Estilo "HOY"
                    if (data.date == LocalDate.now()) {
                        tvNumber.setTextColor(android.graphics.Color.WHITE)
                        tvNumber.setBackgroundResource(R.drawable.bg_selected_day)
                    } else {
                        tvNumber.setTextColor(android.graphics.Color.BLACK)
                        tvNumber.background = null
                    }

                    // Simulación de eventos (Puntitos) - Lógica temporal
                    // (Aquí conectarías viewModel.events[data.date])
                    val eventCount = if (data.date.dayOfMonth % 3 == 0) 2 else 0

                    if (eventCount > 0) {
                        layoutDots.isVisible = true
                        repeat(eventCount) {
                            val dot = View(context).apply {
                                // Tamaño del puntito: 12x12 px (aprox 4dp)
                                layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                                    setMargins(4, 0, 4, 0)
                                }
                                setBackgroundResource(R.drawable.bg_event_dot)
                            }
                            layoutDots.addView(dot)
                        }
                    } else {
                        layoutDots.isVisible = false
                    }

                } else {
                    // --- DÍA DE OTRO MES (RELLENO) ---
                    // Estilo Google Calendar: Se ven, pero en gris
                    tvNumber.isVisible = true
                    tvNumber.setTextColor(android.graphics.Color.LTGRAY)
                    tvNumber.background = null
                    layoutDots.isVisible = false
                    container.view.visibility = View.VISIBLE
                }
            }
        }

        // Inicializamos el calendario con los parámetros
        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        // Listener para actualizar el título del mes
        binding.calendarView.monthScrollListener = { month ->
            val title = titleFormatter.format(month.yearMonth)
            binding.tvMonthTitle.text = title.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    override fun setListener() {
        super.setListener()

        binding.btnNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }

        binding.btnPrevMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }
    }

    override fun observe() {
        // Observar ViewModel aquí en el futuro
    }
}

// =========================================================================
// CLASE CONTAINER (Fuera de la clase Fragment para evitar problemas de scope)
// =========================================================================
class MonthDayViewContainer(view: View) : ViewContainer(view) {
    // Usamos el Binding del NUEVO XML 'item_calendar_month_day'
    val binding = ItemCalendarMonthDayBinding.bind(view)
}