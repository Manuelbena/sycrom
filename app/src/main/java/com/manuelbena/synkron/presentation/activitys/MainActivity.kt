package com.manuelbena.synkron.presentation.activitys

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseActivity
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.data.local.notification.QuickAccessService
import com.manuelbena.synkron.databinding.ActivityMainBinding


import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var notificationHelper: NotificationHelper
    override fun inflateView(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    // 1. Definimos el lanzador para pedir el permiso
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Si el usuario dice SÍ, arrancamos el panel
            startQuickAccessService()
        }
    }


    override fun onResume() {
        super.onResume()
        // Comprobamos permisos cada vez que el usuario vuelve a la app
        checkPermissions()
        checkAndStartService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        setupBadge()

        binding.navView.setupWithNavController(navController)

        // Gestión de visibilidad del BottomNav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_calendar,
                R.id.navigation_money,
                R.id.navigation_note -> binding.navView.visibility = View.VISIBLE

                else -> binding.navView.visibility = View.GONE
            }
        }


    }

    private fun setupBadge() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Nos suscribimos al ViewModel
                viewModel.pendingCount.collect { count ->
                    // Ponemos un Log para ver en el Logcat si el dato llega
                    android.util.Log.d("SycromBadge", "Recibido contador desde ViewModel: $count")

                    // Llamamos a la función encargada de pintar
                    updateNotesBadge(count)
                }
            }
        }
    }

    private fun updateNotesBadge(count: Int) {
        val navView = binding.navView

        // ⚠️ IMPORTANTE: Verifica que este ID existe en tu 'res/menu/bottom_nav_menu.xml'
        val menuItemId = R.id.navigation_note

        // Obtenemos la insignia
        val badge = navView.getOrCreateBadge(menuItemId)

        if (count > 0) {
            badge.isVisible = true
            badge.number = count

            badge.backgroundColor = ContextCompat.getColor(this, R.color.md_theme_error)
            badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)
        } else {
            badge.isVisible = false
            badge.clearNumber() // Buena práctica limpiar el número al ocultar
        }
    }
        /**
         * Comprueba los permisos críticos para que la alarma funcione
         */
        private fun checkPermissions() {

            if (Build.VERSION.SDK_INT >= 33) { // Android 13 (Tiramisu)
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {

                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        101 // Un código de solicitud cualquiera
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    showPermissionDialog(
                        "Permiso de Alarma",
                        "Para que Sycrom pueda avisarte a la hora exacta, necesitas permitir las alarmas y recordatorios.",
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    )
                    return
                }
            }

            // 2. Permiso de Pantalla Completa (Android 14+) - CRÍTICO para mostrar la alarma con pantalla bloqueada
            if (Build.VERSION.SDK_INT >= 34) { // Android 14
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) {
                    showPermissionDialog(
                        "Aviso importante",
                        "Para que la alarma aparezca a pantalla completa cuando el móvil está bloqueado, activa este permiso.",
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                    )
                }
            }
        }

        private fun checkAndStartService() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // En Android 13+ hay que pedir permiso
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startQuickAccessService()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // En versiones antiguas no hace falta pedir permiso
                startQuickAccessService()
            }
        }

        private fun startQuickAccessService() {
            val intent = Intent(this, QuickAccessService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun showPermissionDialog(title: String, message: String, action: String) {
            // Evitamos mostrar diálogos repetidos si ya hay uno visible (opcional)
            // Aquí usamos un diálogo simple
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false) // Obligamos a leer
                .setPositiveButton("Activar ahora") { _, _ ->
                    try {
                        val intent = Intent(action)
                        if (action == Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT ||
                            action == Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        ) {
                            // Intentamos llevarle directo a nuestra app
                            intent.data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback a ajustes generales si la acción específica falla
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }