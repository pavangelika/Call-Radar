package com.example.callradar

import DatabaseHelper
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.View
import android.widget.TextView
import com.example.callradar.databinding.OverlayLayoutBinding

/** Сервис для отображения Overlay с номером телефона */
class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var helper: DatabaseHelper

    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        startForegroundService()
        helper = DatabaseHelper(this)
        helper.copyDatabase() // Подключение к базе данных

    }

    override fun onDestroy() {
        super.onDestroy()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        removeOverlay()
    }


    /**
     * Показ Overlay с номером телефона с задержкой
     */
    fun showOverlayWithDelay(phoneNumber: String, delayMillis: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            showOverlay(phoneNumber)
        }, delayMillis)
    }
    /**
     * Показ Overlay с номером телефона
     */
    private fun showOverlay(phoneNumber: String) {
        removeOverlay() // Удаляем старый overlay, если он есть

        val layoutInflater = LayoutInflater.from(this)
        overlayView = layoutInflater.inflate(R.layout.overlay_layout, null)

        val info = helper.searchPhone(phoneNumber)

        val textView = overlayView!!.findViewById<TextView>(R.id.overlay_text)
        textView.text = "$info"

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER
        // Пробуждение экрана перед показом оверлея:
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "OverlayService::WakeLock"
        )
        wakeLock.acquire(3000) // Пробуждаем экран на 3 секунды


        Handler(Looper.getMainLooper()).post {
            try {
                if (overlayView == null) {
                    Log.e("OverlayService", "overlayView == null")
                }
                if (windowManager == null) {
                    Log.e("OverlayService", "windowManager == null")
                }
                Log.d("StartLog", "Текущая версия Android: ${Build.VERSION.SDK_INT}")
                Log.d("StartLog", "Пытаемся добавить Overlay")
                windowManager.addView(overlayView, layoutParams)
                Log.d("StartLog", "Overlay добавлен для номера: $phoneNumber")
                Log.d("StartLog", "OverlayView: $overlayView")
                Log.d("StartLog", "LayoutParams: $layoutParams")
            } catch (e: Exception) {
                Log.e("StartLog", "Ошибка при добавлении Overlay: ${e.message}")
            }
        }
    }

    /**
     * Удаление Overlay
     */
    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
                overlayView = null
            } catch (e: Exception) {
                Log.e("StartLog", "Ошибка при удалении Overlay: ${e.message}")
            }
        }
    }

    companion object {
        fun start(context: Context, phoneNumber: String) {
            val intent = Intent(context, OverlayService::class.java)
            intent.putExtra("phone_number", phoneNumber)
            Log.d("StartLog", "Intent для запуска: $intent")
            ContextCompat.startForegroundService(context, intent)
            Log.d("StartLog", "ForegroundService запущен")
            Log.d("StartLog", "Вызов startForegroundService завершен")
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("StartLog", "onStartCommand вызван")
        intent?.getStringExtra("phone_number")?.let { phoneNumber ->
            Log.d("StartLog", "Получен номер: $phoneNumber")
//            showOverlay(phoneNumber)
            // Задержка 0.3 секунд перед показом overlay
            showOverlayWithDelay(phoneNumber, 300L)
        }
        return START_STICKY
    }

    /**
     * Настройка Foreground Service для Android 12+
     */
    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        val channelId = "overlay_service_channel"
        val channelName = "Overlay Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Service")
            .setContentText("Служба активна")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Устанавливаем высокий приоритет
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // Указываем категорию
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }
    /**
     * Слушатель для отслеживания состояния вызовов
     */
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Звонок завершен
                    Log.d("StartLog", "CALL_STATE_IDLE: Убираем Overlay")
                    removeOverlay()
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Звонок активен
                    Log.d("StartLog", "CALL_STATE_OFFHOOK: Звонок начался")
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    // Входящий вызов
                    Log.d("StartLog", "CALL_STATE_RINGING: Звонит номер $phoneNumber")
                }
            }
        }
    }
}
