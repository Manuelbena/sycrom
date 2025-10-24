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
import kotlin.math.min

class FinancePiePorcentel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Porcentaje por categoría (0-100, se normalizan si no suman 100) */
    private var generales = 20f
    private var ocio       = 30f
    private var fijos      = 50f

    private var animProgress = 0f

    private val donutStroke = 60f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = donutStroke
        strokeCap = Paint.Cap.BUTT
    }
    private val rectF = RectF()

    /** Colores corporativos */
    private val colorGenerales = Color.parseColor("#0F2744") // navy
    private val colorOcio      = Color.parseColor("#2E9C8B") // teal
    private val colorFijos     = Color.parseColor("#D4AF37") // amber

    /**
     * @param generales%  ej. 20f  (Generales)
     * @param ocio%       ej. 30f  (Ocio)
     * @param fijos%      ej. 50f  (Gastos fijos)
     */
    fun setData(generales: Float, ocio: Float, fijos: Float) {
        var total = generales + ocio + fijos
        if (total <= 0f) return              // evita división por 0

        // Normaliza a 100 si fuera necesario
        this.generales = (generales / total) * 100f
        this.ocio      = (ocio       / total) * 100f
        this.fijos     = (fijos      / total) * 100f

        // Reinicia animación
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

        val size   = min(width, height)
        val radius = (size / 2f) - donutStroke
        val cx = width / 2f
        val cy = height / 2f
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Ángulo acumulativo para ir dibujando cada sector
        var startAngle = -90f

        fun drawSlice(percent: Float, color: Int) {
            if (percent <= 0f) return
            val sweep = percent / 100f * 360f * animProgress
            paint.color = color
            canvas.drawArc(rectF, startAngle, sweep, false, paint)
            startAngle += sweep
        }

        // Orden: Generales → Ocio → Fijos
        drawSlice(generales, colorGenerales)
        drawSlice(ocio,      colorOcio)
        drawSlice(fijos,     colorFijos)
    }
}
