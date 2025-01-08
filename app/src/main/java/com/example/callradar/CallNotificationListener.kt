package com.example.callradar

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.mail.Transport

class CallNotificationListener : NotificationListenerService() {

    private val notificationTimestamps = mutableMapOf<String, Long>()
    private val logFile= "notification_log.txt"
    private val logFileCall = "notification_log_call.txt"

    var number: String? = null // Инициализация как nullable
    var type: Int? = null // Инициализация как nullable
    var date: Long? = null // Инициализация как nullable
    var duration: Long? = null // Инициализация как nullable
    var contactName: String? = null // Инициализация как nullable
    var accountApp: String? = null // Инициализация как nullable

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("ALLNotificationLog", "Сервис подключен")
        writeToLogFile("Сервис подключен\n", logFile)
        writeToLogFile("Сервис подключен\n", logFileCall)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title")
            val text = extras.getString("android.text")

            val timeStart = System.currentTimeMillis() // Время появления уведомления
            val formatTimeStart = formatTimestamp(timeStart)
            notificationTimestamps[sbn.key] = timeStart

            val logMessage = "[$formatTimeStart] app: $packageName, title: $title, text: $text\n"
            writeToLogFile(logMessage, logFile)
            Log.d("ALLNotificationLog", logMessage)


            // Фильтрация уведомлений по VoIP-приложениям
//            if (packageName.contains("telegram") || packageName.contains("whatsapp") || packageName.contains(
//                    "skype"
           if (title?.contains("call", ignoreCase = true) == true && text != null ) {
                handleVoIPNotification(title, text, timeStart)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        sbn?.let {
            val timeEnd = System.currentTimeMillis() // Время удаления уведомления
            val formatTimeEnd = formatTimestamp(timeEnd)
            val postedTime = notificationTimestamps[it.key]

            if (postedTime != null) {
                val duration = (timeEnd - postedTime) / 1000 // Продолжительность в секундах
                val logMessage = "[$formatTimeEnd] Notification removed: $packageName duration: $duration seconds\n"
                Log.d("VoIPNotification", logMessage)
                writeToLogFile(logMessage, logFile)
                writeToLogFile(logMessage, logFileCall)
                notificationTimestamps.remove(it.key) // Удалить запись из карты
            } else {
                val logMessage = "[$formatTimeEnd] Notification removed: $packageName\n"
                writeToLogFile(logMessage, logFile)
                writeToLogFile(logMessage, logFileCall)
                Log.d("VoIPNotification", logMessage)
            }

        }
    }

    private fun writeToLogFile(logMessage: String, fileName: String) {
        try {
            // Путь к внутреннему хранилищу
            val file = File(filesDir, fileName)
            FileOutputStream(file, true).use { fos ->
                fos.write(logMessage.toByteArray())
            }
            Log.d("NotificationListener", "Запись в файл успешна: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("NotificationListener", "Ошибка записи в файл: ${e.message}", e)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun checkStringToNumber(text: String?): Pair<String?, String?> {
        var number: String? = null
        var contactName: String? = null

        if (text != null && text.matches("^[+]?\\d+$".toRegex())) {
            // Если text не null и состоит только из цифр (и возможно знака "+" в начале)
            number = text
            contactName = null // Пустое значение, если это номер
        } else {
            // Если text null или в строке есть символы, не являющиеся цифрами
            number = null // Пустое значение, если это контакт
            contactName = text // Сохраняем как контактное имя
        }

        return Pair(number, contactName)
    }

    private fun handleVoIPNotification(title: String?, text: String?, timeStart: Long) {
        // Здесь вы можете обрабатывать содержимое уведомления

        val formatTimeStart = formatTimestamp(timeStart)

        Log.d("VoIPNotification", "----------------------------------------------")
        when {
            title?.startsWith("Telegram call", ignoreCase = true) == true && text != null -> {
                Log.d("VoIPNotification", "---------- ВХОДЯЩИЙ ОТ ${text} ------------")
                type = 1 // ВХОДЯЩИЙ
                val (num, name) = checkStringToNumber(text)
                number = num
                contactName = name
                accountApp = packageName.split(".")[1]
                Log.d(
                    "VoIPNotification",
                    "app: ${accountApp} type: ${type} number: ${number} contactName: ${contactName}"
                )
            }
            text?.contains("Missed Call", ignoreCase = true) == true -> {
                Log.d("VoIPNotification", "---------- ПРОПУЩЕННЫЙ ------------")
                type = 3 // ПРОПУЩЕННЫЙ
                val (num, name) = checkStringToNumber(text)
                number = num
                contactName = name
                accountApp = packageName.split(".")[1]
                Log.d(
                    "VoIPNotification",
                    "app: ${accountApp} type: ${type} number: ${number} contactName: ${contactName}"
                )
            }
            title?.contains("Ongoing Telegram call", ignoreCase = true) == true && text != null -> {
                Log.d("VoIPNotification", "---------- ИСХОДЯЩИЙ ------------")
                type = 2 // ИСХОДЯЩИЙ
                val (num, name) = checkStringToNumber(text)
                number = num
                contactName = name
                accountApp = packageName.split(".")[1]
                Log.d(
                    "VoIPNotification",
                    "app: ${accountApp} type: ${type} number: ${number} contactName: ${contactName}"
                )
            }
        }
        val logMessage = "[$formatTimeStart] app: $packageName, title: $title, text: $text\n"
        Log.d("VoIPNotification", logMessage)
        writeToLogFile(logMessage, logFileCall)
    }

}
