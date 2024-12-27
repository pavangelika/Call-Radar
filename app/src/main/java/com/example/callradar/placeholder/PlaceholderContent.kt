package com.example.callradar.placeholder

// Импорт библиотек для работы с коллекциями
import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import kotlin.collections.HashMap


/**
 * Вспомогательный класс для предоставления содержимого (контента)
 */

/** Утилита для работы с журналом звонков
 */
object CallLogHelper {
    /** Возвращает список журнала звонков
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
                val number =
                    it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))   // Номер телефона
                val type =
                    it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))          // Тип звонка
                val date =
                    it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))         // Дата звонка
                val duration =
                    it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)) // Длительность
                val cachedName = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                    ?: "Неизвестный" // Имя контакта

                // Добавляем данные звонка в список
                callLogs.add(
                    CallLogEntry(
                        number = number,
                        type = type,
                        date = date,
                        duration = duration,
                        contactName = cachedName
                    )
                )
            }
        }

        Log.d("StartLog", "Список звонков: $callLogs")
        return callLogs // Возвращаем список звонков
    }

    /** Вспомогательная функция для форматирования типа звонка
     */
    fun getCallTypeDescription(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "Входящий"  //1
            CallLog.Calls.OUTGOING_TYPE -> "Исходящий" //2
            CallLog.Calls.MISSED_TYPE -> "Пропущенный" //3
            CallLog.Calls.REJECTED_TYPE -> "Отклонённый" //5
            CallLog.Calls.BLOCKED_TYPE -> "Заблокированный" //6
            CallLog.Calls.VOICEMAIL_TYPE -> "Голосовая почта" //4
            else -> "Неизвестный тип"
        }
    }

    /** Форматирование даты в "dd.MM.yyyy"
     */
    fun formatDateddMMyyyy(date: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(Date(date))
    }

    /** Форматирование даты в "HH:mm"
     */
    fun formatDateTime(date: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(date))
    }

    /** Форматирование даты в "dd.MM.yyyy HH:mm"
     */
    fun formatDateFull(date: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(date))
    }

    data class CallLogEntry(
        val number: String,
        val type: Int,
        val date: Long,
        val duration: Long,
        val contactName: String
    )

    data class GroupedCallLog(
        val type: Int,
        val number: String,
        val callCount: String, // Тип callCount изменён на String
        val date: String,
        val contactName: String
    )

    data class CallDetail(
        val number: String,
        val contactName: String,
        val details: List<Detail>
    )

    data class Detail(
        val type: Int,
        val date: String,
        val time: String,
        val duration: Long
    )

    fun callDetails(callLogs: List<CallLogEntry>): List<CallDetail> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        return callLogs
            .groupBy { entry ->
                entry.number
            }
            .map { (key, group) ->
                CallDetail(
                    number = key,
                    contactName = group.first().contactName,
                    details = group.map {
                        Detail(
                            type = it.type,
                            date = dateFormatter.format(Date(it.date)),
                            time = timeFormatter.format(Date(it.date)),
                            duration = it.duration
                        )
                    }
                )
            }
    }

    fun groupCallLogs(callLogs: List<CallLogEntry>): List<GroupedCallLog> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return callLogs
            .groupBy { entry ->
                Triple(
                    entry.number,
                    dateFormatter.format(Date(entry.date)),
                    entry.type
                )
            }
            .map { (key, group) ->
                GroupedCallLog(
                    type = key.third,
                    number = key.first,
                    callCount = if (group.size > 1) "(${group.size})" else "", // Преобразуем в строку
                    contactName = group.first().contactName,
                    date = key.second

                )
            }
    }

    /** Список элементов для ItemDetailFragment.
     */
    val ITEMS: MutableList<CallDetail> = ArrayList()

    /**Карта элементов для ItemDetailFragment, где ключ — это номер телефона.
     */
    val ITEM_MAP: MutableMap<String, CallDetail> = HashMap()

    fun initializeItems(context: Context) {
        // Очищаем текущие списки
        ITEMS.clear()
        ITEM_MAP.clear()
        // Получаем список звонков
        val callLogs = fetchCallLogs(context)
        // Группируем звонки
        val groupedCall = groupCallLogs(callLogs)
        val groupedCallDetails = callDetails(callLogs)

        Log.d("bundleLog", "groupedCall: $groupedCall")
        Log.d("bundleLog", "callDetail: $groupedCallDetails")

        // Заполняем ITEMS и ITEM_MAP
        groupedCallDetails.forEach { detail ->
            ITEMS.add(detail) // Добавляем детализированные звонки в ITEMS
            ITEM_MAP[detail.number] = detail // Пример использования `number` как ключа
        }


        // Логируем результат для проверки
        Log.d("bundleLog", "ITEMS: $ITEMS")
        Log.d("bundleLog", "ITEM_MAP keys: ${ITEM_MAP.keys}")

    }




}

