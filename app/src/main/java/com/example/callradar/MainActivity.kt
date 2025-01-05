package com.example.callradar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.callradar.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private var isButtonClicked = false // Флаг для отслеживания нажатия кнопки
    private lateinit var binding: ActivityMainBinding
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>


    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("StartLog", "___________START______________")


    }

    override fun onStart() {
        super.onStart()
        Log.d("StartLog", ".... Овца запущена.... - START")

        // Проверка выдано ли разрешение на наложение поверх окон
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermission()
        }

        overlayPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Snackbar.make(
                        binding.root,
                        "Нажмите на кнопку, чтобы перейти в настройки",
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction("Настройки") {
                            openOverlaySettings()
                        }.show()

                } else {
                    Log.d("StartLog", "MainActivity: Разрешение на наложение поверх других приложений выдано")
                    Log.d("StartLog", "MainActivity: Запрос разрешений на звонки, контакты и т.д.")
                    binding.tvPrms.text = "Для получения информации о звонках необходимо разрешить:"
                    checkAndRequestPermissions()

                }
            }

        // Инициализация запроса разрешений у пользователя
        requestPermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.forEach { (permission, isGranted) ->
                    if (isGranted) {
                        Log.d("StartLog", "$permission - РАЗРЕШЕНО")
                    } else {
                        binding.tvPrms.text =
                            "Для корректной работы необходимо разрешить:"
                        val showRationale =
                            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                        if (showRationale) {
                            Log.d("StartLog", "$permission - ОТКЛОНЕНО")
                            checkAndRequestPermissions()
                        } else {
                            Log.d("StartLog", "$permission - ЗАПРЕЩЕНО")
//                            isSnackbarActive = true
                            binding.tvPrms.text =
                                "Для корректной работы перейдите на вкладу 'Права' и предоставьте необходимые разрешения:"
                            Snackbar.make(
                                binding.root,
                                "Нажмите на кнопку, чтобы перейти в настройки",
                                Snackbar.LENGTH_INDEFINITE
                            )
                                .setAction("Настройки") {
                                    openAppSettings()
                                }.show()

                        }
                    }
                }
            }

    }

    override fun onResume() {
        super.onResume()
        Log.d("StartLog", ".... Поймай овцу..... - RESUME")
        checkAllPermission()


    }

    override fun onPause() {
        super.onPause()
        Log.d("StartLog", ".... Смена овец..... - PAUSE")
    }

    override fun onStop() {
        super.onStop()
        Log.d("StartLog", ".... Овцы пропали..... - STOP")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("StartLog", ".... Овцы на скотобойне..... - DESTROY")
    }

    private fun checkAllPermission(){
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        // Проверка наличия разрешений
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty() and Settings.canDrawOverlays(this)) {
            Log.d("StartLog", "MainActivity: Разрешения выданы")
            Log.d("StartLog", "MainActivity: Переход к журналу звонков")
            viewCallLog()
        } else {
            Log.d("StartLog", "$missingPermissions - НЕ ЗАПРОШЕНО")
        }
    }
    // Обработка перехода к системному окну разрешения на наложение поверх других приложений
    private fun showOverlayPermission() {
        binding.tvPrms.text =
            "Для получения информации о звонках разрешите наложение поверх других окон"
        Snackbar.make(
            binding.root,
            "Нажмите на кнопку, чтобы перейти в настройки",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Настройки") {
                openOverlaySettings()
            }.show()
    }
    // Показ системного окна разрешение наложения поверх других приложений
    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    // Проверка и запрос разрешений
    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        // Проверка наличия разрешений
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (!missingPermissions.isEmpty()) {
            Log.d("StartLog", "$missingPermissions")
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            binding.tvPrms.text = "Разрешения предоставлены"
            Log.d("StartLog", "MainActivity: Разрешения предоставлены")
        }
    }

    // Окно настроек приложения
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun viewCallLog(){
        val intent = Intent(this, ItemDetailHostActivity::class.java) // Создаём новый Intent
        startActivity(intent) // Запускаем Activity
    }

}



