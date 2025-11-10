package com.manuelbena.synkron.presentation.util


import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

/**
 * Un OnScrollListener que crea un efecto de carrusel en un RecyclerView horizontal.
 * Aplica transformaciones de escala, opacidad y desenfoque a los ítems según su
 * distancia al centro de la pantalla.
 */
class CarouselScrollListener(
    private val minScale: Float = 0.8f,
    private val minAlpha: Float = 0.5f,
    private val maxBlur: Float = 5f
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        applyEffects(recyclerView)
    }

    private fun applyEffects(recyclerView: RecyclerView) {
        val centerX = recyclerView.width / 2f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = abs(centerX - childCenterX)

            // Factor de normalización (0 en el centro, 1 en el borde)
            // Aseguramos que centerX no sea 0 para evitar dividir por cero
            val effectFactor = if (centerX > 0) distance / centerX else 0f

            // 1. Transformación de Escala
            val scale = max(minScale, 1 - effectFactor)
            child.scaleX = scale
            child.scaleY = scale

            // 2. Transformación de Opacidad (Alpha)
            child.alpha = max(minAlpha, 1 - effectFactor)

            // 3. Transformación de Desenfoque (Blur) - API 31+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlurEffect(child, effectFactor)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyBlurEffect(child: android.view.View, effectFactor: Float) {
        val blurRadius = effectFactor * maxBlur
        val renderEffect = if (blurRadius > 0.5f) {
            RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR)
        } else {
            null // Quitar el efecto si el ítem está casi centrado.
        }
        child.setRenderEffect(renderEffect)
    }
}