package com.manuelbena.synkron.presentation.calendar

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@AndroidEntryPoint
class CalendarFragment(override val viewModel: CalendarViewModel) : BaseFragment<FragmentCalendarBinding, CalendarViewModel>() {

    // Clase contenedora para las celdas (View Holder)
    class DayViewContainer(view: View) : ViewContainer(view) {
        val tvDayText: TextView = view.findViewById(R.id.tvDayText)
        val viewSelectionBackground: View = view.findViewById(R.id.viewSelectionBackground)
        val viewAllDayBorder: View = view.findViewById(R.id.viewAllDayBorder)
        val layoutTaskPreview: LinearLayout = view.findViewById(R.id.layoutTaskPreview)
        lateinit var day: CalendarDay // Referencia al día actual
    }

    override fun setUI() = with(binding) {
        // 1. CONFIGURACIÓN DEL BINDER (EL CEREBRO VISUAL)
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val context = container.view.context

                // A) HACER LA CELDA CUADRADA
                val params = container.view.layoutParams
                params.height = container.view.width
                container.view.layoutParams = params

                // B) DATOS BÁSICOS
                container.tvDayText.text = data.date.dayOfMonth.toString()

                // Limpiar vistas recicladas
                container.viewAllDayBorder.visibility = View.INVISIBLE
                container.layoutTaskPreview.removeAllViews()

                if (data.position == DayPosition.MonthDate) {
                    container.tvDayText.setTextColor(Color.BLACK)

                    // AQUÍ IRÁ LA LÓGICA DE TAREAS (TODO: Conectar con ViewModel)
                    // Por ahora dejo la estructura lista para recibir datos

                    /* EJEMPLO DE CÓMO SE VERÁ (Descomentar para probar visualmente):
                    if (data.date.dayOfMonth == 15) {
                        // Simular Tarea Todo el día
                        container.viewAllDayBorder.visibility = View.VISIBLE
                        (container.viewAllDayBorder.background as GradientDrawable).setStroke(5, Color.RED)

                        // Simular Micro-Tarjeta
                        val taskView = TextView(context)
                        taskView.text = "Reunión importante"
                        taskView.textSize = 9f
                        taskView.maxLines = 1
                        taskView.ellipsize = TextUtils.TruncateAt.END
                        taskView.setTextColor(Color.WHITE)
                        val shape = GradientDrawable().apply {
                            cornerRadius = 8f
                            setColor(Color.RED)
                        }
                        taskView.background = shape
                        container.layoutTaskPreview.addView(taskView)
                    }
                    */

                    // C) SELECCIÓN
                    // if (data.date == viewModel.selectedDate) ...

                } else {
                    container.tvDayText.setTextColor(Color.LTGRAY)
                }
            }
        }

        // 2. CONFIGURACIÓN INICIAL DEL CALENDARIO
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        // Titulo inicial
        val title = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
        tvMonthTitle.text = title.replaceFirstChar { it.uppercase() }

        // 3. LISTENERS DE NAVEGACIÓN
        btnNextMonth.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let {
                calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }
        btnPrevMonth.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let {
                calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        // Listener para actualizar título al hacer scroll
        calendarView.monthScrollListener = { month ->
            val newTitle = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}"
            tvMonthTitle.text = newTitle.replaceFirstChar { it.uppercase() }
        }
    }

    override fun inflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarBinding {
        TODO("Not yet implemented")
    }

    override fun observe() {
        TODO("Not yet implemented")
    }
}