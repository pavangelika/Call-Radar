package com.example.callradar.utils

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.callradar.datamodels.TelegramContact

class GetTelegram {
    fun getTelegramContacts(context: Context): List<TelegramContact> {
        val telegramContacts = mutableListOf<TelegramContact>()

        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Im.DATA, // username
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
        )

        val selection = """
        ${ContactsContract.Data.MIMETYPE} = ? AND 
        ${ContactsContract.CommonDataKinds.Im.PROTOCOL} = ? AND
        ${ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL} = ?
    """.trimIndent()

        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM.toString(),
            "Telegram" // Идентификатор Telegram
        )

        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val usernameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)
            val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: "Unknown"
                val phone = cursor.getString(phoneIndex)
                val username = cursor.getString(usernameIndex)
                val photoUri = cursor.getString(photoIndex)?.let { Uri.parse(it) }

                telegramContacts.add(
                    TelegramContact(
                        id = contactId,
                        name = name,
                        phoneNumber = phone,
                        username = username,
                        photoUri = photoUri
                    )
                )
            }
        }

        return telegramContacts
    }
    fun loadContactPhoto(context: Context, contactId: Long): Bitmap? {
        return try {
            val uri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI,
                contactId
            )

            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                uri
            )

            if (inputStream != null) {
                BitmapFactory.decodeStream(inputStream).also {
                    inputStream.close()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ContactPhoto", "Error loading photo", e)
            null
        }
    }


}