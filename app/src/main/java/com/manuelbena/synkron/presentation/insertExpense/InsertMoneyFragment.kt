package com.manuelbena.synkron.presentation.insertExpense

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.manuelbena.synkron.databinding.FramgmentNewMoneyBinding
import com.manuelbena.synkron.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InsertMoneyFragment : BaseFragment<FramgmentNewMoneyBinding, InsertMoneyViewModel>() {


    override val viewModel: InsertMoneyViewModel by viewModels()

    override fun inflateView(
        inflater: LayoutInflater, container: ViewGroup?
    ): FramgmentNewMoneyBinding {
        return FramgmentNewMoneyBinding.inflate(inflater, container, false)
    }

    override fun observe() {

    }

}