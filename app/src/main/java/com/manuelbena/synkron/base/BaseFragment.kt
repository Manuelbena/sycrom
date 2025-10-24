package com.manuelbena.synkron.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding


abstract class BaseFragment<VB :  ViewBinding, VM : ViewModel> : Fragment() {

    private var _binding : ViewBinding? = null

    @Suppress("UNCHECKED_CAST")
    protected val binding
        get() = _binding as VB

    protected abstract val viewModel : VM

    protected val fragmentTag = this::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateView(inflater, container)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUI()
        setListener()
        observe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBundleArg()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun setUI(){}

    open fun setListener(){}

    open fun getBundleArg(){}

    protected abstract fun inflateView(inflater: LayoutInflater, container : ViewGroup?) : VB

    protected abstract fun observe()




}