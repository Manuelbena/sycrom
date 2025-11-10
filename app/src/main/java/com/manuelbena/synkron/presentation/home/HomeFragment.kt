package com.manuelbena.synkron.presentation.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
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

/**
 * Fragment principal que muestra la vista "Home" de la aplicación.
 *
 * Responsabilidades:
 * 1. Mostrar el calendario semanal (`WeekCalendarManager`).
 * 2. Mostrar la lista de tareas del día seleccionado en un carrusel (`TaskAdapter`).
 * 3. Gestionar las interacciones del usuario (clics en FAB, selección de fecha, scroll).
 * 4. Observar y reaccionar a los eventos del [HomeViewModel].
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    // --- Propiedades ---

    override val viewModel: HomeViewModel by activityViewModels()

    /**
     * Adaptador para el RecyclerView que muestra las [TaskDomain] en el carrusel.
     */
    private val taskAdapter = TaskAdapter(
        onItemClick = { task ->
            // La lógica de mostrar el BottomSheet es responsabilidad de la UI (Fragment).
            showTaskDetail(task)
        },
        onMenuAction = { action ->
            // Delega la lógica de negocio al ViewModel.
            viewModel.onTaskMenuAction(action)
        },
        onTaskCheckedChange = { task, isDone ->
            // Delega la lógica de negocio al ViewModel.
            viewModel.onTaskCheckedChanged(task, isDone)
        }
    )

    /**
     * Gestiona la lógica y la UI del calendario semanal superior.
     */
    private lateinit var weekCalendarManager: WeekCalendarManager

    /**
     * Flag para controlar la animación del menú flotante (FAB).
     */
    private var isFabMenuOpen = false
    private val fabInterpolator = OvershootInterpolator()

    /**
     * Referencia al BottomSheet de detalle para poder cerrarlo o actualizarlo.
     */
    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null

    /**
     * Flag para forzar el scroll al inicio del carrusel (posición 0)
     * cuando se selecciona una nueva fecha.
     */
    private var shouldScrollToStart: Boolean = false

    /**
     * Recibe eventos del sistema cuando cambia la fecha (llega la medianoche)
     * para refrescar la UI y mostrar las tareas de "Hoy".
     */
    private val midnightUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_DATE_CHANGED) {
                viewModel.refreshToToday()
            }
        }
    }

    // --- Ciclo de Vida del Fragment ---

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Estructura de inicialización clara: UI, luego Listeners, luego Observadores.
        // `observe()` se llama desde BaseFragment
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        // Cuando el usuario vuelve, refrescamos a "hoy" y nos aseguramos
        // de que el calendario esté en el día correcto.
        shouldScrollToStart = true
        viewModel.refreshToToday()

        // Registramos el receptor de medianoche
        val filter = IntentFilter(Intent.ACTION_DATE_CHANGED)
        requireActivity().registerReceiver(midnightUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Es crucial des-registrar el receptor para evitar memory leaks.
        requireActivity().unregisterReceiver(midnightUpdateReceiver)
    }

    override fun onDestroyView() {
        // Limpiamos referencias a Vistas o Dialogs para evitar memory leaks.
        taskDetailBottomSheet = null
        super.onDestroyView()
    }

    // --- Configuración ---

    /**
     * Inicializa todos los componentes de la UI (RecyclerView, Calendario, FABs).
     */
    private fun setupUI() {
        setupRecyclerView()
        setupWeekCalendar()
        setupFabAnimation()
    }

    /**
     * Configura el RecyclerView principal con su LayoutManager, Adapter
     * y los helpers para el efecto carrusel (SnapHelper, Padding).
     */
    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
            // Desactivamos animaciones de item (parpadeo) al actualizar.
            itemAnimator = null
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    /**
     * Inicializa el [WeekCalendarManager] y le pasa el callback
     * que se ejecutará cuando el usuario seleccione una fecha.
     */
    private fun setupWeekCalendar() {
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate ->
            shouldScrollToStart = true // Marcamos para que el carrusel se reinicie
            viewModel.onDateSelected(selectedDate) // Informamos al ViewModel
        }
        weekCalendarManager.setupCalendar()
    }

    /**
     * Configura la animación de "shrink/extend" del FAB principal
     * al hacer scroll en el [NestedScrollView].
     */
    private fun setupFabAnimation() {
        binding.fabMain.extend()

        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY) {
                    binding.fabMain.shrink() // Scroll hacia abajo: ocultar
                } else if (scrollY < oldScrollY) {
                    binding.fabMain.extend() // Scroll hacia arriba: mostrar
                }
            }
        )
    }

    /**
     * Observador central de eventos del [HomeViewModel].
     * La vista reacciona a los estados/eventos que el ViewModel emite.
     */
    override fun observe() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is HomeEvent.ShowErrorSnackbar -> showSnackbar(event.message)
                is HomeEvent.ListTasksToday -> updateTaskList(event.list)
                is HomeEvent.NavigateToEditTask -> navigateToContainerActivity(event.task)
                is HomeEvent.UpdateHeaderText -> binding.textDate.text = event.formattedDate
                is HomeEvent.UpdateSelectedDate -> binding.tvDateTitle.text = event.formattedDate
                is HomeEvent.ShareTask -> shareTask(event.task)
                is HomeEvent.TaskUpdated -> taskDetailBottomSheet?.updateTask(event.task)
                is HomeEvent.RefreshCalendarUI -> setupWeekCalendar() // Refresca el calendario (ej. a medianoche)
                is HomeEvent.NavigateToTaskDetail -> {} // Ya gestionado por el clic del adapter
            }
        }
    }

    /**
     * Configura todos los listeners de clics de la UI.
     */
    override fun setListener() {
        binding.apply {
            // --- Listeners del Menú FAB ---
            fabMain.setOnClickListener {
                if (isFabMenuOpen) closeFabMenu() else openFabMenu()
            }

            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                navigateToContainerActivity(null) // null = Tarea nueva
            }

            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
                showSnackbar("Añadir sugerencia (lógica pendiente)")
            }
            tvFabAddGasto.setOnClickListener {
                closeFabMenu()
                showSnackbar("Añadir gasto (lógica pendiente)")
            }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
                showSnackbar("Añadir ingreso (lógica pendiente)")
            }
        }
    }

    // --- Métodos de Acción y UI ---

    /**
     * Actualiza la lista de tareas en el [taskAdapter].
     * Gestiona la visibilidad del estado "sin tareas".
     * Realiza el scroll al inicio si es necesario.
     */
    private fun updateTaskList(tasks: List<TaskDomain>) {
        val hasTasks = tasks.isNotEmpty()
        binding.ivNoTasks.isVisible = !hasTasks
        binding.tvNoTasks.isVisible = !hasTasks
        binding.recyclerViewTasks.isVisible = hasTasks

        taskAdapter.submitList(tasks)

        if (hasTasks && shouldScrollToStart) {
            binding.recyclerViewTasks.post {
                // Usamos smoothScroll para que se active el PagerSnapHelper
                binding.recyclerViewTasks.smoothScrollToPosition(0)
            }
            shouldScrollToStart = false // Reseteamos la bandera
        }
    }

    /**
     * Muestra el BottomSheet con el detalle de la tarea.
     */
    private fun showTaskDetail(task: TaskDomain) {
        taskDetailBottomSheet?.dismiss() // Cierra el anterior si estuviera abierto
        taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
        taskDetailBottomSheet?.show(childFragmentManager, TaskDetailBottomSheet.TAG)
    }

    /**
     * Navega a la [ContainerActivity] para añadir o editar una tarea.
     * @param task La tarea a editar. Si es `null`, se abre en modo "Crear Tarea".
     */
    private fun navigateToContainerActivity(task: TaskDomain?) {
        val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
            if (task != null) {
                putExtra(EDIT_TASK, task) // Modo Edición
            } else {
                putExtra(ADD_TASK, "true") // Modo Creación
            }
        }
        startActivity(intent)
    }

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

    /**
     * Muestra un [Snackbar] simple en la raíz de la vista.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // --- Animaciones del FAB ---

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMain.animate().setInterpolator(fabInterpolator).setDuration(300).start()
        showFab(binding.tvFabAddTask)
        showFab(binding.tvFabAddSuggestion)
        showFab(binding.tvFabAddGasto)
        showFab(binding.tvFabAddIng)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMain.animate().setInterpolator(fabInterpolator).setDuration(300).start()
        hideFab(binding.tvFabAddTask)
        hideFab(binding.tvFabAddSuggestion)
        hideFab(binding.tvFabAddGasto)
        hideFab(binding.tvFabAddIng)
    }

    private fun showFab(fab: View) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = 50f
        fab.animate().alpha(1f).translationY(0f).setInterpolator(fabInterpolator).setDuration(300).start()
    }

    private fun hideFab(fab: View) {
        fab.animate().alpha(0f).translationY(50f).setInterpolator(fabInterpolator).setDuration(300).withEndAction {
            fab.visibility = View.GONE
        }.start()
    }

    /**
     * Calcula el padding necesario para centrar el primer y último
     * item del carrusel en la pantalla.
     */
    private fun RecyclerView.applyCarouselPadding() {
        // (Este valor es de tu archivo original, idealmente debería estar en dimens.xml)
        val itemWidthDp = 250
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = (screenWidthPx / 2f - itemWidthPx / 2f).toInt().coerceAtLeast(0)
        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }
}