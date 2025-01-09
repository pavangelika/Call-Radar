package com.example.callradar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.callradar.databinding.FragmentPermissionsBinding
import com.google.android.material.snackbar.Snackbar

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    private val text_prms1 = "Для получения информации о звонках разрешите наложение поверх других окон"
    private val text_prms2 = "Для вывода звонков из всех приложений предоставьте доступ к уведомлениям: CallRadar - Call Notification Listener"
    private val text_prms3 = "Для получения информации о звонках необходимо разрешить:"
    private val text_prms4 = "Для корректной работы перейдите на вкладку 'Права' и предоставьте необходимые разрешения:"


    companion object {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.WRITE_CALL_LOG
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        Log.d("StartLog", "FragmentPermission - onCreateView")
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        Log.d("StartLog", "FragmentPermission - onViewCreated")
        setupPermissionLaunchers()
        checkOnStart()
    }

    private fun checkOnStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
                binding.tvPrms.text = text_prms1
                showSnackbar { openOverlaySettings() }
            }
        else if (!isNotificationAccessGranted()){
                binding.tvPrms.text = text_prms2
                showSnackbar { openNotificationSettings() }
            }
        else {
                binding.tvPrms.text = text_prms3
                checkAndRequestPermissions()
                Log.d("StartLog", "запрос прав у пользователя 1")
            }

        }


    /** overlayPermissionLauncher и checkAndRequestPermissions() */
    private fun setupPermissionLaunchers() {
        overlayPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(requireContext())) {
                    binding.tvPrms.text = text_prms1
                    showSnackbar {openOverlaySettings()}
                    Log.d("StartLog", "Запрошено разрешение на наложение поверх других окон")
                }
                else if (!isNotificationAccessGranted()) {
                    binding.tvPrms.text =text_prms2
                    showSnackbar { openNotificationSettings() }
                    Log.d("StartLog", "Запрошено разрешение на Call Notification")
                } else {
                    binding.tvPrms.text = text_prms3
                    checkAndRequestPermissions()
                    Log.d("StartLog", "запрос прав у пользователя 2")
                }
            }

        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                handlePermissionsResult(permissions)
            }


    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            binding.tvPrms.text = "Разрешения предоставлены"
            navigateToCallLog()
        }
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.isEmpty()) {
            binding.tvPrms.text = "Разрешения предоставлены"
            navigateToCallLog()
        } else {
            deniedPermissions.forEach { permission ->
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                    Log.d("StartLog", "$permission - ОТКЛОНЕНО")
                    checkAndRequestPermissions()
                    Log.d("StartLog", "запрос прав у пользователя 3")
                } else {
                    Log.d("StartLog", "$permission - ОТКАЗАНО НАВСЕГДА")
                    binding.tvPrms.text =text_prms4
                    showSnackbar { openAppSettings() }
                }
            }
        }
    }

    private fun showSnackbar(action: () -> Unit) {
        Snackbar.make(
            binding.root,
            "Нажмите, чтобы перейти в настройки",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Настройки") { action() }
            .show()
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return enabledListeners?.split(":")?.any { it.contains(requireContext().packageName) } == true
    }

        /** Показ системного окна для разрешения наложения поверх других приложений */
    private fun openOverlaySettings() {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
        overlayPermissionLauncher.launch(intent)
    }

        /** Диалоговое окно для запроса разрешений у пользователя */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

        /** Показ системного окна для разрешения чтения уведомлений всех приложений */
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun navigateToCallLog() {
        val intent = Intent(requireContext(), ItemDetailHostActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
