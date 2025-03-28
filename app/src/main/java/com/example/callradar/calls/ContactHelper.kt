import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactHelper {
    fun getContactPhotoUri(context: Context, phoneNumber: String): Uri? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

                val projection = arrayOf(ContactsContract.Contacts.PHOTO_URI)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.let { Uri.parse(it) }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}