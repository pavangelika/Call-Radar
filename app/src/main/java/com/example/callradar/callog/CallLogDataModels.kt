package com.example.callradar.callog

/**
 * Модели данных для работы с журналом звонков и контактами
 */

// Модель для записи в журнале звонков
data class CallLogEntry(
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long,
    val contactName: String,
    val accountApp: String
)


// Модель для группировки звонков по контакту/номеру
data class GroupedCallLog(
    val type: Int,
    val number: String,
    val callCount: String,
    val date: String,
    val contactName: String,
    val accountApp: String,
    val allNumbers: List<String> = listOf(number)
)

data class CallDetail(
    val number: String,
    val contactName: String,
    val allPhoneNumbers: List<String> = listOf(number),
    val details: List<Detail>
)

data class Detail(
    val number: String,
    val type: Int,
    val date: Long,  // Хранить как timestamp
    val dateString: String,  // Для отображения
    val timeString: String,
    val duration: Long,
    val accountApp: String
)



