package com.manuelbena.synkron.presentation.activitys

import android.view.LayoutInflater

import androidx.navigation.findNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ActivityContainerBinding
import com.manuelbena.synkron.base.BaseActivity
import com.manuelbena.synkron.presentation.util.ADD_MONEY
import com.manuelbena.synkron.presentation.util.ADD_TASK
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ContainerActivity : BaseActivity<ActivityContainerBinding>(){


    override fun inflateView(inflater: LayoutInflater)= ActivityContainerBinding.inflate(inflater)


    override fun setUI() {
        intent.getStringExtra(ADD_TASK)?.let {
            setAddTaskFragmetn()
        } ?: run {
            intent.getStringExtra(ADD_MONEY)?.let {
                setAddMoneyFragmetn()
            }
        }
    }

    private fun setAddTaskFragmetn(){
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_task)
    }

    private fun setAddMoneyFragmetn(){
        binding.fragmentContainerView.findNavController().setGraph(R.navigation.new_money)
    }
}