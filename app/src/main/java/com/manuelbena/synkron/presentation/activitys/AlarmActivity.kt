package com.manuelbena.synkron.presentation.activitys // O el package correcto donde lo tengas

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.manuelbena.synkron.databinding.ActivityAlarmBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmActivity : AppCompatActivity() {

    // Mismo ID que en el Receiver
    private val ALARM_NOTIFICATION_ID = 999
    private lateinit var binding: ActivityAlarmBinding
    // ELIMINADO: private var mediaPlayer: MediaPlayer? = null (Ya no lo necesitamos)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOn()
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Alarma"
        binding.tvTaskTitle.text = message

        // Listener del botón STOP
        binding.btnStop.setOnClickListener {
            stopAlarm()
        }
    }

    private fun stopAlarm() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Matamos la notificación (y el sonido) usando el ID fijo
        notificationManager.cancel(ALARM_NOTIFICATION_ID)

        // 2. Cerramos la pantalla
        finish()
    }

    private fun turnScreenOnAndKeyguardOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }
    }

}