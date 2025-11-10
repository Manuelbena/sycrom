package com.manuelbena.synkron.presentation.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.ADD_TASK
// --- INICIO DE CAMBIOS ---
// 1. Añadimos el import que había eliminado
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
// --- FIN DE CAMBIOS ---
import com.manuelbena.synkron.presentation.util.EDIT_TASK
import com.manuelbena.synkron.presentation.util.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    // --- INICIO DE CAMBIOS ---
    // Restauramos las propiedades de tu HomeFragment original
    override val viewModel: HomeViewModel by activityViewModels()
    private var isFabMenuOpen = false
    private val fabInterpolator = OvershootInterpolator()

    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null

    private val taskAdapter = TaskAdapter(
        onItemClick = { task ->
            // Esta lógica de abrir el BottomSheet es de tu HomeFragment original
            taskDetailBottomSheet?.dismiss()
            taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
            taskDetailBottomSheet?.show(childFragmentManager, TaskDetailBottomSheet.TAG)
        },
        onMenuAction = { action ->
            viewModel.onTaskMenuAction(action)
        },
        onTaskCheckedChange = { task, isDone ->
            viewModel.onTaskCheckedChanged(task, isDone)
        }
    )

    private lateinit var weekCalendarManager: WeekCalendarManager
    // --- FIN DE CAMBIOS ---

    private val midnightUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_DATE_CHANGED) {
                // A medianoche, refrescamos a "hoy"
                viewModel.refreshToToday()
            }
        }
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        // Esta línea es de tu HomeFragment original, la restauramos
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- INICIO DE CAMBIOS ---
        // Usamos la estructura de tu HomeFragment original
        setupRecyclerView()
        setupWeekCalendar()
        setupFabAnimation()
        // --- FIN DE CAMBIOS ---
    }

    override fun onResume() {
        super.onResume()
        // --- INICIO DE CAMBIOS ---
        // Usamos la lógica de tu HomeFragment original
        viewModel.refreshToToday() // Usamos refreshToToday para asegurar que el calendario también se actualiza
        val filter = IntentFilter(Intent.ACTION_DATE_CHANGED)
        requireActivity().registerReceiver(midnightUpdateReceiver, filter)
        // --- FIN DE CAMBIOS ---
    }

    override fun onPause() {
        super.onPause()
        // --- INICIO DE CAMBIOS ---
        requireActivity().unregisterReceiver(midnightUpdateReceiver)
        // --- FIN DE CAMBIOS ---
    }


    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    // --- INICIO DE CAMBIOS ---
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.ListTasksToday -> {
                    val hasTasks = event.list.isNotEmpty()
                    binding.ivNoTasks.isVisible = !hasTasks
                    binding.tvNoTasks.isVisible = !hasTasks
                    binding.recyclerViewTasks.isVisible = hasTasks

                    taskAdapter.submitList(event.list)

                    if (hasTasks) {
                        binding.recyclerViewTasks.post {
                            // --- INICIO DE LA CORRECCIÓN ---
                            // Usamos smoothScrollToPosition(0) en lugar de scrollToPosition(0).
                            // Esto genera un evento de scroll, forzando al CarouselScrollListener
                            // a re-evaluar y escalar las tarjetas correctamente.
                            binding.recyclerViewTasks.smoothScrollToPosition(0)
                            // --- FIN DE LA CORRECCIÓN ---
                        }
                    }
                }

                is HomeEvent.NavigateToEditTask -> {
                    // --- INICIO DE CAMBIOS ---
                    val intent = Intent(requireContext(), ContainerActivity::class.java)
                    intent.putExtra(EDIT_TASK, event.task)
                    startActivity(intent)
                    // --- FIN DE CAMBIOS ---
                }
                is HomeEvent.UpdateHeaderText -> {
                    // --- INICIO DE CAMBIOS ---
                    binding.textDate.text = event.formattedDate
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.UpdateSelectedDate -> {
                    // --- INICIO DE CAMBIOS ---
                    binding.tvDateTitle.text = event.formattedDate
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.ShareTask -> {
                    // --- INICIO DE CAMBIOS ---
                    shareTask(event.task)
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.TaskUpdated -> {
                    // --- INICIO DE CAMBIOS ---
                    taskDetailBottomSheet?.updateTask(event.task)
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.RefreshCalendarUI -> {
                    // --- INICIO DE CAMBIOS ---
                    // Esta lógica refresca la UI del calendario
                    setupWeekCalendar()
                    // --- FIN DE CAMBIOS ---
                }

                is HomeEvent.NavigateToTaskDetail -> {
                    // --- INICIO DE CAMBIOS ---
                    // La lógica ahora está en el `onItemClick` del adapter
                    // --- FIN DE CAMBIOS ---
                }
            }
        }
    }

    override fun setListener() {
        // --- INICIO DE CAMBIOS ---
        // Restauramos la lógica de tu FAB (Floating Action Button)
        binding.apply {
            fabMain.setOnClickListener {
                if (isFabMenuOpen) {
                    closeFabMenu()
                } else {
                    openFabMenu()
                }
            }

            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                val intent = Intent(requireContext(), ContainerActivity::class.java)
                intent.putExtra(ADD_TASK, "true")
                startActivity(intent)
            }

            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
                Snackbar.make(
                    binding.root,
                    "Añadir sugerencia (lógica pendiente)",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            tvFabAddGasto.setOnClickListener {
                closeFabMenu()
            }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
            }
        }
        // --- FIN DE CAMBIOS ---
    }

    private fun setupFabAnimation() {
        // --- INICIO DE CAMBIOS ---
        // Restauramos tu animación del FAB basada en el NestedScrollView
        binding.fabMain.extend()

        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

                if (scrollY > oldScrollY) {
                    binding.fabMain.shrink()
                }
                else if (scrollY < oldScrollY) {
                    binding.fabMain.extend()
                }
            }
        )
        // --- FIN DE CAMBIOS ---
    }

    // --- INICIO DE CAMBIOS ---
    // Restauramos tus funciones del FAB
    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMain.animate()
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        showFab(binding.tvFabAddTask)
        showFab(binding.tvFabAddSuggestion)
        showFab(binding.tvFabAddGasto)
        showFab(binding.tvFabAddIng)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMain.animate()
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        hideFab(binding.tvFabAddTask)
        hideFab(binding.tvFabAddSuggestion)
        hideFab(binding.tvFabAddGasto)
        hideFab(binding.tvFabAddIng)
    }

    private fun showFab(fab: View) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = 50f

        fab.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()
    }

    private fun hideFab(fab: View) {
        fab.animate()
            .alpha(0f)
            .translationY(50f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .withEndAction {
                fab.visibility = View.GONE
            }
            .start()
    }
    // --- FIN DE CAMBIOS ---


    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupWeekCalendar() {
        // --- INICIO DE CAMBIOS ---
        // Restauramos tu implementación original
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate ->
            viewModel.onDateSelected(selectedDate)
        }
        weekCalendarManager.setupCalendar()
        // --- FIN DE CAMBIOS ---
    }

    private fun shareTask(task: TaskDomain) {
        // --- INICIO DE CAMBIOS ---
        // Restauramos tu implementación original
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "¡Echa un vistazo a mi tarea: ${task.title}!\n\n${task.description}"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
        // --- FIN DE CAMBIOS ---
    }

    // --- INICIO DE CAMBIOS ---
    // 3. Restauramos tu función de padding
    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthDp = 300 // (Este valor es de tu archivo original)
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt().coerceAtLeast(0)
        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }
    // --- FIN DE CAMBIOS ---

    override fun onDestroyView() {
        // --- INICIO DE CAMBIOS ---
        taskDetailBottomSheet = null // De tu archivo original
        // --- FIN DE CAMBIOS ---
        super.onDestroyView()
    }
}