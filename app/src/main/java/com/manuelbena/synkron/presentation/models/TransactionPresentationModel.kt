package com.manuelbena.synkron.presentation.models

import com.manuelbena.synkron.domain.models.TransactionDomain
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionPresentationModel(
    val id: Int,
    val title: String,
    val subtitle: String, // Ej: "14:30 • Alimentación"
    val amount: Double,
    val emoji: String,    // Usaremos Emojis reales como "🍔", "🛒", "💰"
    val isIncome: Boolean // true = Verde (+), false = Oscuro (-)
) {
    val formattedAmount: String get() {
        val sign = if (isIncome) "+" else "-"
        return String.format(Locale.getDefault(), "%s %,.2f €", sign, amount)
    }
}

// 👇 ESTA ES LA FUNCIÓN QUE CONECTA LA BASE DE DATOS CON TU MODELO 👇
fun TransactionDomain.toPresentation(): TransactionPresentationModel {
    // 1. Convertimos los milisegundos a una fecha legible
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val dateString = sdf.format(Date(dateMillis))

    return TransactionPresentationModel(
        id = id,
        // Si el usuario no puso nota, le ponemos "Gasto" por defecto
        title = note.ifEmpty { "Gasto" },
        // Usamos la fecha como subtítulo (como en tu foto de diseño)
        subtitle = dateString,
        amount = amount,
        emoji = "💸", // Emoji genérico (como se ve dentro del presupuesto, el importante es el del presupuesto)
        isIncome = type == "INCOME"
    )
}