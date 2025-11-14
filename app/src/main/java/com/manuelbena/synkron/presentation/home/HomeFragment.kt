package com.manuelbena.synkron.presentation.home


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.os.Bundle
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.Callback.getDefaultUIUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentHomeBinding
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.activitys.ContainerActivity
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import com.manuelbena.synkron.presentation.util.ADD_TASK
import com.manuelbena.synkron.presentation.util.CarouselScrollListener
import com.manuelbena.synkron.presentation.util.EDIT_TASK

import com.manuelbena.synkron.presentation.util.WeekCalendarManager
import com.manuelbena.synkron.presentation.util.getDurationInMinutes
import com.manuelbena.synkron.presentation.util.toCalendar
import com.manuelbena.synkron.presentation.util.toDurationString
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
    private val fabInterpolator = OvershootInterpolator()
    private lateinit var weekCalendarManager: WeekCalendarManager
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
        setupRecyclerView()
        setupWeekCalendar()
        setupFabAnimation()
    }

    override fun onResume() {
        super.onResume()
        shouldScrollToStart = true

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
        var isCalendarInitialized = false // El flag que te di antes

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
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
                is HomeViewModel.HomeAction.ShowErrorSnackbar -> {
                    Snackbar.make(binding.root, action.message, Snackbar.LENGTH_SHORT).show()
                }
                is HomeViewModel.HomeAction.NavigateToEditTask -> {
                    navigateToContainerActivity(action.task)
                }
                is HomeViewModel.HomeAction.ShareTask -> {
                    shareTask(action.task)
                }
            }
        }
    }

    /**
     * Función centralizada para actualizar la UI basada en el HomeState.
     */
    private fun updateUi(state: HomeState) {
        // binding.progressBar.isVisible = state.isLoading // (Si tuvieras una)

        binding.textDate.text = state.headerText
        binding.tvDateTitle.text = state.selectedDate.format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM", Locale("es", "ES"))
        )

        val hasTasks = state.tasks.isNotEmpty()
        binding.ivNoTasks.isVisible = !hasTasks
        binding.tvNoTasks.isVisible = !hasTasks
        binding.recyclerViewTasks.isVisible = hasTasks



        taskAdapter.submitList(state.tasks)

        weekCalendarManager.selectDate(state.selectedDate)

        if (hasTasks && shouldScrollToStart) {
            binding.recyclerViewTasks.post {
                binding.recyclerViewTasks.smoothScrollToPosition(0)
            }
            shouldScrollToStart = false
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
                navigateToContainerActivity(null) // null = Tarea Nueva
            }

            tvFabAddSuggestion.setOnClickListener {
                closeFabMenu()
                // Lógica futura
            }
            tvFabAddGasto.setOnClickListener {
                closeFabMenu()
                // Lógica futura
            }
            tvFabAddIng.setOnClickListener {
                closeFabMenu()
                // Lógica futura
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
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = taskAdapter
            applyCarouselPadding()
            addOnScrollListener(CarouselScrollListener())
            itemAnimator = null
        }
        snapHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupWeekCalendar() {
        weekCalendarManager = WeekCalendarManager(binding.weekDaysContainer) { selectedDate: LocalDate ->
            shouldScrollToStart = true
            viewModel.onDateSelected(selectedDate)
        }

    }

    private fun showTaskDetail(task: TaskDomain) {
        taskDetailBottomSheet?.dismiss()
        taskDetailBottomSheet = TaskDetailBottomSheet.newInstance(task)
        taskDetailBottomSheet?.show(childFragmentManager, TaskDetailBottomSheet.TAG)
    }

    private fun shareTask(task: TaskDomain) {
        val shareText = generateShareText(task)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND // CAMBIO: 'action' en minúscula
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain" // CAMBIO: 'type' en minúscula
        }
        // sendIntent.setPackage("com.whatsapp") // (Opcional)

        val shareIntent = Intent.createChooser(sendIntent, "Compartir tarea")
        startActivity(shareIntent)
    }

    private fun generateShareText(task: TaskDomain): String {
        val builder = StringBuilder()
        val startDateCalendar = task.start.toCalendar()
        val durationInMinutes = getDurationInMinutes(task.start, task.end)

        // --- Encabezado y Título ---
        val statusEmoji = if (task.isDone) "✅" else "🎯"
        builder.append("$statusEmoji *¡Ojo a esta tarea!* $statusEmoji\n\n")
        builder.append("*${task.summary.uppercase()}*\n\n") // CAMBIO

        // --- Contexto (Fecha, Hora, Lugar...) ---
        val dateText = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES")).format(startDateCalendar.time)
        builder.append("🗓️ *Cuándo:* ${dateText.replaceFirstChar { it.titlecase(Locale.getDefault()) }}\n")
        builder.append("⏰ *Hora:* ${task.start.toHourString()}\n") // CAMBIO

        if (durationInMinutes > 0) { // CAMBIO
            builder.append("⏳ *Duración:* ${durationInMinutes.toDurationString()}\n")
        }
        if (!task.location.isNullOrEmpty()) { // CAMBIO
            builder.append("📍 *Lugar:* ${task.location}\n")
        }
        if (task.typeTask.isNotEmpty()) {
            builder.append("🏷️ *Categoría:* ${task.typeTask}\n")
        }
        builder.append("\n") // Separador

        // --- Descripción (si existe) ---
        if (!task.description.isNullOrEmpty()) { // CAMBIO
            builder.append("🧐 *El plan:*\n")
            builder.append("${task.description}\n\n")
        }

        // --- Subtareas (si existen) ---
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

        // --- Footer de Synkrón ---
        builder.append("--------------------------------\n")
        builder.append("¡Gestionando mi caos con *Synkrón*! 🚀")

        return builder.toString()
    }

    private fun createGoogleCalendarLink(task: TaskDomain): String {
        // 1. Calcular las fechas de inicio y fin (CAMBIO)
        val startCalendar = task.start.toCalendar()
        val endCalendar = task.end.toCalendar()

        val startTimeMillis = startCalendar.timeInMillis
        val endTimeMillis = endCalendar.timeInMillis

        // 2. Formatear las fechas a ISO 8601 en UTC
        val isoFormatter = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startTimeUtc = isoFormatter.format(Date(startTimeMillis))
        val endTimeUtc = isoFormatter.format(Date(endTimeMillis))

        // 3. Codificar los parámetros (CAMBIO)
        val title = URLEncoder.encode(task.summary, "UTF-8")
        val dates = URLEncoder.encode("$startTimeUtc/$endTimeUtc", "UTF-8")
        val details = URLEncoder.encode(task.description ?: "", "UTF-8")
        val location = URLEncoder.encode(task.location ?: "", "UTF-8")

        // 4. Construir la URL final
        return "https://www.google.com/calendar/render?action=TEMPLATE" +
                "&text=$title" +
                "&dates=$dates" +
                "&details=$details" +
                "&location=$location"
    }

    private fun navigateToContainerActivity(task: TaskDomain?) {
        val intent = Intent(requireContext(), ContainerActivity::class.java).apply {
            if (task != null) {
                putExtra(EDIT_TASK, task)
            } else {
                putExtra(ADD_TASK, "true")
            }
        }
        startActivity(intent)
    }

    private fun RecyclerView.applyCarouselPadding() {
        val itemWidthDp = 300 // (Usando el ancho de 300dp)
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