package com.example.callradar.utils

import android.Manifest
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.callradar.callog.CallLogDataHelper

object CallLogUtils {

    // Удалить записи журнала вызовов для определённого номера
    fun deleteCallLogForNumber(context: Context, number: String): Int {
        if (!hasWriteCallLogPermission(context)) {
            return -1
        }

        val contentResolver: ContentResolver = context.contentResolver
        val where = "${CallLog.Calls.NUMBER} = ?"
        val selectionArgs = arrayOf(number)

        return contentResolver.delete(CallLog.Calls.CONTENT_URI, where, selectionArgs)
    }

    // Проверить разрешение на запись в журнал вызовов
    fun hasWriteCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Нормализовать номер телефона (удалить лишние символы)
    fun normalizePhoneNumber(number: String): String {
        return number.replace("[^0-9+]".toRegex(), "")
            .replace("^8".toRegex(), "8") // Российские номера
            .replace("^7".toRegex(), "+7") // Уже международный
    }

    // Показать диалог подтверждения удаления журнала
    fun showDeleteCallLogDialog(
        context: Context,
        number: String,
        onDeleteConfirmed: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Удалить журнал звонков")
            .setMessage("Вы уверены, что хотите удалить все звонки для номера $number?")
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Обновить статус "Избранного" контакта
    fun toggleFavoriteStatus(
        context: Context,
        phoneNumber: String,
        isCurrentlyFavorite: Boolean,
        onSuccess: (newFavoriteStatus: Boolean) -> Unit,
        onFailure: () -> Unit
    ) {
        val newFavoriteStatus = !isCurrentlyFavorite
        val success = CallLogDataHelper.setContactStarred(context, phoneNumber, newFavoriteStatus)

        if (success) {
            onSuccess(newFavoriteStatus)
        } else {
            onFailure()
        }
    }
}