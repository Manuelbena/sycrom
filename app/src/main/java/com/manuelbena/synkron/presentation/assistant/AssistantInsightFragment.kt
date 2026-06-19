package com.manuelbena.synkron.presentation.assistant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentAssistantInsightBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssistantInsightFragment : BaseFragment<FragmentAssistantInsightBinding, AssistantViewModel>() {

    override val viewModel: AssistantViewModel by viewModels()
    private lateinit var adapter: AssistantInsightAdapter

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentAssistantInsightBinding {
        return FragmentAssistantInsightBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        adapter = AssistantInsightAdapter { id ->
            viewModel.deleteInsight(id)
        }
        binding.rvInsights.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInsights.adapter = adapter
    }

    override fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.insights.collectLatest { list ->
                    adapter.submitList(list)
                    binding.lyNoInsights.isVisible = list.isEmpty()
                    binding.rvInsights.isVisible = list.isNotEmpty()
                }
            }
        }
    }
}
