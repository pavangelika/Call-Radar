package com.example.callradar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.callradar.calls.CallLogForegroundService
import com.example.callradar.calls.CallLogHelper
import com.example.callradar.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.WRITE_CALL_LOG)

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
        setupPermissionLaunchers()
    }

    override fun onStart() {
        super.onStart()
        Log.d("StartLog", ".... Овца запущена.... - START")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            binding.tvPrms.text = "Для получения информации о звонках разрешите наложение поверх других окон"
            showSnackback{openOverlaySettings()}
        } else if (!isNotificationAccessGranted()){
            binding.tvPrms.text = "Для вывода звонков из всех приложений предоставьте доступ к уведомлениям"
            showSnackback{openNotificationSettings()}
        }else {
            binding.tvPrms.text = "Для получения информации о звонках необходимо разрешить:"
            checkAndRequestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("StartLog", ".... Поймай овцу..... - RESUME")
        verifyPermissions()
    }

    /** overlayPermissionLauncher и checkAndRequestPermissions() */
    private fun setupPermissionLaunchers() {
        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                binding.tvPrms.text = "Для получения информации о звонках разрешите наложение поверх других окон"
                showSnackback{openOverlaySettings()}
            } else if (!isNotificationAccessGranted()) {
                binding.tvPrms.text = "Для вывода звонков из всех приложений предоставьте доступ к уведомлениям: CallRadar - Call Notification Listener"
                showSnackback{openNotificationSettings()}
            } else {
                binding.tvPrms.text = "Для получения информации о звонках необходимо разрешить:"
                checkAndRequestPermissions()
            }
        }

        permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionsResult(permissions)
        }
    }

    /** Финальная проверка предоставленных разрешений */
    private fun verifyPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            binding.tvPrms.text = "Разрешения предоставлены"
            Log.d("StartLog", "Разрешения предоставлены")
            navigateToCallLog()

            // Запуск Foreground Service
            val serviceIntent = Intent(this, CallLogForegroundService::class.java)
            startForegroundService(serviceIntent)

        } else {
            Log.d("StartLog", "Missing permissions: $missingPermissions - НЕ РАЗРЕШЕНО")
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            binding.tvPrms.text = "Разрешения предоставлены"
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, isGranted) ->
            when {
                isGranted -> Log.d("StartLog", "$permission - РАЗРЕШЕНО")
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                    Log.d("StartLog", "$permission - ОТКЛОНЕНО ")
                    checkAndRequestPermissions()
                }
                else -> {
                    Log.d("StartLog", "$permission - ОТКАЗАНО НАВСЕГДА")
                    binding.tvPrms.text =
                        "Для корректной работы перейдите на вкладу 'Права' и предоставьте необходимые разрешения:"
                    showSnackback{openAppSettings()}
                }
            }
        }
    }

    private fun showSnackback(action: () -> Unit){
        Snackbar.make(binding.root, "Нажмите, чтобы перейти в настройки", Snackbar.LENGTH_INDEFINITE)
            .setAction("Настройки") { action() }
            .show()
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }

    /** Показ системного окна для разрешения наложения поверх других приложений */
    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        overlayPermissionLauncher.launch(intent)
    }

    /** Диалоговое окно для запроса разрешений у пользователя */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    /** Показ системного окна для разрешения чтения уведомлений всех приложений */
    private fun openNotificationSettings(){
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun navigateToCallLog() {
        startActivity(Intent(this, ItemDetailHostActivity::class.java))
    }
}
