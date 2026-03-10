package com.manuelbena.synkron.presentation.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

/**
 * Modelo de datos interno para las porciones del gráfico.
 * Se define fuera para evitar recreación innecesaria.
 */
data class PieSlice(
    val value: Float,
    val color: Int
)

class FinancePiePorcentel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Lista dinámica de porciones
    private var slices: List<PieSlice> = emptyList()
    private var animProgress = 0f

    private val donutStroke = 60f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = donutStroke
        strokeCap = Paint.Cap.ROUND // Cap redondeado para un acabado UI más moderno
    }
    private val rectF = RectF()

    /**
     * @param data Lista de [PieSlice] con el valor (gasto) y color de cada presupuesto.
     * La clase se encarga de calcular los porcentajes automáticamente.
     */
    fun setData(data: List<PieSlice>) {
        val total = data.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) {
            this.slices = emptyList()
            invalidate()
            return
        }

        // Normalización y mapeo dinámico
        this.slices = data.map { slice ->
            slice.copy(value = (slice.value / total) * 100f)
        }

        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (slices.isEmpty()) return

        val size = min(width, height)
        val radius = (size / 2f) - (donutStroke / 2f) - 10f // Ajuste de padding interno
        val cx = width / 2f
        val cy = height / 2f
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        var startAngle = -90f

        slices.forEach { slice ->
            if (slice.value <= 0f) return@forEach

            val sweep = (slice.value / 100f) * 360f * animProgress
            paint.color = slice.color

            canvas.drawArc(rectF, startAngle, sweep, false, paint)

            // Incrementamos el ángulo para la siguiente porción
            startAngle += (slice.value / 100f) * 360f
        }
    }
}