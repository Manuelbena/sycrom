package com.syncro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.syncro.presentation.navigation.MainScaffold
import com.syncro.presentation.theme.SyncroTheme


import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Esto permite que tu app pinte detrás de la barra de estado y navegación del móvil,
        // dándole ese toque inmersivo y de "pantalla completa" súper moderno.
        enableEdgeToEdge()

        setContent {
            SyncroTheme {
                // ¡Adiós al código de ejemplo, hola a nuestra arquitectura limpia!
                // Llamamos directamente a nuestro componente principal que ya contiene
                // el Scaffold con el menú flotante y el NavHost.
                MainScaffold()
            }
        }
    }
}