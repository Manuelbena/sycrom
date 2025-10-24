package com.manuelbena.synkron.presentation.models

/**
 * Representa cuánto llevas gastado en cada categoría.
 *
 * @param generPercent 0-100 porcentaje ya gastado de GENERALES
 * @param ocioPercent  0-100 porcentaje ya gastado de OCIO
 * @param fijosPercent 0-100 porcentaje ya gastado de GASTOS FIJOS
 */
data class Summary(
    val generalesPercent: Int,
    val ocioPercent: Int,
    val fijosPercent: Int
)
