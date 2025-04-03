package com.example.callradar.callog

import CallLogUpdate
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.provider.CallLog
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import com.example.callradar.R
import android.Manifest

/**
 * Вспомогательный класс для работы с журналом звонков.
 */
object CallLogDataHelper {

    private var callLogObserver: CallLogUpdate? = null

    /**
     * Начинает мониторинг журнала звонков.
     */
    fun startCallLogMonitoring(context: Context, onLogUpdated: (List<CallLogEntry>) -> Unit) {
        val handler = Handler(context.mainLooper)

        // Создаем экземпляр CallLogUpdate с callback-ом, который будет вызываться при изменении данных
            callLogObserver = CallLogUpdate(context, handler) {
            Log.d("CallLogUpdate", "Обнаружено изменение в журнале звонков")
            val updatedLogs = fetchCallLogs(context) // Получаем обновленный список звонков
            onLogUpdated(updatedLogs) // Передаем обновленные данные
            Log.d("CallLogUpdate", "Обновленные данные: $updatedLogs") // Дополнительный лог
        }

        // Регистрируем ContentObserver
        context.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,  // URI для звонков
            true,  // Уведомление о всех изменениях
            callLogObserver!!
        )
    }

    /**
     * Извлекает список звонков из журнала.
     */
    fun fetchCallLogs(context: Context): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

        // Выполняем запрос к CallLog API
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,    // URI для журнала звонков
            null,                         // Все колонки
            null,                         // Без условий
            null,                         // Без параметров
            "${CallLog.Calls.DATE} DESC"  // Сортировка по дате (сначала новые)
        )

        // Обрабатываем результат запроса
        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) // Номер телефона
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))       // Тип звонка
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))      // Дата звонка
                val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)) // Длительность
                val cachedName = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: "Неизвестный"
                val accountId = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)) ?: "Мобильный"

                // Определяем название приложения или SIM-карты
                val accountApp = getAppNameFromComponent(context, accountId)

                // Добавляем данные звонка в список
                callLogs.add(
                    CallLogEntry(
                        number = number,
                        type = type,
                        date = date,
                        duration = duration,
                        contactName = cachedName,
                        accountApp = accountApp ?: ""
                    )
                )
            }
        }

        Log.d("CallLogHelper", "Список звонков: $callLogs")
        return callLogs
    }

    /**
     * Определяет название приложения или SIM-карты по идентификатору компонента.
     */
    private fun getAppNameFromComponent(context: Context, componentName: String?): String? {
        if (componentName.isNullOrEmpty()) return null

        return try {
            val packageName = ComponentName.unflattenFromString(componentName)?.packageName
            when (packageName) {
                "com.android.phone" -> "Мобильный"
                else -> packageName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun getContactPhoneNumbers(context: Context, contactName: String): List<String> {
        val phones = mutableListOf<String>()

        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            // First get the contact ID by name
            val contactId = getContactIdByName(context, contactName)
            if (contactId == null) return emptyList()

            // Then get all numbers for this contact ID
            val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
            val selectionArgs = arrayOf(contactId)

            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    cursor.getString(numberIndex)?.let { number ->
                        phones.add(number.replace("[^+0-9]".toRegex(), ""))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error getting contact numbers", e)
        }

        return phones.distinct()
    }

    private fun getContactIdByName(context: Context, contactName: String): String? {
        val uri = ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts._ID)
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?"
        val selectionArgs = arrayOf(contactName)

        return context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }
    }

    /**
     * Возвращает описание типа звонка.
     */
    fun getCallTypeDescription(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Входящий"
        CallLog.Calls.OUTGOING_TYPE -> "Исходящий"
        CallLog.Calls.MISSED_TYPE -> "Пропущенный"
        CallLog.Calls.REJECTED_TYPE -> "Отклонённый"
        CallLog.Calls.BLOCKED_TYPE -> "Заблокированный"
        CallLog.Calls.VOICEMAIL_TYPE -> "Голосовая почта"
        else -> "Неизвестный тип"
    }

    /**
     * Возвращает иконку для типа звонка (Material Design 2)
     */
    fun getCallTypeIcon(context: Context, type: Int): Drawable? = when (type) {
        CallLog.Calls.INCOMING_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_call_received)
        CallLog.Calls.OUTGOING_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_call_made)
        CallLog.Calls.MISSED_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_call_missed)
        CallLog.Calls.REJECTED_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_call_rejected)
        CallLog.Calls.BLOCKED_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_block)
        CallLog.Calls.VOICEMAIL_TYPE -> ContextCompat.getDrawable(context, R.drawable.ic_voicemail)
        else -> null
    }

    /**
     * Форматирование даты в разные форматы.
     */
    private fun formatDateddMMyyyy(date: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(date))
    private fun formatDateTime(date: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(date))
    private fun formatDateFull(date: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(date))

    /**
     * Группировка звонков по номеру и дате.
     */
    fun groupCallLogs(callLogs: List<CallLogEntry>): List<GroupedCallLog> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        return callLogs
            .groupBy { entry ->
                // Для неизвестных номеров группируем по номеру, для известных - по имени
                if (entry.contactName == "Неизвестный") entry.number else entry.contactName
            }
            .map { (key, group) ->
                val lastCall = group.maxByOrNull { it.date }!!

                GroupedCallLog(
                    type = lastCall.type,
                    number = lastCall.number,
                    callCount = if (group.size > 1) "(${group.size})" else "",
                    contactName = if (lastCall.contactName == "Неизвестный") "Неизвестный" else key,
                    date = dateFormatter.format(Date(lastCall.date)),
                    accountApp = lastCall.accountApp,
                    allNumbers = if (lastCall.contactName == "Неизвестный") listOf(lastCall.number)
                    else group.map { it.number }.distinct()
                )
            }
    }


    /**
     * Проверяет, является ли контакт избранным
     */
    fun isContactStarred(context: Context, phoneNumber: String): Boolean {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber))

            val projection = arrayOf(ContactsContract.Contacts.STARRED)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getInt(0) == 1
                }
            }
            false
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error checking starred status", e)
            false
        }
    }

    /**
     * Обновляет статус "Избранного" для контакта
     */
    fun setContactStarred(context: Context, phoneNumber: String, starred: Boolean): Boolean {
        if (!hasContactsPermissions(context)) {
            return false
        }

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber))

            val projection = arrayOf(ContactsContract.Contacts._ID)
            var updated = false

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getLong(0)
                    val values = ContentValues().apply {
                        put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
                    }

                    updated = context.contentResolver.update(
                        ContactsContract.Contacts.CONTENT_URI,
                        values,
                        "${ContactsContract.Contacts._ID} = ?",
                        arrayOf(contactId.toString())
                    ) > 0
                }
            }
            updated
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error setting starred status", e)
            false
        }
    }

    /**
     * Проверяет разрешения для работы с контактами
     */
    fun hasContactsPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    fun deleteCallLogForNumber(context: Context, number: String): Int {
        return try {
            context.contentResolver.delete(
                CallLog.Calls.CONTENT_URI,
                "${CallLog.Calls.NUMBER} = ?",
                arrayOf(number)
            )
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error deleting call log", e)
            0
        }
    }


    /**
     * Инициализация элементов для фрагмента деталей звонков.
     */
    fun initializeItems(context: Context) {
        ITEMS.clear()
        ITEM_MAP.clear()

        // Get all call logs
        val callLogs = fetchCallLogs(context)

        // Group logs by contact name (for known contacts) or by number (for unknown)
        val groupedLogs = groupCallLogs(callLogs)

        // First pass - collect all contact names and their numbers
        val contactNumbersMap = mutableMapOf<String, List<String>>()
        groupedLogs.filter { it.contactName != "Неизвестный" }.forEach { log ->
            if (!contactNumbersMap.containsKey(log.contactName)) {
                contactNumbersMap[log.contactName] = getContactPhoneNumbers(context, log.contactName)
            }
        }

        // Second pass - create CallDetail objects
        groupedLogs.forEach { log ->
            val allNumbers = if (log.contactName != "Неизвестный") {
                contactNumbersMap[log.contactName] ?: listOf(log.number)
            } else {
                log.allNumbers
            }

            // Get all call details for these numbers
            val details = callLogs
                .filter { call -> allNumbers.contains(call.number) }
                .map {
                    Detail(
                        number = it.number,
                        type = it.type,
                        date = it.date,
                        dateString = formatDateddMMyyyy(it.date),
                        timeString = formatDateTime(it.date),
                        duration = it.duration,
                        accountApp = it.accountApp
                    )
                }
                .sortedByDescending { it.date }

            // Create CallDetail
            val detail = CallDetail(
                number = log.number,
                contactName = log.contactName,
                allPhoneNumbers = allNumbers.distinct(),
                details = details
            )

            ITEMS.add(detail)

            // Map all numbers to this detail
            allNumbers.forEach { number ->
                ITEM_MAP[number] = detail
            }
        }

        // Add contacts that exist in address book but not in call log
        addContactsMissingInCallLog(context, contactNumbersMap)
    }

    private fun addContactsMissingInCallLog(context: Context, allContactsNumbers: Map<String, List<String>>) {
        allContactsNumbers.forEach { (contactName, numbers) ->
            // Проверяем, есть ли уже этот контакт в ITEMS
            val exists = ITEMS.any { it.contactName == contactName }

            if (!exists) {
                // Создаем пустой объект для контакта без звонков
                val detail = CallDetail(
                    number = numbers.firstOrNull() ?: "",
                    contactName = contactName,
                    allPhoneNumbers = numbers,
                    details = emptyList()
                )

                ITEMS.add(detail)
                numbers.forEach { number ->
                    ITEM_MAP[number] = detail
                }
            }
        }
    }

    /** Список и карта элементов для фрагмента. */
    val ITEMS: MutableList<CallDetail> = ArrayList()
    val ITEM_MAP: MutableMap<String, CallDetail> = HashMap()
}

