package com.syncro.presentation.navigation



import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.syncro.presentation.assistant.AssistantScreen
import com.syncro.presentation.calendar.CalendarScreen
import com.syncro.presentation.home.HomeScreen

@Composable
fun MainScaffold() {
    val navController = rememberNavController()

    // Observamos la ruta actual para saber qué botón iluminar en el menú
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            FloatingBottomNav(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemClick = { screen ->
                    navController.navigate(screen.route) {
                        // Limpiamos la pila para no crear un historial infinito al cambiar de pestañas
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        // Aquí conectamos las pantallas reales
        NavHost(
            navController = navController,
            startDestination = AppScreen.Home.route,
            modifier = Modifier.padding(paddingValues) // Respeta el espacio del menú
        ) {
            composable(AppScreen.Home.route) {
                 HomeScreen()
            }
            composable(AppScreen.Calendar.route) {
                 CalendarScreen()
            }
            composable(AppScreen.Savings.route) {
                 //SavingsScreen()
            }
            composable(AppScreen.Assistant.route) {
                 AssistantScreen()
            }
        }
    }
}