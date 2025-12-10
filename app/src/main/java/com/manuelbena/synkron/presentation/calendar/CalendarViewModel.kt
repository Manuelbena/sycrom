package com.manuelbena.synkron.presentation.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.models.CalendarDayPresentation
import com.manuelbena.synkron.presentation.util.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel // [CRÍTICO] Necesario para que 'by viewModels()' funcione en el Fragment
class CalendarViewModel @Inject constructor(
    private val repository: ITaskRepository
) : ViewModel() {

    private val locale = Locale("es", "ES")

    // LiveData para observar desde el Fragment
    private val _calendarDays = MutableLiveData<List<CalendarDayPresentation>>()
    val calendarDays: LiveData<List<CalendarDayPresentation>> = _calendarDays

    private val _currentMonthTitle = MutableLiveData<String>()
    val currentMonthTitle: LiveData<String> = _currentMonthTitle

    private val _tasks = MutableStateFlow<Map<LocalDate, List<TaskDomain>>>(emptyMap())
    val tasks: StateFlow<Map<LocalDate, List<TaskDomain>>> = _tasks.asStateFlow()

    // 2. [NUEVO] Día Seleccionado (Por defecto Hoy)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // 3. [NUEVO] Tareas FILTRADAS para la lista (Reactivo: se actualiza si cambian tasks o selectedDate)
    // Combinamos los dos flujos para obtener la lista exacta
    val selectedDateTasks =
        kotlinx.coroutines.flow.combine(_tasks, _selectedDate) { tasksMap, date ->
            tasksMap[date] ?: emptyList()
        }


    private var currentOffset = 0
    private var selectedDay: Int? = null

    // Inicializamos cargando el mes actual
    init {
        loadMonth(0)
    }


    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }


    private var loadJob: Job? = null

    // Controlamos el mes que se está viendo para cargar datos
    /**
     * Carga las tareas para el mes visible (incluyendo días de relleno del mes anterior/siguiente).
     */
    fun loadEventsForMonth(yearMonth: YearMonth) {
        // Cancelamos la carga anterior si el usuario hace scroll rápido
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            // Calculamos el rango extendido (Mes anterior -> Mes siguiente)
            // para cubrir toda la cuadrícula visual.
            val startMonth = yearMonth.minusMonths(1)
            val endMonth = yearMonth.plusMonths(1)

            // Convertimos a milisegundos (Long) que es lo que Room entiende
            val startEpoch = startMonth.atDay(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val endEpoch = endMonth.atEndOfMonth()
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Pedimos el Flow al repositorio
            repository.getTasksBetweenDates(startEpoch, endEpoch).collect { taskList ->

                // AGRUPACIÓN LIMPIA ✨
                // Usamos tu extensión 'toLocalDate()' que ya maneja si es Long, si es todo el día, etc.
                val grouped = taskList.groupBy { task ->
                    task.start.toLocalDate()
                }

                // Emitimos el nuevo mapa
                _tasks.value = grouped
            }
        }
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