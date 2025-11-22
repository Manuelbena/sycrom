package com.manuelbena.synkron.presentation.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.manuelbena.synkron.R

enum class CategoryType(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val colorRes: Int
) {
    WORK("work", "Trabajo", R.drawable.ic_dashboard_black_24dp, R.color.priority_low_bg), // Asegúrate de tener este color
    PERSONAL("personal", "Personal", R.drawable.ic_home_black_24dp, R.color.slate),
    HEALTH("health", "Salud", R.drawable.baseline_edit_calendar_24, R.color.priority_high), // Necesitarás importar iconos
    FINANCE("finance", "Dinero", R.drawable.ic_attach_money, R.color.priority_medium),
    STUDY("study", "Estudios", R.drawable.ic_calendar_today, R.color.color_primary);

    // Método helper para obtener todos
    companion object {
        fun getAll() = values().toList()
    }
}