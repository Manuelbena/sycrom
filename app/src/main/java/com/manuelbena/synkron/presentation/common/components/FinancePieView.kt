package com.manuelbena.synkron.presentation.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.manuelbena.synkron.R
import kotlin.math.min

class FinancePieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var ingresos: Float = 0f
    private var gastos: Float = 0f
    private var animProgress: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 60f
    }

    private val rectF = RectF()

    fun setData(ingresos: Float, gastos: Float) {
        this.ingresos = ingresos
        this.gastos = gastos.coerceAtMost(ingresos)

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

        if (ingresos == 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - 40f

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Fondo completo (ingresos)
        paint.color = Color.parseColor("#A7C4AD") // Esmeralda
        paint.style = Paint.Style.STROKE
        canvas.drawArc(rectF, -90f, 360f, false, paint)

        // Porción de gastos encima
        val angleGastos = (gastos / ingresos) * 360f * animProgress
        paint.color = Color.parseColor("#B0422C") // amber
        canvas.drawArc(rectF, -90f, angleGastos, false, paint)

        // Texto en el centro: disponible = ingresos - gastos
        val disponible = ingresos - gastos
        paint.apply {
            style = Paint.Style.FILL
            textSize = 48f
            color = resources.getColor(R.color.background_primary)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("€${disponible.toInt()}", centerX, centerY + 16f, paint)
    }
}


