package com.example.callradar.calls

import CallLogUpdate
import android.content.ComponentName
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
import com.example.callradar.R

/**
 * Вспомогательный класс для работы с журналом звонков.
 */
object CallLogHelper {

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
    fun getAppNameFromComponent(context: Context, componentName: String?): String? {
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
    fun formatDateddMMyyyy(date: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(date))
    fun formatDateTime(date: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(date))
    fun formatDateFull(date: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(date))

    /**
     * Группировка звонков по номеру и дате.
     */
    fun groupCallLogs(callLogs: List<CallLogEntry>): List<GroupedCallLog> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return callLogs
            .groupBy {
                CallLogKey(
                    it.number,
                    dateFormatter.format(Date(it.date)),
                    it.type,
                    it.accountApp
                )
            }
            .map { (key, group) ->
                GroupedCallLog(
                    type = key.type,
                    number = key.number,
                    callCount = if (group.size > 1) "(${group.size})" else "",
                    contactName = group.first().contactName,
                    date = key.date,
                    accountApp = key.accountApp
                )
            }
    }

    /**
     * Формирование детализированных данных по звонкам.
     */
    fun callDetails(callLogs: List<CallLogEntry>): List<CallDetail> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        return callLogs
            .groupBy { it.number }
            .map { (number, group) ->
                CallDetail(
                    number = number,
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

    /**
     * Инициализация элементов для фрагмента деталей звонков.
     */
    fun initializeItems(context: Context) {
        ITEMS.clear()
        ITEM_MAP.clear()

        val callLogs = fetchCallLogs(context)
        val groupedDetails = callDetails(callLogs)

        groupedDetails.forEach { detail ->
            ITEMS.add(detail)
            ITEM_MAP[detail.number] = detail
        }

        Log.d("CallLogHelper", "ITEMS: $ITEMS")
        Log.d("CallLogHelper", "ITEM_MAP keys: ${ITEM_MAP.keys}")
    }

    /** Список и карта элементов для фрагмента. */
    val ITEMS: MutableList<CallDetail> = ArrayList()
    val ITEM_MAP: MutableMap<String, CallDetail> = HashMap()
}

