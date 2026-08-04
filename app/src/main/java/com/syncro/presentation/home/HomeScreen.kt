package com.syncro.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syncro.presentation.home.components.AssistantCard
import com.syncro.presentation.home.components.HomeHeader
import com.syncro.presentation.home.components.WeekCalendarStrip
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(
                userName = uiState.userName,
                currentDate = formattedDate,
                onSettingsClick = { /* TODO */ },
                onThemeToggle = { /* TODO */ },
                onTodayClick = { viewModel.onDaySelected(LocalDate.now()) }
            )

            WeekCalendarStrip(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.onDaySelected(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AssistantCard(
                quote = uiState.quote,
                author = uiState.quoteAuthor
            )
            
            // Aquí irán las otras dos partes de la Home (Tareas, etc.)
        }
    }
}
