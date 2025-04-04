package com.example.callradar.callog

import CallLogUpdate
import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.callradar.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object CallLogDataHelper {

    private var callLogObserver: CallLogUpdate? = null

    // region Константы для работы с контактами
    private val PROJECTION_CONTACT = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        ContactsContract.Contacts.STARRED,
        ContactsContract.Contacts.PHOTO_URI,
        ContactsContract.Contacts.CUSTOM_RINGTONE
    )

    private val PROJECTION_PHONE = arrayOf(
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL
    )

    private val PROJECTION_EMAIL = arrayOf(
        ContactsContract.CommonDataKinds.Email.ADDRESS,
        ContactsContract.CommonDataKinds.Email.TYPE,
        ContactsContract.CommonDataKinds.Email.LABEL,
        ContactsContract.CommonDataKinds.Email.IS_PRIMARY
    )

    private val PROJECTION_STRUCTURED_NAME = arrayOf(
        ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
        ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME
    )
    private val PROJECTION_ORGANIZATION = arrayOf(
        ContactsContract.CommonDataKinds.Organization.COMPANY,
        ContactsContract.CommonDataKinds.Organization.TITLE
    )

    private val PROJECTION_NOTE = arrayOf(
        ContactsContract.CommonDataKinds.Note.NOTE
    )

    private val PROJECTION_EVENT = arrayOf(
        ContactsContract.CommonDataKinds.Event.START_DATE,
        ContactsContract.CommonDataKinds.Event.TYPE
    )
    private val PROJECTION_SOCIAL = arrayOf(
        ContactsContract.CommonDataKinds.Im.DATA,
        ContactsContract.CommonDataKinds.Im.PROTOCOL,
        ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
    )
    private val PROJECTION_ADDRESS = arrayOf(
        ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
        ContactsContract.CommonDataKinds.StructuredPostal.STREET,
        ContactsContract.CommonDataKinds.StructuredPostal.CITY,
        ContactsContract.CommonDataKinds.StructuredPostal.REGION,
        ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY,
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
        ContactsContract.CommonDataKinds.StructuredPostal.LABEL,
        ContactsContract.CommonDataKinds.StructuredPostal.IS_PRIMARY
    )
    // endregion

    // region Основные методы работы с контактами

    /**
     * Читает полную информацию о контакте по его ID
     */
    @SuppressLint("Range")
    fun readFullContactInfo(context: Context, contactId: String): ContactInfo {
        if (!hasContactsPermissions(context)) {
            Log.w("ContactsReader", "No contacts permissions")
            return ContactInfo.empty(contactId)
        }

        val contentResolver = context.contentResolver
        var contactInfo = ContactInfo.empty(contactId)

        // 1. Получаем основную информацию о контакте
        val contactCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            PROJECTION_CONTACT,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId),
            null
        )

        contactCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactInfo = contactInfo.copy(
                    displayName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    ) ?: "",
                    photoUri = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                    ),
                    ringtoneUri = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.CUSTOM_RINGTONE)
                    ),
                    starred = cursor.getInt(
                        cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
                    ) == 1
                )
            } else {
                Log.w("ContactInfo", "No contact found with ID: $contactId")
            }
        } ?: Log.w("ContactInfo", "Contact cursor is null for ID: $contactId")

        // 2. Получаем структурированное имя (ФИО)
        val nameCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_STRUCTURED_NAME,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            ),
            null
        )

        nameCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactInfo = contactInfo.copy(
                    firstName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                    ),
                    lastName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                    ),
                    middleName = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
                    )
                )
            }
        }

        // 3. Получаем номера телефонов
        val phoneNumbers = getPhoneNumbers(contentResolver, contactId)
        contactInfo = contactInfo.copy(phoneNumbers = phoneNumbers)

        // 4. Получаем email-адреса
        val emails = getEmails(contentResolver, contactId)
        contactInfo = contactInfo.copy(emails = emails)

        // 5. Получаем группы контакта
        val groups = getContactGroups(contentResolver, contactId)
        contactInfo = contactInfo.copy(groups = groups)

        // 6. Никнейм
        val nickname = getNickname(contentResolver, contactId)
        contactInfo = contactInfo.copy(nickname = nickname)

        // 7. Организация
        val organization = getOrganization(contentResolver, contactId)
        contactInfo = contactInfo.copy(organization = organization)

        // 8. Заметки
        val notes = getNotes(contentResolver, contactId)
        contactInfo = contactInfo.copy(notes = notes)

        // 9. День рождения
        val birthday = getBirthday(contentResolver, contactId)
        contactInfo = contactInfo.copy(birthday = birthday)

        // 10. Социальные сети
        val socialNetworks = getSocialNetworks(contentResolver, contactId)
        contactInfo = contactInfo.copy(socialNetworks = socialNetworks)

        // 11. Адреса
        val addresses = getAddresses(contentResolver, contactId)
        contactInfo = contactInfo.copy(addresses = addresses)

        return contactInfo
    }

    // Add this method to CallLogDataHelper object
    /**
     * Finds contact ID by matching display name with contact name from call log
     */
    @SuppressLint("Range")
    fun findContactIdByName(context: Context, contactName: String): String? {
        if (!hasContactsPermissions(context)) return null

        return try {
            val contentResolver = context.contentResolver
            var contactId: String? = null

            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?",
                arrayOf(contactName),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                }
            }

            contactId
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error finding contact by name", e)
            null
        }
    }

    private fun getPhoneNumbers(
        contentResolver: android.content.ContentResolver,
        contactId: String
    ): List<PhoneNumberInfo> {
        val numbers = mutableListOf<PhoneNumberInfo>()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION_PHONE,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use { c ->
            val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (c.moveToNext()) {
                numbers.add(
                    PhoneNumberInfo(
                        number = c.getString(numberIdx),
                        type = c.getInt(typeIdx),
                        label = if (c.isNull(labelIdx)) null else c.getString(labelIdx)
                    )
                )
            }
        }

        return numbers
    }

    private fun getEmails(
        contentResolver: android.content.ContentResolver,
        contactId: String
    ): List<EmailInfo> {
        val emails = mutableListOf<EmailInfo>()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            PROJECTION_EMAIL,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use { c ->
            val addressIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)
            val primaryIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)

            while (c.moveToNext()) {
                emails.add(
                    EmailInfo(
                        address = c.getString(addressIdx),
                        type = EmailType.fromTypeConstant(c.getInt(typeIdx)),
                        label = if (c.isNull(labelIdx)) null else c.getString(labelIdx),
                        isPrimary = c.getInt(primaryIdx) == 1
                    )
                )
            }
        }

        return emails
    }

    private fun getContactGroups(
        contentResolver: android.content.ContentResolver,
        contactId: String
    ): List<String> {
        val groups = mutableListOf<String>()

        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
            ),
            null
        )

        cursor?.use { c ->
            val groupIdIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)
            while (c.moveToNext()) {
                val groupId = c.getString(groupIdIdx)
                val groupName = getGroupName(contentResolver, groupId)
                groupName?.let { groups.add(it) }
            }
        }

        return groups
    }

    private fun getGroupName(
        contentResolver: android.content.ContentResolver,
        groupId: String
    ): String? {
        val cursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups.TITLE),
            "${ContactsContract.Groups._ID} = ?",
            arrayOf(groupId),
            null
        )

        cursor?.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0)
            }
        }
        return null
    }
    private fun getNickname(contentResolver: ContentResolver, contactId: String): String? {
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Nickname.NAME),
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE
            ),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun getOrganization(contentResolver: ContentResolver, contactId: String): String? {
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_ORGANIZATION,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            ),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                val company = it.getString(0)
                val title = it.getString(1)
                if (!company.isNullOrEmpty() && !title.isNullOrEmpty()) {
                    "$company, $title"
                } else {
                    company ?: title
                }
            } else null
        }
    }

    private fun getNotes(contentResolver: ContentResolver, contactId: String): String? {
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_NOTE,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
            ),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun getBirthday(contentResolver: ContentResolver, contactId: String): String? {
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_EVENT,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ? AND " +
                    "${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
            ),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
    private fun getSocialNetworks(
        contentResolver: ContentResolver,
        contactId: String
    ): List<SocialNetworkInfo> {
        val socialNetworks = mutableListOf<SocialNetworkInfo>()

        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_SOCIAL,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
            ),
            null
        )

        cursor?.use { c ->
            val dataIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)
            val protocolIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL)
            val customProtoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)

            while (c.moveToNext()) {
                val username = c.getString(dataIdx)
                val protocol = c.getInt(protocolIdx)
                val customProto = if (c.isNull(customProtoIdx)) null else c.getString(customProtoIdx)

                val type = when (protocol) {
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM -> {
                        SocialNetworkType.fromNamespace(customProto ?: "")
                    }
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER -> SocialNetworkType.OTHER
                    ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING -> SocialNetworkType.OTHER
                    else -> SocialNetworkType.OTHER
                }

                if (type != SocialNetworkType.OTHER) {
                    socialNetworks.add(
                        SocialNetworkInfo(
                            type = type,
                            username = username,
                            profileUrl = null, // Android не хранит URL профиля
                            isVerified = false // Нет данных о верификации
                        )
                    )
                }
            }
        }

        return socialNetworks
    }
    private fun getAddresses(
        contentResolver: ContentResolver,
        contactId: String
    ): List<AddressInfo> {
        val addresses = mutableListOf<AddressInfo>()

        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            PROJECTION_ADDRESS,
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
            ),
            null
        )

        cursor?.use { c ->
            val formattedIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
            val streetIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
            val cityIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
            val regionIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
            val postalCodeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
            val countryIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
            val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
            val labelIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)
            val primaryIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.IS_PRIMARY)

            while (c.moveToNext()) {
                addresses.add(
                    AddressInfo(
                        type = AddressType.fromTypeConstant(c.getInt(typeIdx)),
                        label = if (c.isNull(labelIdx)) null else c.getString(labelIdx),
                        formattedAddress = c.getString(formattedIdx) ?: "",
                        street = if (c.isNull(streetIdx)) null else c.getString(streetIdx),
                        city = if (c.isNull(cityIdx)) null else c.getString(cityIdx),
                        region = if (c.isNull(regionIdx)) null else c.getString(regionIdx),
                        postalCode = if (c.isNull(postalCodeIdx)) null else c.getString(postalCodeIdx),
                        country = if (c.isNull(countryIdx)) null else c.getString(countryIdx),
                        isPrimary = c.getInt(primaryIdx) == 1
                    )
                )
            }
        }

        return addresses
    }
    // endregion

    // region Методы работы с журналом звонков
    fun startCallLogMonitoring(context: Context, onLogUpdated: (List<CallLogEntry>) -> Unit) {
        val handler = Handler(context.mainLooper)
        callLogObserver = CallLogUpdate(context, handler) {
            Log.d("CallLogUpdate", "Call log changed")
            onLogUpdated(fetchCallLogs(context))
        }
        context.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            callLogObserver!!
        )
    }

    @SuppressLint("Range")
    fun fetchCallLogs(context: Context): List<CallLogEntry> {
        val callLogs = mutableListOf<CallLogEntry>()

        if (!hasCallLogPermission(context)) return callLogs

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                callLogs.add(CallLogEntry(
                    number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)) ?: "",
                    type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)),
                    date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)),
                    duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION)),
                    contactName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)) ?: "Неизвестный",
                    accountApp = getAppNameFromComponent(
                        context,
                        cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME))
                    ) ?: "Мобильный"
                ))
            }
        }
        return callLogs
    }

    /**
     * Группирует список звонков по контактам и номерам телефонов
     */
    /**
     * Группировка звонков по номеру и дате.
     */
    fun groupCallLogs(callLogs: List<CallLogEntry>): List<GroupedCallLog> {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        return callLogs
            .groupBy { entry ->
                // Группируем по contactName, если он есть, иначе по номеру
                if (entry.contactName != "Неизвестный") entry.contactName else entry.number
            }
            .map { (key, group) ->
                val lastCall = group.maxByOrNull { it.date }!!
                val allNumbers = if (lastCall.contactName != "Неизвестный") {
                    // Для известных контактов собираем все уникальные номера из группы
                    group.map { it.number }.distinct()
                } else {
                    // Для неизвестных - только текущий номер
                    listOf(lastCall.number)
                }

                GroupedCallLog(
                    type = lastCall.type,
                    number = lastCall.number,
                    callCount = if (group.size > 1) "(${group.size})" else "",
                    contactName = lastCall.contactName,
                    date = dateFormatter.format(Date(lastCall.date)),
                    accountApp = lastCall.accountApp,
                    allNumbers = allNumbers
                )
            }
    }

    fun initializeItems(context: Context) {
        ITEMS.clear()
        ITEM_MAP.clear()

        val callLogs = fetchCallLogs(context)
        val groupedByContact = callLogs.groupBy { it.contactName }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        groupedByContact.forEach { (contactName, logs) ->
            val numbers = logs.map { it.number }.distinct()
            val primaryNumber = numbers.first()

            val callDetail = CallDetail(
                number = primaryNumber,
                contactName = contactName,
                allPhoneNumbers = numbers,
                details = logs.map { log ->
                    val date = Date(log.date)
                    Detail(
                        number = log.number,
                        type = log.type,
                        date = log.date,
                        duration = log.duration,
                        accountApp = log.accountApp,
                        dateString = dateFormat.format(date),
                        timeString = timeFormat.format(date)
                    )
                }
            )

            ITEMS.add(callDetail)
            numbers.forEach { number -> ITEM_MAP[number] = callDetail }
        }

        // Add contacts that aren't in call log
        if (hasContactsPermissions(context)) {
            addContactsMissingInCallLog(context)
        }
    }

    private fun addContactsMissingInCallLog(context: Context) {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: "Неизвестный"

                if (!ITEM_MAP.containsKey(number)) {
                    val callDetail = CallDetail(
                        number = number,
                        contactName = name,
                        allPhoneNumbers = listOf(number),
                        details = emptyList()
                    )
                    ITEMS.add(callDetail)
                    ITEM_MAP[number] = callDetail
                }
            }
        }
    }

    fun deleteCallLogForNumber(context: Context, number: String): Int {
        if (!hasWriteCallLogPermission(context)) return 0

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

    /* ========================= */
    /* === Contacts Methods === */
    /* ========================= */

    fun isContactStarred(context: Context, phoneNumber: String): Boolean {
        if (!hasContactsPermissions(context)) return false

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.STARRED),
                null, null, null
            )?.use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) == 1
            } ?: false
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error checking starred status", e)
            false
        }
    }

    fun setContactStarred(context: Context, phoneNumber: String, starred: Boolean): Boolean {
        if (!hasContactsPermissions(context)) return false

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            var updated = false
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getString(0)
                    val values = ContentValues().apply {
                        put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
                    }

                    updated = context.contentResolver.update(
                        ContactsContract.Contacts.CONTENT_URI,
                        values,
                        "${ContactsContract.Contacts._ID} = ?",
                        arrayOf(contactId)
                    ) > 0
                }
            }
            updated
        } catch (e: Exception) {
            Log.e("CallLogHelper", "Error setting starred status", e)
            false
        }
    }

    /* ======================== */
    /* === Utility Methods === */
    /* ======================== */

    fun getCallTypeIcon(context: Context, type: Int): Drawable? {
        val iconRes = when (type) {
            CallLog.Calls.INCOMING_TYPE -> R.drawable.ic_call_received
            CallLog.Calls.OUTGOING_TYPE -> R.drawable.ic_call_made
            CallLog.Calls.MISSED_TYPE -> R.drawable.ic_call_missed
            CallLog.Calls.REJECTED_TYPE -> R.drawable.ic_call_rejected
            CallLog.Calls.BLOCKED_TYPE -> R.drawable.ic_block
            CallLog.Calls.VOICEMAIL_TYPE -> R.drawable.ic_voicemail
            else -> null
        }
        return iconRes?.let { ContextCompat.getDrawable(context, it) }
    }

    private fun getAppNameFromComponent(context: Context, componentName: String?): String? {
        if (componentName.isNullOrEmpty()) return null
        return try {
            ComponentName.unflattenFromString(componentName)?.packageName?.let {
                when (it) {
                    "com.android.phone" -> "Мобильный"
                    else -> it
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun hasContactsPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWriteCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }


    val ITEMS: MutableList<CallDetail> = mutableListOf()
    val ITEM_MAP: MutableMap<String, CallDetail> = HashMap()
}