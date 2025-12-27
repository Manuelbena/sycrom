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
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.taskIA.TaskIaBottomSheet
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY
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

    // --- NUEVO: Variables para guardar la vista ("Cache") ---
    private var savedBinding: FragmentHomeBinding? = null
    private var isInitialized = false
    // --------------------------------------------------------

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

    // --- MODIFICADO: Lógica de reutilización de vista ---
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        if (savedBinding == null) {
            savedBinding = FragmentHomeBinding.inflate(inflater, container, false)
        } else {
            // Si ya existe, la desconectamos de su padre anterior para evitar crashes
            (savedBinding?.root?.parent as? ViewGroup)?.removeView(savedBinding?.root)
        }
        return savedBinding!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- MODIFICADO: Si ya está inicializado, no hacemos nada más ---
        if (isInitialized) {
            // Solo nos aseguramos de que el adapter tenga los datos actualizados
            val currentTasks = viewModel.uiState.value.tasks
            if (currentTasks.isNotEmpty()) {
                taskAdapter.submitList(currentTasks)
            }
            // Retornamos aquí para evitar re-ejecutar setupRecyclerView, setupCalendar, etc.
            // Esto es lo que evita el parpadeo: usamos lo que ya estaba en memoria.
            return
        }

        // --- Configuración Inicial (Solo se ejecuta la primera vez) ---
        setupButtomFloating()
        setupHeader()
        setupCalendar()
        setupRecyclerView()
        setupFabAnimation()
        setupDotIndicatorListener()

        // Marcamos como inicializado
        isInitialized = true
    }

    override fun onResume() {
        super.onResume()

        // Como usamos la vista cacheada, los logs deberían mostrar que NO recarga innecesariamente
        android.util.Log.d("DEBUG_BUG", "onResume Disparado")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        requireActivity().registerReceiver(timeUpdateReceiver, filter)

        val isViewModelDateToday = viewModel.uiState.value.selectedDate == LocalDate.now()
        val isDisplayDateToday = displayedDate == LocalDate.now()
        val hasData = viewModel.uiState.value.tasks.isNotEmpty()

        if (!isViewModelDateToday || !hasData) {
            android.util.Log.d("DEBUG_BUG", "onResume -> Llamando a refreshToToday()")
            viewModel.refreshToToday()
        } else {
            android.util.Log.d("DEBUG_BUG", "onResume -> NO se llama a refreshToToday()")
        }

        if (!isDisplayDateToday) {
            try {
                weekManager.scrollToToday()
            } catch (e: Exception) {}
            displayedDate = LocalDate.now()
            setupHeader()
        }
    }

    override fun observe() {
        var isCalendarInitialized = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    android.util.Log.d("DEBUG_BUG", "Estado Recibido -> isLoading: ${state.isLoading}, Tasks: ${state.tasks.size}, Fecha: ${state.selectedDate}")

                    if (!isCalendarInitialized && !isInitialized) {
                        // Solo configuramos el calendario si no tenemos la vista cacheada lista
                        weekManager.setupCalendar(state.selectedDate)
                        isCalendarInitialized = true
                    } else if (isInitialized && !isCalendarInitialized) {
                        // Si reutilizamos vista, solo marcamos el flag
                        isCalendarInitialized = true
                    }

                    updateUi(state)
                }
            }
        }

        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                is HomeAction.ShowErrorSnackbar -> Snackbar.make(binding.root, action.message, Snackbar.LENGTH_SHORT).show()
                is HomeAction.NavigateToEditTask -> navigateToContainerActivity(action.task)
                is HomeAction.ShareTask -> shareTask(action.task)
            }
        }
    }

    // ... (Resto de métodos auxiliares igual que antes) ...

    private fun setupButtomFloating(){
        binding.fabMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        binding.fabMain.backgroundTintList = null
        binding.tvFabAddTask.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        binding.tvFabAddTask.backgroundTintList = null
        binding.tvFabAddSuggestion.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        binding.tvFabAddSuggestion.backgroundTintList = null
        binding.tvFabAddIng.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        binding.tvFabAddIng.backgroundTintList = null
        binding.tvFabAddGasto.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_add_buttom)
        binding.tvFabAddGasto.backgroundTintList = null
    }

    private fun checkDateChange() {
        val currentSystemDate = LocalDate.now()
        if (currentSystemDate != displayedDate) {
            displayedDate = currentSystemDate
            setupHeader()
            try {
                weekManager.scrollToToday()
            } catch (e: Exception) {}
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
        binding.progressIndicator.isVisible = state.isLoading
        val hasTasks = state.tasks.isNotEmpty()

        if (state.isLoading && !hasTasks) {
            binding.recyclerViewTasks.isVisible = false
            binding.ivNoTasks.isVisible = false
            binding.tvNoTasks.isVisible = false
            binding.tabLayoutDots.isVisible = false
        } else {
            binding.recyclerViewTasks.isVisible = hasTasks
            binding.tabLayoutDots.isVisible = hasTasks
            binding.ivNoTasks.isVisible = !hasTasks && !state.isLoading
            binding.tvNoTasks.isVisible = !hasTasks && !state.isLoading

            taskAdapter.submitList(state.tasks)

            if (binding.tabLayoutDots.tabCount != state.tasks.size) {
                binding.tabLayoutDots.removeAllTabs()
                state.tasks.forEach { _ ->
                    val newTab = binding.tabLayoutDots.newTab()
                    newTab.setCustomView(R.layout.dot_indicator_layout)
                    binding.tabLayoutDots.addTab(newTab)
                }
                val layoutManager = binding.recyclerViewTasks.layoutManager as? LinearLayoutManager
                val currentPos = layoutManager?.findFirstCompletelyVisibleItemPosition()
                if (currentPos != null && currentPos != RecyclerView.NO_POSITION && currentPos < binding.tabLayoutDots.tabCount) {
                    binding.tabLayoutDots.getTabAt(currentPos)?.select()
                }
            }

            if (shouldScrollToStart && hasTasks) {
                binding.recyclerViewTasks.scrollToPosition(0)
                shouldScrollToStart = false
            }
        }
        weekManager.selectDate(state.selectedDate)
    }

    override fun setListener() {
        binding.apply {
            fabMain.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (isFabMenuOpen) closeFabMenu() else openFabMenu()
            }
            tvFabAddTask.setOnClickListener {
                closeFabMenu()
                navigateToContainerActivity(null)
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

    private fun setupFabAnimation() {
        binding.fabMain.extend()
        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY) binding.fabMain.shrink()
                else if (scrollY < oldScrollY) binding.fabMain.extend()
            }
        )
    }

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
        // NO HACEMOS savedBinding = null AQUÍ PARA MANTENER LA VISTA VIVA
        taskDetailBottomSheet = null
        super.onDestroyView()
    }
}