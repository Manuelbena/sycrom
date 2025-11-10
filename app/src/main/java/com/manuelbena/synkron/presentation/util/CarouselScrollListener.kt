package com.manuelbena.synkron.presentation.util


import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

class CarouselScrollListener(
    private val minScale: Float = 0.8f,
    private val minAlpha: Float = 0.5f,
    private val maxBlur: Float = 10f
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        // --- INICIO DE CAMBIOS ---
        // 1. Mover la lógica a una función reutilizable
        applyEffects(recyclerView)
        // --- FIN DE CAMBIOS ---
    }

    // --- INICIO DE CAMBIOS ---
    // 2. Añadir 'onScrollStateChanged'
    /**
     * Se llama cuando el estado del scroll cambia (ej. empieza a arrastrar, se detiene).
     */
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        // Cuando el scroll se detiene (SCROLL_STATE_IDLE),
        // volvemos a aplicar los efectos para asegurarnos de que la vista
        // que ha sido "snapped" (centrada) por el PagerSnapHelper
        // tenga el escalado y alpha correctos.
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            applyEffects(recyclerView)
        }
    }

    // 3. Crear la función 'applyEffects'
    /**
     * Aplica los efectos de escala, opacidad y desenfoque a todos los
     * hijos visibles del RecyclerView.
     */
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
    // --- FIN DE CAMBIOS ---

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyBlurEffect(view: View, effectFactor: Float) {
        val blur = effectFactor * maxBlur
        if (blur > 0) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(
                    blur,
                    blur,
                    Shader.TileMode.MIRROR
                )
            )
        } else {
            view.setRenderEffect(null)
        }
    }
}