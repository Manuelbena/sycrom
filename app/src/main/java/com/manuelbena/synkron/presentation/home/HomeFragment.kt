package com.manuelbena.synkron.presentation.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView // ¡AÑADIDO! Import necesario
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.EDIT_TASK
import com.manuelbena.synkron.presentation.util.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by activityViewModels()
    private var isFabMenuOpen = false
    private val fabInterpolator = OvershootInterpolator()

    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null

    private val taskAdapter = TaskAdapter(
        onItemClick = { task ->
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

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupWeekCalendar()
        setupFabAnimation() // ¡AÑADIDO! Llamamos a la nueva función de animación
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }


    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                }

                is HomeEvent.ListTasksToday -> {

                    val hasTasks = event.list.isNotEmpty()
                    // ¡AÑADIDO! He unido tus 3 líneas en una para más limpieza
                    binding.ivNoTasks.isVisible = !hasTasks
                    binding.tvNoTasks.isVisible = !hasTasks
                    binding.recyclerViewTasks.isVisible = hasTasks

                    taskAdapter.submitList(event.list)
                }

                is HomeEvent.NavigateToEditTask -> {
                    val intent = Intent(requireContext(), ContainerActivity::class.java)
                    intent.putExtra(EDIT_TASK, event.task)
                    startActivity(intent)
                }

                is HomeEvent.UpdateHeaderText -> {
                    binding.textDate.text = event.formattedDate
                }

                is HomeEvent.UpdateSelectedDate -> {
                    binding.tvDateTitle.text = event.formattedDate
                }

                is HomeEvent.ShareTask -> {
                    shareTask(event.task)
                }

                is HomeEvent.TaskUpdated -> {
                    taskDetailBottomSheet?.updateTask(event.task)
                }

                is HomeEvent.NavigateToTaskDetail -> {
                    // Lógica manejada en el click del adapter
                }
            }
        }
    }

    override fun setListener() {
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
    }

    // --- ¡FUNCIÓN NUEVA AÑADIDA! ---
    /**
     * Configura el NestedScrollView para que escuche el scroll
     * y anime el FAB principal (fabMain) para encogerse o extenderse.
     */
    private fun setupFabAnimation() {
        // Aseguramos que el botón empiece extendido
        binding.fabMain.extend()

        // Escuchamos el scroll del NestedScrollView (usando el ID 'nested_scroll_view' del XML)
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

                // Si el usuario baja (el scroll nuevo 'Y' es mayor que el anterior)
                if (scrollY > oldScrollY) {
                    // Encogemos el botón
                    binding.fabMain.shrink()
                }
                // Si el usuario sube (el scroll nuevo 'Y' es menor que el anterior)
                else if (scrollY < oldScrollY) {
                    // Extendemos el botón
                    binding.fabMain.extend()
                }
            }
        )
    }
    // --- FIN DE LA FUNCIÓN AÑADIDA ---


    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMain.animate()
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        // Pequeña corrección: Tu 'showFab' toma dos vistas pero las usas como una.
        // Lo he limpiado para que solo pases la vista que quieres mostrar.
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

        // Igual aquí, he limpiado la llamada
        hideFab(binding.tvFabAddTask)
        hideFab(binding.tvFabAddSuggestion)
        hideFab(binding.tvFabAddGasto)
        hideFab(binding.tvFabAddIng)
    }

    // ¡AÑADIDO! Firma de la función simplificada
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

    // ¡AÑADIDO! Firma de la función simplificada
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


    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupWeekCalendar() {
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate ->
            viewModel.onDateSelected(selectedDate)
        }
        weekCalendarManager.setupCalendar()
    }

    private fun shareTask(task: TaskDomain) {
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
    }

    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthDp = 300
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt().coerceAtLeast(0)
        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }

    override fun onDestroyView() {
        taskDetailBottomSheet = null
        super.onDestroyView()
    }
}