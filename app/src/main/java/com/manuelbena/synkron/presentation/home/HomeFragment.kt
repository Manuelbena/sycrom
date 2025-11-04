package com.manuelbena.synkron.presentation.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain // <-- AÑADIDO (si no estaba)
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.EDIT_TASK // <-- AÑADIDO
import com.manuelbena.synkron.presentation.util.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by viewModels()
    private var isFabMenuOpen = false
    private val fabInterpolator = OvershootInterpolator() // Para animación "expressive"

    // --- MODIFICACIÓN AQUÍ ---
    // Ahora inicializamos el adapter con las tres lambdas:
    private val taskAdapter = TaskAdapter(
        // 1. onItemClick: Para abrir el BottomSheet
        onItemClick = { task ->
            TaskDetailBottomSheet.newInstance(task).show(
                parentFragmentManager,
                TaskDetailBottomSheet.TAG
            )
        },
        // 2. onMenuAction: Para manejar las acciones del menú
        onMenuAction = { action ->
            // Simplemente pasamos la acción al ViewModel
            viewModel.onTaskMenuAction(action)
        },
        // 3. onTaskCheckedChange: Para manejar el click en el checkbox
        onTaskCheckedChange = { task, isDone ->
            viewModel.onTaskCheckedChanged(task, isDone)
        }
    )
    // --- FIN DE MODIFICACIÓN ---

    private lateinit var weekCalendarManager: WeekCalendarManager


    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupRecyclerView()
        setupWeekCalendar()
    }

    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                }

                is HomeEvent.NavigateToTaskDetail -> {
                    // Lógica de navegación si es necesaria
                }

                is HomeEvent.ListTasksToday -> {
                    binding.recyclerViewTasks.visibility =
                        if (event.list.isEmpty()) View.INVISIBLE else View.VISIBLE
                    taskAdapter.submitList(event.list)
                }

                // --- AÑADIR ESTOS CASOS PARA EL MENÚ ---
                is HomeEvent.NavigateToEditTask -> {
                    // El ViewModel nos pide navegar a Editar
                    val intent = Intent(requireContext(), ContainerActivity::class.java)
                    // Usamos la constante EDIT_TASK y pasamos el objeto Task
                    // (Asegúrate de que TaskDomain sea Parcelable)
                    intent.putExtra(EDIT_TASK, event.task)
                    startActivity(intent)
                }

                is HomeEvent.ShareTask -> {
                    // El ViewModel nos pide compartir
                    shareTask(event.task)
                }
                // --- FIN DE CASOS AÑADIDOS ---

                // --- AÑADIDO PARA ACTUALIZAR BOTTOMSHEET ---
                is HomeEvent.TaskUpdated -> {
                    // Buscamos el BottomSheet por su TAG
                    val bottomSheet =
                        parentFragmentManager.findFragmentByTag(TaskDetailBottomSheet.TAG) as? TaskDetailBottomSheet
                    // Si está abierto, le pasamos la tarea actualizada
                    bottomSheet?.updateTask(event.task)
                }
                // --- FIN DE CASO AÑADIDO ---
            }
        }
    }

    override fun setListener() {
        binding.apply {
            // --- Lógica del Menú FAB ---
            fabMain.setOnClickListener {
                if (isFabMenuOpen) {
                    closeFabMenu()
                } else {
                    openFabMenu()
                }
            }

            // Listeners para los mini FABs
            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                val intent = Intent(requireContext(), ContainerActivity::class.java)
                intent.putExtra(ADD_TASK, "true")
                startActivity(intent)
            }

            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
                // Lógica para añadir la tarea sugerida
                // (La misma que tenías en buttonAddTaskSuggestion)
                Snackbar.make(
                    binding.root,
                    "Añadir sugerencia (lógica pendiente)",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            tvFabAddGasto.setOnClickListener {
                closeFabMenu()
                // Lógica para añadir la tarea sugerida
            }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
                // Lógica para añadir la tarea sugerida
            }
            buttonAddTaskSuggestion.setOnClickListener {
                // Lógica para añadir la tarea sugerida
            }
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        // Rotar el FAB principal a 45 grados (forma de 'X')
        binding.fabMain.animate()
            .rotation(45f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        // Mostrar y animar mini FABs y textos
        showFab(binding.tvFabAddTask, binding.tvFabAddTask)
        showFab(binding.tvFabAddSuggestion, binding.tvFabAddSuggestion)
        showFab(binding.tvFabAddGasto, binding.tvFabAddGasto)
        showFab(binding.tvFabAddIng, binding.tvFabAddIng)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        // Rotar el FAB principal de vuelta a 0 grados
        binding.fabMain.animate()
            .rotation(0f)
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .start()

        // Ocultar y animar mini FABs y textos
        hideFab(binding.tvFabAddTask, binding.tvFabAddTask)
        hideFab(binding.tvFabAddSuggestion, binding.tvFabAddSuggestion)
        hideFab(binding.tvFabAddGasto, binding.tvFabAddGasto)
        hideFab(binding.tvFabAddIng, binding.tvFabAddIng)
    }

    private fun showFab(fab: View, textView: View) {
        fab.visibility = View.VISIBLE
        textView.visibility = View.VISIBLE // Mostrar texto
        fab.alpha = 0f
        textView.alpha = 0f
        fab.translationY = 50f // Empezar un poco abajo
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
            .setDuration(300)
            .start()
    }

    private fun hideFab(fab: View, textView: View) {
        fab.animate()
            .alpha(0f)
            .translationY(50f) // Mover hacia abajo al ocultar
            .setInterpolator(fabInterpolator)
            .setDuration(300)
            .withEndAction {
                fab.visibility = View.INVISIBLE
                textView.visibility = View.INVISIBLE // Ocultar texto
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
            adapter = taskAdapter // Asignar el adaptador ya inicializado
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

    // --- AÑADIR ESTA FUNCIÓN HELPER ---
    /**
     * Crea un Intent para compartir el contenido de una tarea.
     */
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
    // --- FIN DE FUNCIÓN HELPER ---


    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthDp = 300
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt().coerceAtLeast(0)
        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }
}
