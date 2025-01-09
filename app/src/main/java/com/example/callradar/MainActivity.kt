package com.example.callradar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.callradar.calls.CallLogForegroundService
import com.example.callradar.calls.GroupedCallLog
import com.example.callradar.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions = PermissionsFragment.requiredPermissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка WorkManager для выполнения задачи раз в неделю
        val emailWorkRequest = PeriodicWorkRequestBuilder<EmailWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Задача выполняется только при наличии интернета
                    .setRequiresCharging(false) // Не обязательно на зарядке
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EmailWorker",  // Уникальное имя задачи
            ExistingPeriodicWorkPolicy.KEEP,  // Сохранить существующую задачу, если она уже запланирована
            emailWorkRequest

        )

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("StartLog", "___________START______________")
        Log.d("StartLog", ".... Овца родилась... - CREATE")

    }

    override fun onStart() {
        super.onStart()
        Log.d("StartLog", ".... Овца запущена.... - START")
        chooseFragment()

    }

    override fun onResume() {
        super.onResume()
        Log.d("StartLog", ".... Поймай овцу..... - RESUME")

    }

    override fun onPause() {
        super.onPause()
        Log.d("StartLog", ".... Овца замерла..... - PAUSE")
    }

    override fun onStop() {
        super.onStop()
        Log.d("StartLog", ".... Овца встала..... - STOP")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("StartLog", ".... Овца умерла..... - DESTROY")
    }



    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    private fun chooseFragment() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this) || !isNotificationAccessGranted() || missingPermissions.isNotEmpty())) {
            Log.d("StartLog", "Разрешения есть")

            startForegroundService()

            navigateToCallLog()
        } else {
            Log.d("StartLog", "Требуются разрешения")
            openPermissionsFragment()
        }
    }

    private fun navigateToCallLog() {
        val intent = Intent(this, ItemDetailHostActivity::class.java)
        startActivity(intent)
    }

    private fun openPermissionsFragment() {
        val permissionsFragment = PermissionsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, permissionsFragment)
            .addToBackStack(null)
            .commit()
    }

    /** Запуск Foreground Service */
    private fun startForegroundService(){
        val serviceIntent = Intent(this, CallLogForegroundService::class.java)
        startService(serviceIntent)
    }






}
