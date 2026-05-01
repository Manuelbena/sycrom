package com.manuelbena.synkron.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewbinding.ViewBinding

/**
 * Base Activity to handle ViewBinding and system bar styling.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding should not be accessed after onDestroy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateView(layoutInflater)
        setContentView(binding.root)
        setupSystemBars()
        setUI()
        setListener()
    }

    private fun setupSystemBars() {
        val isDarkMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
    }

    /**
     * Optional method to setup the initial UI.
     */
    open fun setUI() {}

    /**
     * Optional method to setup listeners.
     */
    open fun setListener() {}

    /**
     * Inflates the ViewBinding for this activity.
     */
    abstract fun inflateView(inflater: LayoutInflater): VB

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
