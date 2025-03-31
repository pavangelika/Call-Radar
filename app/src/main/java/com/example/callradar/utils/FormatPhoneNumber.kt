import android.content.Context

object FormatPhoneNumber {

    fun formatPhoneNumber(phoneNumber: String, isCity: Boolean = false): String {
        return try {
            val cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
            when {
                isCity && (cleanNumber.startsWith("8") || cleanNumber.startsWith("7")) && cleanNumber.length == 11 -> {
                    val regionCode = cleanNumber.substring(1, 4)
                    val mainNumber = cleanNumber.substring(4)
                    "${cleanNumber[0]} ($regionCode${cleanNumber[4]}) ${mainNumber.substring(1, 3)}-${mainNumber.substring(3, 5)}-${mainNumber.substring(5)}"
                }
                cleanNumber.startsWith("8") && cleanNumber.length == 11 -> {
                    "8 (${cleanNumber.substring(1, 4)}) ${cleanNumber.substring(4, 7)}-${cleanNumber.substring(7, 9)}-${cleanNumber.substring(9)}"
                }
                cleanNumber.startsWith("7") && cleanNumber.length == 11 -> {
                    "+7 (${cleanNumber.substring(1, 4)}) ${cleanNumber.substring(4, 7)}-${cleanNumber.substring(7, 9)}-${cleanNumber.substring(9)}"
                }
                cleanNumber.length == 10 -> {
                    "+7 (${cleanNumber.substring(0, 3)}) ${cleanNumber.substring(3, 6)}-${cleanNumber.substring(6, 8)}-${cleanNumber.substring(8)}"
                }
                else -> phoneNumber
            }
        } catch (e: Exception) {
            phoneNumber
        }
    }
}