package com.example.callradar.callog

/**
 * Модели данных для работы с журналом звонков.
 */

data class CallLogEntry(
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long,
    val contactName: String,
    val accountApp: String
)

data class GroupedCallLog(
    val type: Int,
    val number: String,
    val callCount: String,
    val date: String,
    val contactName: String,
    val accountApp: String,
    val allNumbers: List<String> = listOf(number) // Все номера контакта
)

data class CallDetail(
    val number: String,
    val contactName: String,
    val allPhoneNumbers: List<String>,
    val details: List<Detail>
)

data class Detail(
    val type: Int,
    val date: String,
    val time: String,
    val duration: Long,
    val accountApp: String
)

data class CallLogKey(
    val number: String,
    val date: String,
    val type: Int,
    val accountApp: String
)
