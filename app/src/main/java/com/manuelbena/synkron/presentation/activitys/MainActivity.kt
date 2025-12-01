package com.manuelbena.synkron.presentation.activitys

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseActivity
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.databinding.ActivityMainBinding

import com.manuelbena.synkron.presentation.task_ia.TaskIaBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var notificationHelper: NotificationHelper
    override fun inflateView(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }


    override fun onResume() {
        super.onResume()
        // Comprobamos permisos cada vez que el usuario vuelve a la app
        checkPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        // --- PRUEBA TEMPORAL ---
        binding.root.postDelayed({
            try {
                notificationHelper.showStandardNotification(
                    "Prueba de Sistema",
                    "Si ves esto, las notificaciones funcionan.",
                    "12345"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 5000)

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

        
        // --- CHECK ESPECÍFICO PARA VIVO AL INICIAR ---
        checkVivoAutoStart()
    }

    /**
     * Comprueba los permisos críticos para que la alarma funcione
     */
    private fun checkPermissions() {

        if (Build.VERSION.SDK_INT >= 33) { // Android 13 (Tiramisu)
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {

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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                showPermissionDialog(
                    "Aviso importante",
                    "Para que la alarma aparezca a pantalla completa cuando el móvil está bloqueado, activa este permiso.",
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                )
            }
        }
    }

    /**
     * Lógica especial para móviles VIVO (Funtouch OS)
     * Intenta abrir el gestor de "Inicio Automático" o "High Background Power Consumption".
     */
    private fun checkVivoAutoStart() {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        if (manufacturer.contains("vivo")) {
            // Usamos una preferencia simple para no molestar al usuario cada vez que abre la app
            val prefs = getSharedPreferences("SycromSettings", Context.MODE_PRIVATE)
            val isVivoChecked = prefs.getBoolean("vivo_autostart_checked", false)

            if (!isVivoChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Configuración Vivo")
                    .setMessage("En móviles Vivo, debes permitir el 'Inicio Automático' para que la alarma suene con la pantalla apagada. \n\nPor favor, busca Sycrom en la lista y actívalo.")
                    .setPositiveButton("Ir a Configuración") { _, _ ->
                        openVivoManager()
                        // Guardamos que ya le hemos avisado
                        prefs.edit().putBoolean("vivo_autostart_checked", true).apply()
                    }
                    .setNegativeButton("Más tarde", null)
                    .show()
            }
        }
    }

    private fun openVivoManager() {
        try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
                startActivity(intent)
            } catch (e2: Exception) {
                try {
                    val intent = Intent()
                    intent.component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.PurviewTabActivity"
                    )
                    startActivity(intent)
                } catch (e3: Exception) {
                    // Si fallan los intentos específicos de Vivo, abrimos la info de la app normal
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
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
                        action == Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM) {
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