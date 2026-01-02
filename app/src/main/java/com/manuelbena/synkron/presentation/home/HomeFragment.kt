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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.task.TaskBottomSheet
import com.manuelbena.synkron.presentation.taskIA.TaskIaBottomSheet
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import com.manuelbena.synkron.presentation.util.extensions.toDurationString
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toCalendar
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by activityViewModels()

    private var savedBinding: FragmentHomeBinding? = null
    private var isInitialized = false

    private var isFabMenuOpen = false
    private lateinit var weekManager: WeekCalendarManager
    private var displayedDate: LocalDate = LocalDate.now()

    private val fabInterpolator = OvershootInterpolator()

    private var lastSnappedPosition = RecyclerView.NO_POSITION
    private var shouldScrollToStart: Boolean = false
    private var taskDetailBottomSheet: TaskDetailBottomSheet? = null

    companion object {
        private const val TAG = "HomeFragment"
    }

    private val taskAdapter = TaskAdapter(
        onItemClick = { task -> showTaskDetail(task) },
        onMenuAction = { action -> viewModel.onTaskMenuAction(action) },
        onTaskCheckedChange = { task, isDone -> viewModel.onTaskCheckedChanged(task, isDone) },
        onSubTaskChange = { taskId, subTask -> viewModel.onSubTaskChanged(taskId, subTask) }
    ).apply{
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        if (savedBinding == null) {
            savedBinding = FragmentHomeBinding.inflate(inflater, container, false)
        } else {
            (savedBinding?.root?.parent as? ViewGroup)?.removeView(savedBinding?.root)
        }
        return savedBinding!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isInitialized) {
            val currentTasks = viewModel.uiState.value.tasks
            if (currentTasks.isNotEmpty()) {
                taskAdapter.submitList(currentTasks)
            }
            return
        }
        setupSwipeRefresh()
        setupButtomFloating()
        setupHeader()
        setupCalendar()
        setupRecyclerView()
        setupFabAnimation()
        setupDotIndicatorListener()

        isInitialized = true
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        requireActivity().registerReceiver(timeUpdateReceiver, filter)

        val isViewModelDateToday = viewModel.uiState.value.selectedDate == LocalDate.now()
        val hasData = viewModel.uiState.value.tasks.isNotEmpty()

        if (!isViewModelDateToday || !hasData) {
            viewModel.refreshToToday()
        }

        if (displayedDate != LocalDate.now()) {
            try { weekManager.scrollToToday() } catch (e: Exception) {}
            displayedDate = LocalDate.now()
            setupHeader()
        }
    }

    override fun observe() {
        var isCalendarInitialized = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!isCalendarInitialized && !isInitialized) {
                        weekManager.setupCalendar(state.selectedDate)
                        isCalendarInitialized = true
                    } else if (isInitialized && !isCalendarInitialized) {
                        isCalendarInitialized = true
                    }
                    updateUi(state)
                }
            }
        }

        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is HomeAction.ShowErrorSnackbar -> Snackbar.make(binding.root, action.message, Snackbar.LENGTH_SHORT).show()
                is HomeAction.NavigateToEditTask -> showTaskBottomSheet(action.task)
                is HomeAction.ShareTask -> shareTask(action.task)
            }
        }
    }

    private fun setupButtomFloating(){
        // Configuración visual de FABs (manteniendo tu código)
        val bgDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        listOf(binding.fabMain, binding.tvFabAddTask, binding.tvFabAddSuggestion,
            binding.tvFabAddIng, binding.tvFabAddGasto).forEach {
            it.background = bgDrawable
            it.backgroundTintList = null
        }

        binding.tvFabAddTask.setOnClickListener {
            closeFabMenu()
            showTaskBottomSheet(null)
        }
    }

    private fun showTaskBottomSheet(task: TaskDomain?) {
        val bottomSheet = TaskBottomSheet.newInstance(task)
        bottomSheet.show(childFragmentManager, "TaskBottomSheet")
    }

    private fun checkDateChange() {
        val currentSystemDate = LocalDate.now()
        if (currentSystemDate != displayedDate) {
            displayedDate = currentSystemDate
            setupHeader()
            try { weekManager.scrollToToday() } catch (e: Exception) {}
            viewModel.refreshToToday()
        }
    }

    private fun setupHeader() {
        val date = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
        binding.textDate.text = date.replaceFirstChar { it.uppercase() }
        binding.textGreeting.text = "Hola, Manuel"
        binding.textTemperature.text = "19°C"
    }

    private fun setupCalendar() {
        weekManager = WeekCalendarManager(
            calendarView = binding.weekCalendarView,
            onDaySelected = { date -> viewModel.onDateSelected(date) },
            onMonthChanged = { title -> binding.tvMonthYear.text = title },
            requireContext()
        )
        weekManager.generateWeekDays()
    }

    private fun updateUi(state: HomeState) {
        binding.apply {
            // Variables de estado
            val hasTasks = state.tasks.isNotEmpty()
            // ¿Es la primera carga? (Está cargando Y aún no hay tareas en la lista)
            val isFirstLoad = state.isLoading && !hasTasks

            // 1. CONTROL DEL SPINNER (SwipeRefresh)
            // Solo mostramos el spinner girando si NO es la primera carga (es decir, si es un refresco manual).
            // Si es primera carga, el protagonista es el Skeleton.
            swipeRefresh.isRefreshing = state.isLoading && hasTasks

            // Ocultamos indicadores antiguos para siempre
            progressIndicator.isVisible = false

            // 2. GESTIÓN DE VISIBILIDAD (Skeleton vs Contenido)
            if (isFirstLoad) {
                // --- MODO: PRIMERA CARGA (SKELETON) ---
                if (!shimmerViewContainer.isShimmerStarted) {
                    shimmerViewContainer.startShimmer()
                }
                shimmerViewContainer.isVisible = true

                // Ocultamos todo lo demás para dejar solo los "huesos" grises
                recyclerViewTasks.isVisible = false
                lyProgress.isVisible = false // Asegúrate que este ID coincide con tu XML (el contenedor del progreso)
                ivNoTasks.isVisible = false
                tvNoTasks.isVisible = false
                tabLayoutDots.isVisible = false

            } else {
                // --- MODO: DATOS LISTOS O REFRESCO MANUAL ---
                if (shimmerViewContainer.isShimmerStarted) {
                    shimmerViewContainer.stopShimmer()
                }
                shimmerViewContainer.isVisible = false

                if (hasTasks) {
                    // A) HAY TAREAS (Mostramos todo)
                    recyclerViewTasks.isVisible = true
                    lyProgress.isVisible = true
                    tabLayoutDots.isVisible = true

                    // Ocultamos el Empty State
                    ivNoTasks.isVisible = false
                    tvNoTasks.isVisible = false

                    // Actualizamos la lista y la tarjeta
                    taskAdapter.submitList(state.tasks)
                    updateProgressCard(state.tasks)

                    // Lógica de los puntitos (Dots Indicator)
                    if (tabLayoutDots.tabCount != state.tasks.size) {
                        tabLayoutDots.removeAllTabs()
                        state.tasks.forEach { _ ->
                            val newTab = tabLayoutDots.newTab()
                            newTab.setCustomView(R.layout.dot_indicator_layout)
                            tabLayoutDots.addTab(newTab)
                        }
                        // Mantener el punto seleccionado sincronizado con el scroll
                        val layoutManager = recyclerViewTasks.layoutManager as? LinearLayoutManager
                        val currentPos = layoutManager?.findFirstCompletelyVisibleItemPosition()
                        if (currentPos != null && currentPos != RecyclerView.NO_POSITION && currentPos < tabLayoutDots.tabCount) {
                            tabLayoutDots.getTabAt(currentPos)?.select()
                        }
                    }
                } else {
                    // B) LISTA VACÍA (Terminó de cargar y no hay nada)
                    // Gracias al animateLayoutChanges="true" en el XML, esto aparecerá suave (fade-in)
                    recyclerViewTasks.isVisible = false
                    lyProgress.isVisible = false
                    tabLayoutDots.isVisible = false

                    ivNoTasks.isVisible = true
                    tvNoTasks.isVisible = true
                }
            }
        }
        // Actualizamos el calendario de la semana
        weekManager.selectDate(state.selectedDate)
    }

    // --- NUEVO: Lógica de la Tarjeta de Progreso ---
    private fun updateProgressCard(tasks: List<TaskDomain>) {
        binding.apply {
            val totalTasks = tasks.size
            val completedTasks = tasks.count { it.isDone }

            // Cálculo seguro del porcentaje
            val percentage = if (totalTasks > 0) {
                ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt()
            } else {
                0
            }

            // Actualizar textos
            // Si el XML espera %d (enteros), asegúrate de que sean Int:
            tvSubtitle.text = getString(R.string.home_progress_subtitle_format, completedTasks, totalTasks)
            tvPercentage.text = "$percentage%"

            // Actualizar Barra (Nota: Si usas ProgressBar estándar para el degradado, usa 'progress = percentage')
            // linearProgressIndicator.progress = percentage // <- Descomenta esto si usas ProgressBar con degradado
            linearProgressIndicator.setProgressCompat(percentage, true) // <- Esto es para Material LinearProgressIndicator

            // Cambio de color al completar (Opcional)
            if (percentage == 100) {
                tvProgressLabel.text = "¡Todo listo!"
                ivProgressIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.cat_personal)
            } else {
                tvProgressLabel.text = getString(R.string.home_tasks_progress_label)
                ivProgressIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.purple_600)
            }
        }
    }

    override fun setListener() {
        binding.apply {
            fabMain.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (isFabMenuOpen) closeFabMenu() else openFabMenu()
            }
            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                showTaskBottomSheet(null)
            }
            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
                showAiButton()
            }
            tvFabAddGasto.setOnClickListener { closeFabMenu() }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
                showAiButton()
            }
            btnToday.setOnClickListener { weekManager.scrollToToday() }
        }
    }

    // --- AÑADE ESTA FUNCIÓN AL FINAL DEL FRAGMENTO ---
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            // COLOR DE FONDO (El círculo): Usamos un tono "Surface Container" (gris muy suave/azulado)
            // Esto le da profundidad M3 en lugar del blanco plano antiguo.
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceContainerHigh
            )
            setProgressBackgroundColorSchemeColor(surfaceColor)

            // COLORES DEL INDICADOR (La flecha giratoria):
            // Alternamos entre Primary (tu color principal) y Tertiary (tu color de acento)
            // para darle ese toque "juguetón" de Google.
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary
            )
            val tertiaryColor = com.google.android.material.color.MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorTertiary
            )
            setColorSchemeColors(primaryColor, tertiaryColor)

            // Ajuste de posición: Que baje un poco más para que se vea bien
            setProgressViewOffset(true, 0, 150)

            setOnRefreshListener {
                viewModel.onRefreshRequested()
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

    // ... (Métodos openFabMenu, closeFabMenu, showFab, hideFab sin cambios) ...
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
        fab.animate().alpha(0f).translationY(50f).setInterpolator(fabInterpolator).setDuration(300)
            .withEndAction { fab.visibility = View.GONE }.start()
    }

    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            val lm = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            layoutManager = lm
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val centerView = snapHelper.findSnapView(lm) ?: return
                        val newPosition = lm.getPosition(centerView)
                        if (newPosition != RecyclerView.NO_POSITION && newPosition != lastSnappedPosition) {
                            vibratePhone(50)
                            binding.tabLayoutDots.getTabAt(newPosition)?.select()
                            lastSnappedPosition = newPosition
                        }
                    }
                }
            })
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupDotIndicatorListener() {
        binding.tabLayoutDots.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val lm = binding.recyclerViewTasks.layoutManager as? LinearLayoutManager
                    if (lm != null) {
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

    // ... (Métodos showTaskDetail, showAiButton, shareTask, generateShareText, createGoogleCalendarLink, timeUpdateReceiver, applyCarouselPadding, onDestroyView sin cambios) ...
    private fun showTaskDetail(task: TaskDomain) {
        taskDetailBottomSheet?.dismiss()
        taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
        taskDetailBottomSheet?.show(childFragmentManager, TAG)
    }

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
        if (durationInMinutes > 0) builder.append("⏳ *Duración:* ${durationInMinutes.toDurationString()}\n")
        if (!task.location.isNullOrEmpty()) builder.append("📍 *Lugar:* ${task.location}\n")
        if (task.typeTask.isNotEmpty()) builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
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
                "&text=$title" + "&dates=$dates" + "&details=$details" + "&location=$location"
    }

    private val timeUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_TIME_TICK || action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIME_CHANGED) {
                checkDateChange()
            }
        }
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