package com.manuelbena.synkron.presentation.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentCalendarBinding
import com.manuelbena.synkron.databinding.ItemCalendarMonthDayBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.calendar.adapter.CalendarTaskAdapter
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.getCategoryColor
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@AndroidEntryPoint
class CalendarFragment : BaseFragment<FragmentCalendarBinding, CalendarViewModel>() {

    override val viewModel: CalendarViewModel by viewModels()
    private val titleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null
    private lateinit var tasksAdapter: CalendarTaskAdapter

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentCalendarBinding {
        return FragmentCalendarBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupRecycler()
        setupCalendar()
    }

    private fun setupCalendar() {
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        viewModel.loadEventsForMonth(currentMonth)

        binding.calendarView.post {
            if (!isAdded) return@post
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
        dayHeight: Int
    ) {
        binding.calendarView.dayBinder = object : MonthDayBinder<MonthDayViewContainer> {

            override fun create(view: View): MonthDayViewContainer {
                val container = MonthDayViewContainer(view)
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

                // Resetear estados visuales
                tvNumber.text = data.date.dayOfMonth.toString()
                tvNumber.setTextColor(Color.BLACK) // Color base
                tvNumber.background = null
                layoutDots.removeAllViews()

                // Recuperar tareas del día
                val tasksForDay = viewModel.tasks.value[data.date] ?: emptyList()

                if (data.position == DayPosition.MonthDate) {
                    tvNumber.isVisible = true
                    container.view.visibility = View.VISIBLE

                    // --- 1. SEPARAR TIPOS DE TAREAS ---
                    // Todo el día (para el fondo)
                    val allDayTask = tasksForDay.firstOrNull {
                        it.start?.date != null && it.start?.dateTime == null
                    }
                    // Tareas con hora (para los puntos)
                    val timedTasks = tasksForDay.filter {
                        it.start?.dateTime != null
                    }

                    // --- 2. FONDO DE LA CELDA (Logica Todo el Día) ---
                    if (allDayTask != null) {
                        try {
                            val colorResId = allDayTask.typeTask.getCategoryColor()
                            val colorInt = ContextCompat.getColor(context, colorResId)

                            // [TOQUE PREMIUM] Aplicar Alpha (25% opacidad)
                            val pastelColor = adjustAlpha(colorInt, 0.30f)
                            container.view.setBackgroundColor(pastelColor)

                            // Texto en el color fuerte de la categoría para armonía
                            // (O puedes dejarlo negro si prefieres contraste máximo)
                            tvNumber.setTextColor(colorInt)

                        } catch (e: Exception) {
                            container.view.setBackgroundColor(Color.LTGRAY)
                        }
                    } else {
                        // Fondo normal (blanco con borde)
                        container.view.setBackgroundResource(R.drawable.bg_calendar_cell_border)
                        tvNumber.setTextColor(Color.BLACK)
                    }

                    // --- 3. ESTILO "HOY" (Siempre encima de todo) ---
                    if (data.date == LocalDate.now()) {
                        tvNumber.setTextColor(Color.WHITE)
                        tvNumber.setBackgroundResource(R.drawable.bg_selected_day)
                    }

                    // --- 4. PUNTOS (Tareas normales con hora) ---
                    if (timedTasks.isNotEmpty()) {
                        layoutDots.isVisible = true
                        timedTasks.take(4).forEach { task ->
                            val dot = View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                                    setMargins(3, 0, 3, 0)
                                }
                                setBackgroundResource(R.drawable.bg_event_dot)

                                try {
                                    val categoryName = task.typeTask
                                    if (categoryName.isNotEmpty()) {
                                        val colorResId = categoryName.getCategoryColor()
                                        val colorInt = ContextCompat.getColor(context, colorResId)
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
                    container.view.setBackgroundResource(R.drawable.bg_calendar_cell_border)
                    container.view.visibility = View.VISIBLE
                }

                // [NUEVO] GESTIÓN DE SELECCIÓN VISUAL
                // Obtenemos el día seleccionado actual del VM
                val selectedDate = viewModel.selectedDate.value

                if (data.date == selectedDate) {
                    // Si es el día seleccionado, le ponemos un borde o fondo especial
                    // Ejemplo: Un borde azul fuerte
                    tvNumber.setTextColor(Color.WHITE)
                    container.view.setBackgroundResource(R.drawable.day_selected_backgorund)
                }

                // [NUEVO] CLICK LISTENER
                container.view.setOnClickListener {
                    // 1. Actualizar VM
                    viewModel.selectDate(data.date)

                    // 2. Refrescar calendario para actualizar el borde de selección
                    binding.calendarView.notifyCalendarChanged()

                    // 3. [TRUCO DE ANIMACIÓN] Forzar la animación de la lista
                    // Al cambiar los datos, el Adapter se actualiza, pero para ver la escalera
                    // a veces ayuda forzarlo:
                    binding.rvCalendarTasks.scheduleLayoutAnimation()
                }
            }

        }

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.monthScrollListener = { month ->
            val title = titleFormatter.format(month.yearMonth)
            binding.tvMonthTitle.text = title.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            viewModel.loadEventsForMonth(month.yearMonth)
        }
    }

    /**
     * Función auxiliar para añadir transparencia a un color.
     * @param color El color original (Int ARGB).
     * @param factor Factor de opacidad (0.0 a 1.0). Ej: 0.2f es 20% visible.
     */
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    override fun observe() {
        // 1. Observar mapa de tareas (Puntitos)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasks.collect {
                // BLINDAJE 🛡️
                if (binding.calendarView.adapter != null) {
                    binding.calendarView.notifyCalendarChanged()
                }
            }
        }

        // 2. Observar lista filtrada (RecyclerView)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.selectedDateTasks.collect { tasks ->
                val date = viewModel.selectedDate.value
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es", "ES"))

                binding.tvSelectedDateTitle.text = if (date == LocalDate.now()) {
                    "Hoy"
                } else {
                    date.format(formatter).replaceFirstChar { it.uppercase() }
                }

                tasksAdapter.submitList(tasks) {
                    binding.rvCalendarTasks.scheduleLayoutAnimation()
                }
            }
        }

        // 3. Observar día seleccionado (Borde de selección)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.selectedDate.collect {
                // BLINDAJE AQUÍ TAMBIÉN 🛡️ (Este era el que fallaba)
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

    private fun setupRecycler() {
        // Inicializamos el NUEVO adaptador
        tasksAdapter = CalendarTaskAdapter(
            onItemClick = { task ->
                // Navegar a detalle
                showTaskDetail(task)

            },
            onTaskCheckedChange = { task, isDone ->
                // Actualizar estado en ViewModel (necesitarás crear este método en el VM)
                // viewModel.updateTaskStatus(task, isDone)
            }
        )

        binding.rvCalendarTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
            scheduleLayoutAnimation()
        }
    }

    private fun showTaskDetail(task: TaskDomain) {
        taskDetailBottomSheet?.dismiss()
        taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
        taskDetailBottomSheet?.show(childFragmentManager, "TaskDetailBottomSheet")
    }
}

class MonthDayViewContainer(view: View) : ViewContainer(view) {
    val binding = ItemCalendarMonthDayBinding.bind(view)
}