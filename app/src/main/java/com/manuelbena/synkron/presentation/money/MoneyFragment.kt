package com.manuelbena.synkron.presentation.money

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manuelbena.synkron.R

import com.manuelbena.synkron.databinding.FragmentMoneyBinding
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.MoneyViewModel
import com.manuelbena.synkron.presentation.models.ItemMoneyPresentation
import com.manuelbena.synkron.presentation.models.Summary
import com.manuelbena.synkron.presentation.models.TypeSpent
import com.manuelbena.synkron.presentation.util.ADD_MONEY
import com.manuelbena.synkron.presentation.util.ADD_TASK
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {


    override val viewModel: MoneyViewModel by viewModels()

    override fun inflateView(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentMoneyBinding {
        return FragmentMoneyBinding.inflate(inflater, container, false)
    }

    override fun observe() {
        viewModel.text.observe(viewLifecycleOwner) {

        }
    }

    override fun setListener() {
        super.setListener()

        binding.floatingActionButton.setOnClickListener {
            val intent = Intent(requireContext(), ContainerActivity::class.java)
            intent.putExtra(ADD_MONEY, "true")
            startActivity(intent)
        }
    }

    override fun setUI() {
        super.setUI()

        setAdapter()

        // Donut fijo 20/30/50
        binding.financePie.setData(generales = 20f, ocio = 30f, fijos = 50f)
        // 1- Gráfico donut (distribución teórica 20/30/50)
        binding.financePie.setData(
            generales = 20f,
            ocio = 30f,
            fijos = 50f
        )

        // 2- Datos reales: qué porcentaje YA llevamos gastado
        val resumenActual = Summary(
            generalesPercent = 40,
            ocioPercent = 65,
            fijosPercent = 55
        )
        updateBars(resumenActual)


    }

    private fun updateBars(summary: Summary) {
        binding.barGenerales.setProgressCompat(summary.generalesPercent, /*animated=*/true)
        binding.barOcio.setProgressCompat(summary.ocioPercent, true)
        binding.barFijos.setProgressCompat(summary.fijosPercent, true)
    }

    private fun setAdapter() {
        val itemMoney = listOf(
            ItemMoneyPresentation(R.drawable.ic_note, "Reunión casfdasdfasdfsdafsdfdsfsdfasdfsadfasdfdsfsadfsadfasdfdsfdfsafsdfdsafdasfsdfafasfdsfsadsfasfsadfasfdon diseño", 399, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.baseline_edit_calendar_24, "500", -4, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.ic_home_black_24dp, "Alguiler", 4, TypeSpent.FIJOS),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                0,
                TypeSpent.GENERALES,
            ),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                455,
                TypeSpent.GENERALES,
            ),
            ItemMoneyPresentation(R.drawable.ic_note, "Reunión con diseño", 399, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.baseline_edit_calendar_24, "500", -4, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.ic_home_black_24dp, "Alguiler", 4, TypeSpent.FIJOS),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                0,
                TypeSpent.GENERALES,
            ),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                455,
                TypeSpent.GENERALES,
            ),
            ItemMoneyPresentation(R.drawable.ic_note, "Reunión casdfasdfasdfasdfsadfsadfsdafsdfsdfsdfsdafsdon diseño", 399, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.baseline_edit_calendar_24, "500", -4, TypeSpent.OCIO),
            ItemMoneyPresentation(R.drawable.ic_home_black_24dp, "Alguiler", 4, TypeSpent.FIJOS),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                0,
                TypeSpent.GENERALES,
            ),
            ItemMoneyPresentation(
                R.drawable.ic_dashboard_black_24dp,
                "Llamada con cliente",
                455,
                TypeSpent.GENERALES,
            ),

            )

        // En tu Fragment o Activity (Home)

        val moneyAdapter =
            ItemMoneyAdapter() // Tu adaptador que hereda de ListAdapter (o tu BaseAdapter)

        val recyclerView: RecyclerView = binding.rvExpenses
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = moneyAdapter
        moneyAdapter.submitList(itemMoney) // ListAdapter se encargará de las diferencias y animaciones
    }


}