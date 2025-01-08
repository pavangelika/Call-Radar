package com.example.callradar.calls

import CallLogUpdate
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.util.Log
import androidx.core.app.NotificationCompat

class CallLogForegroundService : Service() {

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // Создаем уведомление для Foreground сервиса
        createNotificationChannel()

        // Отправляем сервис в foreground
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Radar")
            .setContentText("Слежение за звонками активно")
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .build()

        // Обязателен вызов startForeground, иначе система может завершить сервис
        startForeground(FOREGROUND_ID, notification)

        // Запускаем отслеживание звонков
//        startCallLogUpdate()

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Log Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

//    private fun startCallLogUpdate() {
//        val handler = Handler(Looper.getMainLooper())
//        val onCallLogChanged: () -> Unit = {
//            Log.d("CallLogForegroundService", "Журнал звонков изменился")
//        }
//
//        val callLogUpdate = CallLogUpdate(applicationContext, handler, onCallLogChanged)
//        contentResolver.registerContentObserver(
//            CallLog.Calls.CONTENT_URI,
//            true,
//            callLogUpdate
//        )
//    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "CallLogServiceChannel"
        private const val FOREGROUND_ID = 1
    }
}