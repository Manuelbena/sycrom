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
import com.manuelbena.synkron.presentation.util.ADD_TASK // Asegúrate de que existen en Util o Constants
import com.manuelbena.synkron.presentation.util.TASK_TO_EDIT_KEY // Asegúrate de que existen en Util o Constants
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
    private var isFabMenuOpen = false
    private lateinit var weekManager: WeekCalendarManager
    private var displayedDate: LocalDate = LocalDate.now()

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


    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtomFloating()
        setupHeader()
        // Inicializamos el calendario AQUÍ, antes de llamar a métodos sobre él
        setupCalendar()
        setupRecyclerView()
        setupFabAnimation()
        setupDotIndicatorListener()
    }

    override fun onResume() {
        super.onResume()

        // 1. Registrar el receiver para cambios de hora/fecha (mantiene la app viva si la dejas abierta)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        requireActivity().registerReceiver(timeUpdateReceiver, filter)

        // 2. 🔥 LÓGICA DE "SIEMPRE HOY" AL ENTRAR 🔥
        // Esto asegura que si sales y vuelves, o si la app estaba en segundo plano,
        // siempre aterrices en el día actual, reseteando cualquier navegación previa.

        // Forzamos al ViewModel a seleccionar la fecha de hoy
        viewModel.refreshToToday()

        // 3. Forzamos visualmente al calendario a hacer scroll a hoy
        // (Esto es útil si estabas viendo otra semana)
        try {
            weekManager.scrollToToday()
        } catch (e: Exception) {
            // Protección por si el calendario aún no se ha inicializado
        }

        // Actualizamos la variable interna de control
        displayedDate = java.time.LocalDate.now()
        setupHeader() // Refresca el texto "Hola Manuel, hoy es..."
    }

    override fun observe() {
        var isCalendarInitialized = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (!isCalendarInitialized) {
                        // Asegúrate de que 'setupCalendar' existe en WeekCalendarManager (ver mi respuesta anterior)
                        weekManager.setupCalendar(state.selectedDate)
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
    private fun setupButtomFloating(){
        binding.fabMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_fab_gradiant)
        binding.fabMain.backgroundTintList = null
        binding.tvFabAddTask.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_fab_gradiant)
        binding.tvFabAddTask.backgroundTintList = null
        binding.tvFabAddSuggestion.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_fab_gradiant)
        binding.tvFabAddSuggestion.backgroundTintList = null
        binding.tvFabAddIng.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_fab_gradiant)
        binding.tvFabAddIng.backgroundTintList = null
        binding.tvFabAddGasto.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_fab_gradiant)
        binding.tvFabAddGasto.backgroundTintList = null
    }

    private fun checkDateChange() {
        val currentSystemDate = LocalDate.now()

        // Si la fecha del sistema es diferente a la que estamos mostrando...
        if (currentSystemDate != displayedDate) {
            displayedDate = currentSystemDate

            // 1. Actualizamos el Texto del Header ("Jueves, 5 de Diciembre")
            setupHeader()

            // 2. Movemos el calendario visualmente a Hoy
            // (Asegúrate de que tu WeekCalendarManager tenga un método para esto o reinícialo)
            try {
                weekManager.scrollToToday() // O weekManager.selectDate(currentSystemDate)
            } catch (e: Exception) {
                // Fallback por si el manager no está listo
            }

            // 3. Pedimos al ViewModel que cargue las tareas del nuevo día
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
            onDaySelected = { date ->
                viewModel.onDateSelected(date)
            },
            onMonthChanged = { title ->
                // CORRECCIÓN 1: Actualizamos el TextView del mes/año aquí
                binding.tvMonthYear.text = title
            },
            requireContext()
        )
        weekManager.generateWeekDays()
    }

    private fun updateUi(state: HomeState) {
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

            taskAdapter.submitList(state.tasks){
                binding.recyclerViewTasks.scheduleLayoutAnimation()
            }


            if (binding.tabLayoutDots.tabCount != state.tasks.size) {
                binding.tabLayoutDots.removeAllTabs()
                state.tasks.forEach { _ ->
                    val newTab = binding.tabLayoutDots.newTab()
                    newTab.setCustomView(R.layout.dot_indicator_layout)
                    binding.tabLayoutDots.addTab(newTab)
                }
            }

            if (shouldScrollToStart && hasTasks) {
                binding.recyclerViewTasks.scrollToPosition(0)
                shouldScrollToStart = false
            }
        }
        // Sincronizar visualmente la fecha
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

            btnToday.setOnClickListener {
                // Navegar a la fecha actual
                weekManager.scrollToToday()
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
        taskDetailBottomSheet?.show(childFragmentManager, TaskDetailBottomSheet.TAG)
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
        val itemWidthDp = 350
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