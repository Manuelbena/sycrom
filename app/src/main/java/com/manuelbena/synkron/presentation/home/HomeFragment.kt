package com.manuelbena.synkron.presentation.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
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
import com.manuelbena.synkron.presentation.adapter.SuperTaskAdapter
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.models.Quote
import com.manuelbena.synkron.presentation.superTask.SuperTaskBottomSheet
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

        // Si ya está inicializado, solo restauramos datos para evitar flicker
        if (isInitialized) {
            val currentTasks = viewModel.uiState.value.tasks
            if (currentTasks.isNotEmpty()) {
                taskAdapter.submitList(currentTasks)
            }
            // Restauramos SuperTareas si ya existen
            val currentSuperTasks = viewModel.uiState.value.superTasks
            if (currentSuperTasks.isNotEmpty()){
                (binding.rvSuperTasks.adapter as? SuperTaskAdapter)?.submitList(currentSuperTasks)
            }
            return
        }

        setupSuperTasks()
        setupSwipeRefresh()
        setupButtomFloating()
        setupHeader()
        setupCalendar()
        setupRecyclerView()
        setupQuoteOfTheDay()
        setupDotIndicatorListener()

        isInitialized = true
    }

    override fun onResume() {
        super.onResume()
        updateDayTimeline()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        context?.registerReceiver(timeUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        try {
            context?.unregisterReceiver(timeUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignorar
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

    private fun setupSuperTasks() {
        // Inicializamos el adaptador con el click listener
        val superTaskAdapter = SuperTaskAdapter { task ->
            // Al hacer click, abrimos el BottomSheet
            val sheet = SuperTaskBottomSheet.newInstance(task)
            sheet.onSaveClickListener = { updatedTask ->
                // AQUÍ: Guardamos en base de datos a través del ViewModel
                viewModel.updateSuperTask(updatedTask)
            }
            sheet.show(childFragmentManager, "SuperTaskSheet")
        }

        val snapHelper = PagerSnapHelper()

        binding.rvSuperTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = superTaskAdapter
            applyCarouselPadding(350)
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null
        }

        snapHelper.attachToRecyclerView(binding.rvSuperTasks)

        // NOTA: He eliminado el mockData. Ahora los datos vendrán del viewModel.uiState
    }

    private fun updateUi(state: HomeState) {
        binding.apply {
            val hasTasks = state.tasks.isNotEmpty()
            val isFirstLoad = state.isLoading && !hasTasks

            swipeRefresh.isRefreshing = state.isLoading && hasTasks
            progressIndicator.isVisible = false

            if (isFirstLoad) {
                // Skeleton loading
                if (!shimmerViewContainer.isShimmerStarted) shimmerViewContainer.startShimmer()
                shimmerViewContainer.isVisible = true

                recyclerViewTasks.isVisible = false
                ivNoTasks.isVisible = false
                tvNoTasks.isVisible = false
                tabLayoutDots.isVisible = false
                // Ocultar supertareas mientras carga inicial
                rvSuperTasks.isVisible = false

            } else {
                // Datos listos
                if (shimmerViewContainer.isShimmerStarted) shimmerViewContainer.stopShimmer()
                shimmerViewContainer.isVisible = false

                // 1. Tareas Normales
                if (hasTasks) {
                    recyclerViewTasks.isVisible = true
                    tabLayoutDots.isVisible = true
                    ivNoTasks.isVisible = false
                    tvNoTasks.isVisible = false

                    taskAdapter.submitList(state.tasks)
                    updateProgressCard(state.tasks)
                    updateDots(state.tasks.size)
                } else {
                    recyclerViewTasks.isVisible = false
                    tabLayoutDots.isVisible = false
                    ivNoTasks.isVisible = true
                    tvNoTasks.isVisible = true
                }

                // 2. Super Tareas (NUEVO)
                // Enviamos la lista real del estado al adaptador
                val superAdapter = rvSuperTasks.adapter as? SuperTaskAdapter
                superAdapter?.submitList(state.superTasks)

                // Mostramos el RV solo si hay super tareas para este día
                rvSuperTasks.isVisible = state.superTasks.isNotEmpty()
                // Opcional: Ocultar el título "Super Tareas" si la lista está vacía
                // binding.tvSuperTasksTitle.isVisible = state.superTasks.isNotEmpty()
            }
        }
        weekManager.selectDate(state.selectedDate)
    }

    private fun updateDots(count: Int) {
        if (binding.tabLayoutDots.tabCount != count) {
            binding.tabLayoutDots.removeAllTabs()
            repeat(count) {
                val newTab = binding.tabLayoutDots.newTab()
                newTab.setCustomView(R.layout.dot_indicator_layout)
                binding.tabLayoutDots.addTab(newTab)
            }
            val lm = binding.recyclerViewTasks.layoutManager as? LinearLayoutManager
            val currentPos = lm?.findFirstCompletelyVisibleItemPosition()
            if (currentPos != null && currentPos != RecyclerView.NO_POSITION && currentPos < binding.tabLayoutDots.tabCount) {
                binding.tabLayoutDots.getTabAt(currentPos)?.select()
            }
        }
    }

    // --- RESTO DE MÉTODOS EXISTENTES (Setup UI, Listeners, Utils, etc.) ---
    // (Se mantienen igual que tu código original, solo he limpiado para brevedad en la respuesta)

    private fun setupButtomFloating() {
        val options = listOf(binding.tvFabAddTask, binding.tvFabAddSuggestion, binding.tvFabAddIng, binding.tvFabAddGasto)
        options.forEach { it.visibility = View.GONE; it.alpha = 0f; it.translationY = 50f }

        binding.fabMain.setOnClickListener { if (isFabMenuOpen) closeFabMenu() else openFabMenu() }
        binding.tvFabAddTask.setOnClickListener { closeFabMenu(); showTaskBottomSheet(null) }
        binding.tvFabAddSuggestion.setOnClickListener { closeFabMenu(); showAiButton() }
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
            setupQuoteOfTheDay()
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

        }
    }

    override fun setListener() {
        binding.apply {
            fabMain.setOnClickListener { if (isFabMenuOpen) closeFabMenu() else openFabMenu() }
            tvFabAddTask.setOnClickListener { closeFabMenu(); showTaskBottomSheet(null) }
            tvFabAddSuggestion.setOnClickListener { closeFabMenu(); showAiButton() }
            tvFabAddGasto.setOnClickListener { closeFabMenu() }
            tvFabAddIng.setOnClickListener { closeFabMenu(); showAiButton() }
            btnToday.setOnClickListener { weekManager.scrollToToday() }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            val surfaceColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh)
            setProgressBackgroundColorSchemeColor(surfaceColor)
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
            val tertiaryColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary)
            setColorSchemeColors(primaryColor, tertiaryColor)
            setProgressViewOffset(true, 0, 150)
            setOnRefreshListener { viewModel.onRefreshRequested() }
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true
        binding.fabMain.animate().rotation(45f).setInterpolator(fabInterpolator).setDuration(300).start()
        showFab(binding.tvFabAddTask); showFab(binding.tvFabAddSuggestion); showFab(binding.tvFabAddGasto); showFab(binding.tvFabAddIng)
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false
        binding.fabMain.animate().rotation(0f).setInterpolator(fabInterpolator).setDuration(300).start()
        hideFab(binding.tvFabAddTask); hideFab(binding.tvFabAddSuggestion); hideFab(binding.tvFabAddGasto); hideFab(binding.tvFabAddIng)
    }

    private fun showFab(fab: View) {
        fab.visibility = View.VISIBLE; fab.alpha = 0f; fab.translationY = 50f
        fab.animate().alpha(1f).translationY(0f).setInterpolator(fabInterpolator).setDuration(300).start()
    }

    private fun hideFab(fab: View) {
        fab.animate().alpha(0f).translationY(50f).setInterpolator(fabInterpolator).setDuration(300).withEndAction { fab.visibility = View.GONE }.start()
    }

    private fun setupRecyclerView() {
        val snapHelper = PagerSnapHelper()
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding(350)
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val centerView = snapHelper.findSnapView(layoutManager) ?: return
                        val newPosition = (layoutManager as LinearLayoutManager).getPosition(centerView)
                        if (newPosition != RecyclerView.NO_POSITION && newPosition != RecyclerView.NO_POSITION && binding.tabLayoutDots.tabCount > newPosition) {
                            binding.tabLayoutDots.getTabAt(newPosition)?.select()
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

    private fun updateDayTimeline() {
        val now = java.time.LocalTime.now()
        val totalMinutesInDay = 24 * 60
        val currentMinutes = now.hour * 60 + now.minute
        val percentage = currentMinutes.toFloat() / totalMinutesInDay
        val params = binding.ivTimeCursor.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.horizontalBias = percentage.coerceIn(0f, 1f)
        binding.ivTimeCursor.layoutParams = params
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        binding.tvCurrentTimeBubble.text = now.format(formatter)
        binding.clDayTimeline.isVisible = viewModel.uiState.value.tasks.isNotEmpty()
    }

    private val timeUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (view == null || !isAdded || context == null) return
            val action = intent?.action
            if (action == Intent.ACTION_TIME_TICK || action == Intent.ACTION_DATE_CHANGED || action == Intent.ACTION_TIME_CHANGED) {
                try {
                    checkDateChange()
                    updateDayTimeline()
                    binding.recyclerViewTasks.adapter?.notifyItemRangeChanged(0, binding.recyclerViewTasks.adapter?.itemCount ?: 0, "PAYLOAD_TIME_UPDATE")
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupQuoteOfTheDay() {
        try {
            val jsonString = requireContext().assets.open("quotes.json").bufferedReader().use { it.readText() }
            val quoteList = com.google.gson.Gson().fromJson(jsonString, Array<Quote>::class.java)
            if (!quoteList.isNullOrEmpty()) {
                val calendar = java.util.Calendar.getInstance()
                val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                val index = dayOfYear % quoteList.size
                val todayQuote = quoteList[index]
                binding.tvQuoteText.text = "\"${todayQuote.texto}\""
                binding.tvQuoteAuthor.text = "— ${todayQuote.autor}"
                binding.cardQuote.alpha = 0f
                binding.cardQuote.animate().alpha(1f).setDuration(500).start()
            } else {
                binding.cardQuote.isVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.cardQuote.isVisible = false
        }
    }

    private fun RecyclerView.applyCarouselPadding(itemWidthDp: Int) {
        val itemWidthPx = resources.displayMetrics.density * itemWidthDp
        val screenWidthPx = resources.displayMetrics.widthPixels
        val padding = ((screenWidthPx - itemWidthPx) / 2f).toInt().coerceAtLeast(0)
        setPadding(padding, 0, padding, 0)
        clipToPadding = false
    }

    override fun onDestroyView() {
        taskDetailBottomSheet = null
        super.onDestroyView()
    }
}