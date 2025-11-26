package com.manuelbena.synkron.presentation.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.manuelbena.synkron.R

enum class CategoryType(
    val id: String,
    val title: String,
    val googleColorId: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val colorRes: Int
) {
    WORK("work", "Trabajo", "9", R.drawable.ic_work, R.color.color_surface),
    PERSONAL("personal", "Personal", "2", R.drawable.ic_home_black_24dp, R.color.slate),
    HEALTH("health", "Salud", "10", R.drawable.ic_health, R.color.priority_high),
    FINANCE("finance", "Dinero", "6", R.drawable.ic_banck, R.color.priority_medium), // 6 = Mandarina
    STUDY("study", "Estudios", "5", R.drawable.ic_book, R.color.priority_low), // 5 = Amarillo huevo
    OTHER("other", "Otros", "8", R.drawable.ic_other, R.color.gold_dark); //



    // Método helper para obtener todos
    companion object {
        fun getAll() = values().toList()
    }
}