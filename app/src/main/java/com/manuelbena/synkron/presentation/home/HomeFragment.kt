package com.manuelbena.synkron.presentation.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
// --- AÑADIDOS ---
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
// --- FIN AÑADIDOS ---
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
import kotlinx.coroutines.launch // <-- AÑADIDO

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

    override fun setUI() {
        super.setUI()
        setupRecyclerView()
        setupWeekCalendar()
    }

    // --- ¡AQUÍ ESTÁ LA CORRECCIÓN DEL ERROR! ---
    override fun observe() {
        // Usamos '.observe' de LiveData en lugar de '.collect' de Flow
        viewModel.event.observe(viewLifecycleOwner) { event ->
            // El tipo 'event' se infiere correctamente como HomeEvent
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                }

                is HomeEvent.ListTasksToday -> {
                    val hasTasks = event.list.isNotEmpty()
                    binding.recyclerViewTasks.isVisible = hasTasks
                    binding.tvNoTasks.isVisible = !hasTasks
                    binding.ivNoTasks.isVisible = !hasTasks
                    taskAdapter.submitList(event.list)
                }

                is HomeEvent.NavigateToEditTask -> {
                    val intent = Intent(requireContext(), ContainerActivity::class.java)
                    intent.putExtra(EDIT_TASK, event.task)
                    startActivity(intent)
                }

                // --- MANEJO DE FECHAS ---
                is HomeEvent.UpdateHeaderText -> {
                    // 1. Actualiza el saludo "Hola Manuel, [FECHA DE HOY]"
                    binding.textDate.text = event.formattedDate
                }

                is HomeEvent.UpdateSelectedDate -> {
                    // 2. Actualiza el título de la tarjeta "Tareas programadas"
                    binding.tvDateTitle.text = event.formattedDate
                }
                // --- FIN MANEJO DE FECHAS ---

                is HomeEvent.ShareTask -> {
                    shareTask(event.task)
                }

                is HomeEvent.TaskUpdated -> {
                    taskDetailBottomSheet?.updateTask(event.task)
                }

                is HomeEvent.NavigateToTaskDetail -> {
                    // Esta lógica ya la manejamos en el init del adapter
                }
            }
        }
    }
    // --- FIN DE LA CORRECCIÓN ---

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
            buttonAddTaskSuggestion.setOnClickListener {
                // Lógica para añadir la tarea sugerida
            }
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMain.animate()
            .rotation(45f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        showFab(binding.tvFabAddTask, binding.tvFabAddTask)
        showFab(binding.tvFabAddSuggestion, binding.tvFabAddSuggestion)
        showFab(binding.tvFabAddGasto, binding.tvFabAddGasto)
        showFab(binding.tvFabAddIng, binding.tvFabAddIng)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMain.animate()
            .rotation(0f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        hideFab(binding.tvFabAddTask, binding.tvFabAddTask)
        hideFab(binding.tvFabAddSuggestion, binding.tvFabAddSuggestion)
        hideFab(binding.tvFabAddGasto, binding.tvFabAddGasto)
        hideFab(binding.tvFabAddIng, binding.tvFabAddIng)
    }

    private fun showFab(fab: View, textView: View) {
        fab.visibility = View.VISIBLE
        textView.visibility = View.VISIBLE
        fab.alpha = 0f
        textView.alpha = 0f
        fab.translationY = 50f
        textView.translationY = 50f

        fab.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        textView.animate()
            .alpha(1f)
            .translationY(0f)
            .setInterpolator(fabInterpolator)
            .setDuration(3)
            .start()
    }

    private fun hideFab(fab: View, textView: View) {
        fab.animate()
            .alpha(0f)
            .translationY(50f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .withEndAction {
                fab.visibility = View.INVISIBLE
                textView.visibility = View.INVISIBLE
            }
            .start()

        textView.animate()
            .alpha(0f)
            .translationY(50f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
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

