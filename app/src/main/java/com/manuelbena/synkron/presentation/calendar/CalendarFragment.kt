package com.manuelbena.synkron.presentation.calendar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
import com.manuelbena.synkron.databinding.ItemCalendarMonthDayBinding
import com.manuelbena.synkron.presentation.util.getCategoryColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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
        setupCalendar()
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        // 1. Cargar datos iniciales
        viewModel.loadEventsForMonth(currentMonth)

        // 2. Configurar calendario (con ajuste de altura diferido)
        binding.calendarView.post {
            if (!isAdded ) return@post

            val viewHeight = binding.calendarView.height
            val dayHeight = if (viewHeight > 0) (viewHeight / 6) else ViewGroup.LayoutParams.WRAP_CONTENT

            configureCalendarWithHeight(startMonth, endMonth, firstDayOfWeek, currentMonth, dayHeight)
        }
    }

    private fun configureCalendarWithHeight(
        startMonth: YearMonth,
        endMonth: YearMonth,
        firstDayOfWeek: java.time.DayOfWeek,
        currentMonth: YearMonth,
        dayHeight: Int // Puede ser un valor fijo o WRAP_CONTENT si el cálculo falla
    ) {
        binding.calendarView.dayBinder = object : MonthDayBinder<MonthDayViewContainer> {

            override fun create(view: View): MonthDayViewContainer {
                val container = MonthDayViewContainer(view)
                // Aplicar altura calculada
                if (dayHeight is Int && dayHeight > 0) {
                    val params = container.view.layoutParams
                    params.height = dayHeight
                    container.view.layoutParams = params
                }
                return container
            }

            override fun bind(container: MonthDayViewContainer, data: CalendarDay) {
                val tvNumber = container.binding.tvDayNumber
                val layoutDots = container.binding.layoutEventIndicators
                val context = container.view.context

                tvNumber.text = data.date.dayOfMonth.toString()
                layoutDots.removeAllViews()

                // Recuperamos tareas
                val tasksForDay = viewModel.tasks.value[data.date] ?: emptyList()

                if (data.position == DayPosition.MonthDate) {
                    tvNumber.isVisible = true
                    container.view.visibility = View.VISIBLE

                    // Estilo HOY
                    if (data.date == LocalDate.now()) {
                        tvNumber.setTextColor(Color.WHITE)
                        tvNumber.setBackgroundResource(R.drawable.bg_selected_day)
                    } else {
                        tvNumber.setTextColor(Color.BLACK)
                        tvNumber.background = null
                    }

                    // --- PINTAR PUNTOS CON COLOR DE CATEGORÍA ---
                    if (tasksForDay.isNotEmpty()) {
                        layoutDots.isVisible = true
                        // Limitamos a 4 puntos
                        tasksForDay.take(4).forEach { task ->
                            val dot = View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                                    setMargins(3, 0, 3, 0)
                                }

                                // Detectar si es todo el día
                                val isAllDay = task.start?.date != null && task.start?.dateTime == null

                                if (isAllDay) {
                                    // [OPCIÓN 1] PUNTITO CUADRADO para diferenciar
                                    // Necesitarías crear un drawable 'bg_event_square.xml' o usar shape dinámico
                                    setBackgroundResource(R.drawable.bg_event_square)

                                    // O simplemente cambiar la opacidad
                                    alpha = 0.6f
                                } else {
                                    setBackgroundResource(R.drawable.bg_event_dot)
                                    alpha = 1.0f
                                }

                                // Drawable circular blanco base
                                setBackgroundResource(R.drawable.bg_event_dot)

                                // APLICAR COLOR DE LA TAREA
                                try {
                                    val categoryName = task.typeTask
                                    if (categoryName.isNotEmpty()) {
                                        // 1. Obtenemos el ID del recurso (R.color.xxx)
                                        val colorResId = categoryName.getCategoryColor()

                                        // 2. [CORRECCIÓN] Convertimos ese ID en un Color Real
                                        val colorInt = ContextCompat.getColor(context, colorResId)

                                        // 3. Pintamos
                                        background.setTint(colorInt)
                                    } else {
                                        background.setTint(Color.LTGRAY)
                                    }
                                } catch (e: Exception) {
                                    background.setTint(Color.LTGRAY)
                                }
                            }
                            layoutDots.addView(dot)
                        }
                    } else {
                        layoutDots.isVisible = false
                    }

                } else {
                    // Días de relleno
                    tvNumber.isVisible = true
                    tvNumber.setTextColor(Color.LTGRAY)
                    tvNumber.background = null
                    layoutDots.isVisible = false
                    container.view.visibility = View.VISIBLE
                }
            }
        }

        // Setup final
        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        // Scroll listener para cargar datos al cambiar de mes
        binding.calendarView.monthScrollListener = { month ->
            val title = titleFormatter.format(month.yearMonth)
            binding.tvMonthTitle.text = title.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            viewModel.loadEventsForMonth(month.yearMonth)
        }
    }

    override fun observe() {
        // --- FIX DEL CRASH AQUÍ ---
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasks.collect {
                // Solo notificamos si el calendario ya tiene adaptador (setup completado)
                if (binding.calendarView.adapter != null) {
                    binding.calendarView.notifyCalendarChanged()
                }
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
}

class MonthDayViewContainer(view: View) : ViewContainer(view) {
    val binding = ItemCalendarMonthDayBinding.bind(view)
}