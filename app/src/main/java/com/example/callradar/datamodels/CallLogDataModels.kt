package com.example.callradar.datamodels

import android.provider.ContactsContract

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
    val accountApp: String,
    val allNumbers: List<String> = listOf(number) // По умолчанию содержит только основной номер
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

data class PhoneNumberInfo(
    val number: String,
    val type: Int,
    val label: String? = null
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

data class ContactInfo(
    val id: String, // ID контакта
    val displayName: String, // Полное имя (как отображается)
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val nickname: String?,
    val organization: String?,
    val notes: String?,
    val birthday: String?,
    val ringtoneUri: String?,
    val photoUri: String?,
    val groups: List<String>,
    val phoneNumbers: List<PhoneNumberInfo>,
    val socialNetworks: List<SocialNetworkInfo>,
    val emails: List<EmailInfo>,
    val addresses: List<AddressInfo>,
    val starred: Boolean // Избранный контакт
){
    companion object {
        fun empty(id: String) = ContactInfo(
            id = id,
            displayName = "",
            firstName = null,
            lastName = null,
            middleName = null,
            nickname = null,
            organization = null,
            notes = null,
            birthday = null,
            ringtoneUri = null,
            photoUri = null,
            groups = emptyList(),
            phoneNumbers = emptyList(),
            socialNetworks = emptyList(),
            emails = emptyList(),
            addresses = emptyList(),
            starred = false
        )
    }
}

data class SocialNetworkInfo(
    val type: SocialNetworkType, // Тип соцсети
    val username: String,        // Имя пользователя/ID
    val profileUrl: String?,     // URL профиля (если доступен)
    val isVerified: Boolean = false // Подтвержденный аккаунт
)

enum class SocialNetworkType {
    FACEBOOK,
    TWITTER,
    INSTAGRAM,
    LINKEDIN,
    TELEGRAM,
    WHATSAPP,
    VK,
    OTHER;


    companion object {
        fun fromNamespace(namespace: String): SocialNetworkType {
            return when (namespace.lowercase()) {
                "facebook" -> FACEBOOK
                "twitter" -> TWITTER
                "instagram" -> INSTAGRAM
                "linkedin" -> LINKEDIN
                "telegram" -> TELEGRAM
                "whatsapp" -> WHATSAPP
                "vk", "vkontakte" -> VK
                else -> OTHER
            }
        }
    }
}

data class EmailInfo(
    val address: String,          // email@example.com
    val type: EmailType,          // Тип адреса
    val label: String?,           // Произвольная метка (если type = CUSTOM)
    val isPrimary: Boolean = false // Основной email
)

enum class EmailType {
    HOME,       // Домашний
    WORK,       // Рабочий
    MOBILE,     // Мобильный
    OTHER,      // Другой
    CUSTOM;     // Пользовательский (используется label)

    companion object {
        fun fromTypeConstant(type: Int): EmailType {
            return when (type) {
                ContactsContract.CommonDataKinds.Email.TYPE_HOME -> HOME
                ContactsContract.CommonDataKinds.Email.TYPE_WORK -> WORK
                ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> MOBILE
                ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM -> CUSTOM
                else -> OTHER
            }
        }
    }
}

data class AddressInfo(
    val type: AddressType,        // Тип адреса
    val label: String?,           // Произвольная метка
    val formattedAddress: String, // Полный адрес одной строкой
    val street: String?,          // Улица
    val city: String?,            // Город
    val region: String?,          // Область/регион
    val postalCode: String?,      // Почтовый индекс
    val country: String?,         // Страна
    val isPrimary: Boolean = false // Основной адрес
)

enum class AddressType {
    HOME,       // Домашний
    WORK,       // Рабочий
    OTHER,      // Другой
    CUSTOM;     // Пользовательский

    companion object {
        fun fromTypeConstant(type: Int): AddressType {
            return when (type) {
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> HOME
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> WORK
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM -> CUSTOM
                else -> OTHER
            }
        }
    }
}

