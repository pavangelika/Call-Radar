//BroadcastReceiver для получения событий изменения состояния телефона:

package com.example.callradar

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем, что это событие изменения состояния телефона
        if (intent.action == android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
            val incomingNumber =
                intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (stateStr == android.telephony.TelephonyManager.EXTRA_STATE_RINGING && incomingNumber != null) {
                // Проверка разрешения на чтение контактов
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val contactName = getContactName(context, incomingNumber)
                    if (contactName != null) {
                        Log.d("CallReceiver", "Имя контакта: $contactName, Номер: $incomingNumber")
                    } else {
                        Log.d("CallReceiver", "Номера нет в контактах: $incomingNumber")
                        Log.d("CallReceiver", "Запуск OverlayService для номера: $incomingNumber")
                        Log.d("OverlayServiceDebug", "Вызов OverlayService.start")


                        OverlayService.start(context, incomingNumber)
                        Log.d("OverlayServiceDebug", "OverlayService.start вызван")
                    }
                } else {
                    Log.d("CallReceiver", "Нет разрешения на чтение контактов")
                }
            }
        }

        if (intent.action == "android.intent.action.PHONE_STATE") {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Звонок завершен
                OverlayService.stop(context)
                Log.d("StartLog", "Звонок завершен, оверлей закрыт")
            }
        }


    }

    /**
     * Метод для поиска имени контакта по номеру телефона
     */

    private fun getContactName(context: Context, phoneNumber: String): String? {
        // Убираем все нецифровые символы (например, пробелы и скобки)
        val normalizedPhoneNumber = phoneNumber.replace(Regex("[^\\d]"), "")

        val contentResolver = context.contentResolver
        val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number =
                    it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))

                // Убираем все нецифровые символы из номера контакта
                val normalizedContactNumber = number.replace(Regex("[^\\d]"), "")

                // Сравниваем номера с использованием PhoneNumberUtils
                if (PhoneNumberUtils.compare(normalizedContactNumber, normalizedPhoneNumber)) {
                    return name
                }

                Log.d(
                    "ContactsDebug",
                    "Номер в контактах: $number, Нормализованный номер: $normalizedContactNumber"
                )

            }
        }
        return null
    }



}



