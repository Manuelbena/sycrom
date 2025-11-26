package com.manuelbena.synkron.presentation.home


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
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
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.task_ia.TaskIaBottomSheet

import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY


import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toCalendar
import com.manuelbena.synkron.presentation.util.toDurationString
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect // --- CAMBIO IMPORTANTE: NO 'collectLatest' ---
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by activityViewModels()
    private var isFabMenuOpen = false

    private lateinit var weekCalendarManager: WeekCalendarManager
    private val fabInterpolator = OvershootInterpolator()

    private var lastSnappedPosition = RecyclerView.NO_POSITION
    private var shouldScrollToStart: Boolean = false
    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null

    private val taskAdapter = TaskAdapter(
        onItemClick = { task ->
            showTaskDetail(task)
        },
        onMenuAction = { action ->
            viewModel.onTaskMenuAction(action)
        },
        onTaskCheckedChange = { task, isDone ->
            viewModel.onTaskCheckedChanged(task, isDone)
        }
    )

    private val midnightUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_DATE_CHANGED) {
                viewModel.refreshToToday()
            }
        }
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate: LocalDate ->
            shouldScrollToStart = true
            viewModel.onDateSelected(selectedDate)
        }
        setupRecyclerView()
        setupFabAnimation()
        setupDotIndicatorListener()
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(Intent.ACTION_DATE_CHANGED)
        requireActivity().registerReceiver(midnightUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(midnightUpdateReceiver)
    }


    /**
     * Observa el StateFlow y el Action LiveData del ViewModel.
     */
    override fun observe() {
        // --- OBSERVADOR DE ESTADO (StateFlow) ---

        // --- CAMBIO: Flag para inicializar el calendario solo una vez ---
        var isCalendarInitialized = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // --- CAMBIO: de 'collectLatest' a 'collect' ---
                viewModel.uiState.collect { state ->

                    // --- CAMBIO: Inicializar el calendario con el estado del VM ---
                    if (!isCalendarInitialized) {
                        weekCalendarManager.setupCalendar(state.selectedDate)
                        isCalendarInitialized = true
                    }

                    updateUi(state)
                }
            }
        }

        // --- OBSERVADOR DE ACCIÓN (SingleLiveEvent) ---
        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is HomeAction.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, action.message, Snackbar.LENGTH_SHORT).show()
                }
                is HomeAction.NavigateToEditTask -> {
                    navigateToContainerActivity(action.task)
                }
                is HomeAction.ShareTask -> {
                    shareTask(action.task)
                }
            }
        }
    }

    /**
     * Función centralizada para actualizar la UI basada en el HomeState.
     *
     * ⬇️ SECCIÓN MODIFICADA ⬇️
     */
    private fun updateUi(state: HomeState) {
        // Actualizaciones de texto (sin cambios)
        binding.textDate.text = state.headerText
        binding.tvDateTitle.text = state.selectedDate.format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("es", "ES"))
        )

        // Lógica de Carga (sin cambios)
        binding.progressIndicator.isVisible = state.isLoading

        if (state.isLoading) {
            binding.recyclerViewTasks.isVisible = false
            binding.ivNoTasks.isVisible = false
            binding.tvNoTasks.isVisible = false
            binding.tabLayoutDots.isVisible = false
        } else {
            val hasTasks = state.tasks.isNotEmpty()
            binding.ivNoTasks.isVisible = !hasTasks
            binding.tvNoTasks.isVisible = !hasTasks
            binding.recyclerViewTasks.isVisible = hasTasks
            binding.tabLayoutDots.isVisible = hasTasks

            taskAdapter.submitList(state.tasks)

            if (binding.tabLayoutDots.tabCount != state.tasks.size) {
                binding.tabLayoutDots.removeAllTabs()

                // Bucle para añadir cada punto como una "vista personalizada"
                state.tasks.forEach { _ ->
                    val newTab = binding.tabLayoutDots.newTab()
                    newTab.setCustomView(R.layout.dot_indicator_layout)
                    binding.tabLayoutDots.addTab(newTab)
                }
            }

            // 5. Lógica de scroll (movida aquí para que solo se ejecute
            //    cuando la carga ha terminado y hay tareas)
            if (shouldScrollToStart && hasTasks) {
                binding.recyclerViewTasks.scrollToPosition(0)
                shouldScrollToStart = false
}
        }
        // --- FIN DEL CAMBIO ---

        // Sincroniza la UI del calendario con el estado (sin cambios)
        weekCalendarManager.selectDate(state.selectedDate)
    }
    // ⬆️ SECCIÓN MODIFICADA ⬆️

    override fun setListener() {
        binding.apply {
            fabMain.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (isFabMenuOpen) closeFabMenu() else openFabMenu()
            }

            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                navigateToContainerActivity(null) // null = Tarea Nueva
            }

            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
               showAiButton()
            }
            tvFabAddGasto.setOnClickListener {
                closeFabMenu()
                // Lógica futura
            }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
                showAiButton()
            }
        }
    }

    private fun setupFabAnimation() {
        binding.fabMain.extend()
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY) binding.fabMain.shrink()
                else if (scrollY < oldScrollY) binding.fabMain.extend()
            }
        )
    }

    // --- Funciones del FAB (open, close, show, hide) ---
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


    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutManager = linearLayoutManager
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null

            // Lógica de vibración y SINCRONIZACIÓN DE PUNTOS
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                        val centerView = snapHelper.findSnapView(linearLayoutManager) ?: return
                        val newPosition = linearLayoutManager.getPosition(centerView)

                        if (newPosition != RecyclerView.NO_POSITION && newPosition != lastSnappedPosition) {

                            // 1. Vibramos (como ya tenías)
                            vibratePhone(50)

                            // 2. ⬇️ NUEVO: Seleccionamos el punto correspondiente ⬇️
                            binding.tabLayoutDots.getTabAt(newPosition)?.select()

                            // 3. Actualizamos la posición
                            lastSnappedPosition = newPosition
                        }
                    }
                }
            })
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    /**
     * ⬇️ NUEVA FUNCIÓN: Para que el usuario pueda pulsar los puntos ⬇️
     */
    private fun setupDotIndicatorListener() {
        binding.tabLayoutDots.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    // Verificamos que el RecyclerView está listo
                    val lm = binding.recyclerViewTasks.layoutManager as? LinearLayoutManager
                    if (lm != null) {
                        // Solo hacemos scroll si el usuario pulsó un punto DIFERENTE al actual
                        val currentPos = lm.findFirstCompletelyVisibleItemPosition()
                        if (currentPos != it.position) {
                            binding.recyclerViewTasks.smoothScrollToPosition(it.position)
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    /**
     * Función de vibración (sin cambios)
     */
    private fun vibratePhone(duration: Long) {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(duration, 255)
                it.vibrate(effect)
            } else {
                it.vibrate(duration)
            }
        }
    }



    private fun showTaskDetail(task: TaskDomain) {
        taskDetailBottomSheet?.dismiss()
        taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
        taskDetailBottomSheet?.show(childFragmentManager, TaskDetailBottomSheet.TAG)
    }

    // Nueva función para configurar el botón
    private fun showAiButton() {

            val bottomSheet = TaskIaBottomSheet()
            bottomSheet.show(childFragmentManager, "TaskIaBottomSheet")

    }

    private fun shareTask(task: TaskDomain) {
        val shareText = generateShareText(task)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
    }

    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()
        val startDateCalendar = task.start.toCalendar()
        val durationInMinutes = getDurationInMinutes(task.start, task.end)

        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.summary.uppercase()}*\n\n")

        val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(startDateCalendar.time)
        builder.append("🗓️ *Cuándo:* ${dateText.replaceFirstChar { it.titlecase(Locale.getDefault()) }}\n")
        builder.append("⏰ *Hora:* ${task.start.toHourString()}\n")

        if (durationInMinutes > 0) {
            builder.append("⏳ *Duración:* ${durationInMinutes.toDurationString()}\n")
        }
        if (!task.location.isNullOrEmpty()) {
            builder.append("📍 *Lugar:* ${task.location}\n")
        }
        if (task.typeTask.isNotEmpty()) {
            builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
        }
        builder.append("\n")

        if (!task.description.isNullOrEmpty()) {
            builder.append("🧐 *El plan:*\n")
            builder.append("${task.description}\n\n")
        }

        if (task.subTasks.isNotEmpty()) {
            builder.append("📋 *Los pasos a seguir:*\n")
            task.subTasks.forEach { subtask ->
                val subtaskStatus = if (subtask.isDone) "✔️" else "◻️"
                builder.append("   $subtaskStatus ${subtask.title}\n")
            }
            builder.append("\n")
        }
        try {
            val calendarLink = createGoogleCalendarLink(task)
            builder.append("➕ *¡Añádelo a tu calendario!:*\n")
            builder.append("$calendarLink\n\n")
        } catch (e: Exception) {}

        builder.append("--------------------------------\n")
        builder.append("¡Gestionando mi caos con *Synkrón*! 🚀")

        return builder.toString()
    }

    private fun createGoogleCalendarLink(task: TaskDomain): String {
        val startCalendar = task.start.toCalendar()
        val endCalendar = task.end.toCalendar()

        val startTimeMillis = startCalendar.timeInMillis
        val endTimeMillis = endCalendar.timeInMillis

        val isoFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startTimeUtc = isoFormatter.format(Date(startTimeMillis))
        val endTimeUtc = isoFormatter.format(Date(endTimeMillis))

        val title = URLEncoder.encode(task.summary, "UTF-8")
        val dates = URLEncoder.encode("$startTimeUtc/$endTimeUtc", "UTF-8")
        val details = URLEncoder.encode(task.description ?: "", "UTF-8")
        val location = URLEncoder.encode(task.location ?: "", "UTF-8")

        return "https://www.google.com/calendar/render?action=TEMPLATE" +
                "&text=$title" +
                "&dates=$dates" +
                "&details=$details" +
                "&location=$location"
    }

    private fun navigateToContainerActivity(task: TaskDomain?) {
        val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
            if (task != null) {
                putExtra(TASK_TO_EDIT_KEY, task)
            } else {
                putExtra(ADD_TASK, "true")
            }
        }
        startActivity(intent)
    }

    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthDp = 250
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