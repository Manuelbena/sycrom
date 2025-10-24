package com.manuelbena.synkron.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.color.DynamicColors

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    private var loadNavGraph = true


    protected val binding : VB
        get() = _binding ?: throw IllegalStateException("ViewVinding is only valid between onCreateView and onDestroyView")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateView(layoutInflater)

        setContentView(_binding?.root)

    }

    override fun onStart() {
        super.onStart()
        if (loadNavGraph) setUI()
        setListener()
    }

    open fun setUI() {}

    open fun setListener() {}

    fun notReloadNavGraph() {
        loadNavGraph = false
    }

    abstract fun inflateView(inflater: LayoutInflater): VB

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}