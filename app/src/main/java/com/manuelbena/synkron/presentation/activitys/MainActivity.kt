package com.manuelbena.synkron.presentation.activitys

import android.view.LayoutInflater
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.databinding.ActivityMainBinding
import com.manuelbena.synkron.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp


@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {


    override fun inflateView(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }



    override fun setUI() {
        super.setUI()
        setUpNavigationBotton()
    }

    private fun setUpNavigationBotton(){
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)


    }

}