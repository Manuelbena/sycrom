package com.manuelbena.synkron.presentation.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.manuelbena.synkron.presentation.models.CalendarDayPresentation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class CalendarViewModel : ViewModel() {

    private val locale = Locale("es", "ES")

    // Estado UI
    private val _calendarDays = MutableLiveData<List<CalendarDayPresentation>>()
    val calendarDays: LiveData<List<CalendarDayPresentation>> = _calendarDays

    private val _currentMonthTitle = MutableLiveData<String>()
    val currentMonthTitle: LiveData<String> = _currentMonthTitle

    // Desplazamiento respecto al mes actual (0 = mes actual; +1 siguiente; -1 anterior)
    private var currentOffset = 0

    // Día seleccionado (opcional)
    private var selectedDay: Int? = null

    /**
     * Eventos agrupados por mes. Clave: "yyyy-MM" -> (día -> nº eventos)
     * Ej: "2025-08" -> { 13:1, 21:2, 26:1, 28:3 }
     */
    private val monthEvents: MutableMap<String, Map<Int, Int>> = mutableMapOf()

    fun loadMonth(offset: Int = currentOffset) {
        currentOffset = offset

        val base = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val year = base.get(Calendar.YEAR)
        val month0 = base.get(Calendar.MONTH) // 0..11
        val maxDays = base.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Título "agosto 2025" -> "Agosto 2025"
        val raw = SimpleDateFormat("LLLL yyyy", locale).format(base.time)
        _currentMonthTitle.value = raw.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }

        // Offset de celdas vacías (lunes = 0, martes = 1, ...)
        val firstDow = base.get(Calendar.DAY_OF_WEEK)
        val blanks = (firstDow - Calendar.MONDAY + 7) % 7

        val today = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }

        val monthKey = "%04d-%02d".format(locale, year, month0 + 1)
        val eventsForMonth = monthEvents[monthKey] ?: emptyMap()

        val items = ArrayList<CalendarDayPresentation>(blanks + maxDays)
        repeat(blanks) { items.add(CalendarDayPresentation.Empty) }

        for (d in 1..maxDays) {
            base.set(Calendar.DAY_OF_MONTH, d)

            val isToday =
                base.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        base.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        base.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

            val count = eventsForMonth[d] ?: 0

            items.add(
                CalendarDayPresentation.Date(
                    day = d,
                    isToday = isToday,
                    hasEvents = count > 0,
                    eventsCount = count,
                    isSelected = (selectedDay == d)
                )
            )
        }

        _calendarDays.value = items
    }

    fun nextMonth() = loadMonth(currentOffset + 1)
    fun prevMonth() = loadMonth(currentOffset - 1)

    fun selectDay(day: Int) {
        selectedDay = day
        loadMonth(currentOffset) // reconstruye la lista marcando selección
    }

    /**
     * Inyecta/actualiza los eventos de un mes concreto.
     * @param year Año (ej: 2025)
     * @param month0Based Mes 0..11 (ej: Calendar.AUGUST)
     * @param dayToCount Mapa día -> nº de eventos (1..31)
     */
    fun setEventsForMonth(year: Int, month0Based: Int, dayToCount: Map<Int, Int>) {
        val key = "%04d-%02d".format(locale, year, month0Based + 1)
        monthEvents[key] = dayToCount

        // Si el mes actualizado es el que está en pantalla, refrescamos
        val probe = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.MONTH, currentOffset)
        }
        if (probe.get(Calendar.YEAR) == year && probe.get(Calendar.MONTH) == month0Based) {
            loadMonth(currentOffset)
        }
    }
}