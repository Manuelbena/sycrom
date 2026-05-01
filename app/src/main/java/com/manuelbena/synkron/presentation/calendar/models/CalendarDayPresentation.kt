package com.manuelbena.synkron.presentation.models

sealed class CalendarDayPresentation {
    data class Date(
        val day: Int,
        val isToday: Boolean = false,
        val hasEvents: Boolean = false,
        val eventsCount: Int = 0,
        val isSelected: Boolean = false
    ) : CalendarDayPresentation()

    object Empty : CalendarDayPresentation()
}