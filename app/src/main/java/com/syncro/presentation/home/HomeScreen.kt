package com.syncro.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syncro.presentation.home.components.AssistantCard
import com.syncro.presentation.home.components.EventCard
import com.syncro.presentation.home.components.HomeHeader
import com.syncro.presentation.home.components.TaskRow
import com.syncro.presentation.home.components.WeekCalendarStrip
import com.syncro.domain.model.SyncroItem
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Formatear la fecha actual para el header
    val formattedDate = uiState.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() } + ", " + 
        uiState.selectedDate.dayOfMonth + " de " + 
        uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // PARTE FIJA: Cabecera y Calendario
            HomeHeader(
                userName = uiState.userName,
                currentDate = formattedDate,
                onSettingsClick = { /* TODO */ },
                onThemeToggle = onThemeToggle,
                isDarkTheme = isDarkTheme,
                onTodayClick = { viewModel.onDaySelected(LocalDate.now()) }
            )

            WeekCalendarStrip(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.onDaySelected(it) }
            )

            // CONTENEDOR CON DEGRADADOS (Arriba y Abajo)
            Box(modifier = Modifier.fillMaxSize()) {
                // PARTE SCROLLABLE: Asistente y Timeline
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    AssistantCard(
                        quote = uiState.quote,
                        author = uiState.quoteAuthor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Timeline de items (Eventos y Tareas)
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 160.dp) // Espacio extra para el degradado y menú
                    ) {
                        uiState.timelineItems.forEach { item ->
                            when (item) {
                                is SyncroItem.Event -> EventCard(
                                    event = item,
                                    onSubtaskToggle = { subtaskTitle ->
                                        viewModel.toggleSubtaskCompletion(item.id, subtaskTitle)
                                    }
                                )
                                is SyncroItem.Task -> TaskRow(
                                    task = item,
                                    onToggle = { viewModel.toggleTaskCompletion(item.id) }
                                )
                            }
                        }
                    }
                }

                // Degradado SUPERIOR (Para que las tareas se desvanezcan al subir)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Degradado INFERIOR (Para que se vea por detrás del menú)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
        }
    }
}
