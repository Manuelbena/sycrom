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
    WORK("work", "Trabajo", R.drawable.ic_work, R.color.color_surface), // Asegúrate de tener este color
    PERSONAL("personal", "Personal", R.drawable.ic_home_black_24dp, R.color.slate),
    HEALTH("health", "Salud", R.drawable.ic_health, R.color.priority_high), // Necesitarás importar iconos
    FINANCE("finance", "Dinero", R.drawable.ic_banck, R.color.priority_medium),
    STUDY("study", "Estudios", R.drawable.ic_book, R.color.priority_low),

    OTHER("other", "Otros", R.drawable.ic_other, R.color.gold_dark);



    // Método helper para obtener todos
    companion object {
        fun getAll() = values().toList()
    }
}