import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import com.example.callradar.R
import java.net.URL

object SocialMediaHelper {
    // Основной метод для получения фото
    fun getContactPhoto(context: Context, phoneNumber: String): Bitmap? {
        return try {
            // Пробуем получить фото из разных источников
            getTelegramPhotoFromCache(context, phoneNumber)
                ?: getViberContactPhoto(context, phoneNumber)
                ?: getVKPhotoFromCache(context, phoneNumber)
                ?: getPhoneContactPhoto(context, phoneNumber)
        } catch (e: Exception) {
            null
        }
    }

    // Telegram implementation
    private fun getTelegramPhotoFromCache(context: Context, phoneNumber: String): Bitmap? {
        return try {
            val contentResolver = getContentResolverForPackage(
                context,
                "org.telegram.messenger",
                "content://org.telegram.messenger.provider/contacts"
            ) ?: return null

            val cursor = contentResolver.query(
                Uri.parse("content://org.telegram.messenger.provider/contacts"),
                arrayOf("photo_uri"),
                "phone=?",
                arrayOf(phoneNumber),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    loadBitmapFromUri(it.getString(0))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Viber implementation
    private fun getViberContactPhoto(context: Context, phoneNumber: String): Bitmap? {
        return try {
            val contentResolver = getContentResolverForPackage(
                context,
                "com.viber.voip",
                "content://com.viber.voip.provider/contacts"
            ) ?: return null

            val cursor = contentResolver.query(
                Uri.parse("content://com.viber.voip.provider/contacts"),
                arrayOf("photo_uri"),
                "number=?",
                arrayOf(phoneNumber),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    loadBitmapFromUri(it.getString(0))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // VK implementation
    private fun getVKPhotoFromCache(context: Context, phoneNumber: String): Bitmap? {
        return try {
            val contentResolver = getContentResolverForPackage(
                context,
                "com.vkontakte.android",
                "content://com.vkontakte.android.provider.UserProvider/phones"
            ) ?: return null

            val cursor = contentResolver.query(
                Uri.parse("content://com.vkontakte.android.provider.UserProvider/phones"),
                arrayOf("photo_url"),
                "phone_number=?",
                arrayOf(phoneNumber),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    loadBitmapFromUri(it.getString(0))
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Phone contacts implementation
    private fun getPhoneContactPhoto(context: Context, phoneNumber: String): Bitmap? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

                val projection = arrayOf(ContactsContract.Contacts.PHOTO_URI)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val photoUri = cursor.getString(0)
                    if (photoUri != null) {
                        ContactsContract.Contacts.openContactPhotoInputStream(
                            context.contentResolver,
                            Uri.parse(photoUri)
                        )?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Helper methods
    private fun getContentResolverForPackage(
        context: Context,
        packageName: String,
        uriToCheck: String
    ) = try {
        val app = context.packageManager.getApplicationInfo(packageName, 0)
        val contentResolver = context.createPackageContext(
            app.packageName,
            Context.CONTEXT_IGNORE_SECURITY
        ).contentResolver

        // Проверяем доступность провайдера
        contentResolver.query(Uri.parse(uriToCheck), null, null, null, null)?.close()
        contentResolver
    } catch (e: Exception) {
        null
    }

    private fun loadBitmapFromUri(uriString: String?): Bitmap? {
        return try {
            uriString?.let {
                BitmapFactory.decodeStream(URL(it).openStream())
            }
        } catch (e: Exception) {
            null
        }
    }

}