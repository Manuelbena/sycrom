package com.manuelbena.synkron.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding, SettingsViewModel>() {

    override val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        // Inicializar los items del menú con sus respectivos iconos y títulos
        setupMenuItems()
    }

    private fun setupMenuItems() {
        binding.itemAccount.apply {
            ivIcon.setImageResource(R.drawable.ic_note)
            tvTitle.text = "Cuenta"
        }

        binding.itemPersonalization.apply {
            ivIcon.setImageResource(R.drawable.sparkle)
            tvTitle.text = "Personalización"
        }

        binding.itemNotifications.apply {
            ivIcon.setImageResource(R.drawable.ic_clock)
            tvTitle.text = "Notificaciones"
        }

        binding.itemHelp.apply {
            ivIcon.setImageResource(R.drawable.ic_info)
            tvTitle.text = "Ayuda"
        }
    }

    override fun setListener() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.itemAccount.root.setOnClickListener {
            showToast("Cuenta (Próximamente)")
        }

        binding.itemPersonalization.root.setOnClickListener {
            showToast("Personalización (Próximamente)")
        }

        binding.itemNotifications.root.setOnClickListener {
            showToast("Notificaciones (Próximamente)")
        }

        binding.itemHelp.root.setOnClickListener {
            showToast("Ayuda (Próximamente)")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun observe() {
        // No hay estados que observar por ahora
    }
}
