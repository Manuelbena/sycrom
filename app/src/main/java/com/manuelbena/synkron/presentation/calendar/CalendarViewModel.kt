package com.manuelbena.synkron.presentation.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.manuelbena.synkron.presentation.models.CalendarDayPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel // [CRÍTICO] Necesario para que 'by viewModels()' funcione en el Fragment
class CalendarViewModel @Inject constructor() : ViewModel() {

    private val locale = Locale("es", "ES")

    // LiveData para observar desde el Fragment
    private val _calendarDays = MutableLiveData<List<CalendarDayPresentation>>()
    val calendarDays: LiveData<List<CalendarDayPresentation>> = _calendarDays

    private val _currentMonthTitle = MutableLiveData<String>()
    val currentMonthTitle: LiveData<String> = _currentMonthTitle

    private var currentOffset = 0
    private var selectedDay: Int? = null

    // Inicializamos cargando el mes actual
    init {
        loadMonth(0)
    }

    fun loadMonth(offset: Int) {
        currentOffset = offset

        val base = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val year = base.get(Calendar.YEAR)
        val month = base.get(Calendar.MONTH)
        val maxDays = base.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Título: "Agosto 2025"
        val rawTitle = SimpleDateFormat("LLLL yyyy", locale).format(base.time)
        _currentMonthTitle.value = rawTitle.replaceFirstChar { it.uppercase() }

        // Calcular espacios vacíos al inicio (Lunes=0, Martes=1...)
        val firstDayOfWeek = base.get(Calendar.DAY_OF_WEEK)
        // Ajuste: En Calendar.java Domingo es 1, Lunes es 2.
        // Queremos Lunes=0.
        // Formula: (Day - 2 + 7) % 7
        // Ej: Lunes(2) -> (2-2+7)%7 = 0 (0 espacios)
        // Ej: Martes(3) -> (3-2+7)%7 = 1 (1 espacio)
        // Ej: Domingo(1) -> (1-2+7)%7 = 6 (6 espacios)
        val emptyCells = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        val daysList = mutableListOf<CalendarDayPresentation>()

        // 1. Celdas vacías
        repeat(emptyCells) {
            daysList.add(CalendarDayPresentation.Empty)
        }

        // 2. Días del mes
        val todayCalendar = Calendar.getInstance()
        for (day in 1..maxDays) {
            val isToday = (year == todayCalendar.get(Calendar.YEAR) &&
                    month == todayCalendar.get(Calendar.MONTH) &&
                    day == todayCalendar.get(Calendar.DAY_OF_MONTH))

            daysList.add(
                CalendarDayPresentation.Date(
                    day = day,
                    isToday = isToday,
                    isSelected = (day == selectedDay),
                    hasEvents = false, // Conectarás esto luego
                    eventsCount = 0
                )
            )
        }

        _calendarDays.value = daysList
    }

    fun nextMonth() = loadMonth(currentOffset + 1)
    fun prevMonth() = loadMonth(currentOffset - 1)
}