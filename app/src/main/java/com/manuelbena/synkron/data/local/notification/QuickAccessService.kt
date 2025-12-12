package com.manuelbena.synkron.data.local.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.presentation.activitys.MainActivity
import com.manuelbena.synkron.presentation.activitys.ContainerActivity

class QuickAccessService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // Si el sistema lo mata, lo reinicia automáticamente
    }

    private fun createNotification(): Notification {
        val channelId = "quick_access_channel"
        createChannel(channelId)

        // Intents para los botones
        val intentHome = Intent(this, MainActivity::class.java)
        val piHome = PendingIntent.getActivity(
            this, 1, intentHome,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // <--- CORRECCIÓN AQUÍ
        )

        // Acción: Nueva Tarea
        val intentNewTask = Intent(this, ContainerActivity::class.java).apply {
            putExtra("EXTRA_START_FRAGMENT", "NEW_TASK")
            // Añadimos estos flags para forzar una actividad nueva y limpia
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piNewTask = PendingIntent.getActivity(
            this, 2, intentNewTask,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // <--- CORRECCIÓN AQUÍ
        )

        // Acción: Calendario
        val intentCalendar = Intent(this, ContainerActivity::class.java).apply {
            putExtra("EXTRA_START_FRAGMENT", "CALENDAR")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piCalendar = PendingIntent.getActivity(
            this, 3, intentCalendar,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // <--- CORRECCIÓN AQUÍ
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sykrom Panel")
            .setContentText("Accesos rápidos")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

            // Botón 1: Nueva Tarea
            .addAction(R.drawable.ic_plus, "Nueva Tarea", piNewTask) // He puesto ic_plus, usa tu icono
            // Botón 2: Calendario
            .addAction(R.drawable.ic_calendar_today, "Calendario", piCalendar)
            // Botón 3: Home
            .addAction(R.drawable.ic_home_black_24dp, "Home", piHome)

            .setContentIntent(piHome)
            .build()
    }
    private fun createChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "Sykrom Quick Access",
                NotificationManager.IMPORTANCE_LOW // Low: Sin sonido, solo visual
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 999
    }
}