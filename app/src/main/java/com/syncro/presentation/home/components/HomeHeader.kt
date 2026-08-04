package com.syncro.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.syncro.presentation.components.SyncroIconButton

@Composable
fun HomeHeader(
    userName: String,
    currentDate: String,
    onSettingsClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Hola, $userName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SyncroIconButton(
                icon = Icons.Outlined.Today,
                onClick = onTodayClick,
                contentColor = MaterialTheme.colorScheme.primary
            )
            SyncroIconButton(
                icon = Icons.Outlined.DarkMode,
                onClick = onThemeToggle
            )
            SyncroIconButton(
                icon = Icons.Outlined.Settings,
                onClick = onSettingsClick
            )
        }
    }
}
