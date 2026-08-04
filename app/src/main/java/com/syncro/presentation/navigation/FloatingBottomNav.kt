package com.syncro.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FloatingBottomNav(
    items: List<AppScreen>,
    currentRoute: String?,
    onItemClick: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 40.dp, vertical = 42.dp)
            .fillMaxWidth(),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavBarItem(
                    screen = screen,
                    isSelected = isSelected,
                    onClick = { onItemClick(screen) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    screen: AppScreen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Colores del nuevo diseño expresivo
    val colorAzulSeleccionado = Color(0xFF1976D2)
    val colorGrisInactivo = Color(0xFF424242)
    val colorFondoPildora = Color(0xFFE3F2FD)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) colorFondoPildora else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "BgAnimation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colorAzulSeleccionado else colorGrisInactivo,
        animationSpec = tween(durationMillis = 300),
        label = "ContentAnimation"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Contenedor del Icono con la forma de píldora ancha del nuevo diseño
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = contentColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Texto inferior
        Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}