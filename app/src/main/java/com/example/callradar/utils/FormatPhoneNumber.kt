import android.content.Context

object FormatPhoneNumber {

    fun formatPhoneNumber(phone: String, isCity: Boolean = false): String {
        if (phone.isBlank()) return phone

        // Очищаем номер от всех нецифровых символов, кроме '+'
        val cleanNumber = phone.replace("[^+0-9]".toRegex(), "")

        return try {
            when {
                // Российские мобильные номера (формат: +7 123 456-78-90)
                cleanNumber.matches("^(\\+7|8)?9\\d{9}$".toRegex()) -> {
                    val digits = cleanNumber.takeLast(10) // Берем последние 10 цифр
                    "+7(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}"
                }

                // Российские городские номера (формат: +7 1234 56-78-90)
                cleanNumber.matches("^(\\+7)[1-8]\\d{9}$".toRegex()) && isCity -> {
                    val digits = cleanNumber.takeLast(10)
                    "+7(${digits.substring(0, 4)}) ${digits.substring(4, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}"
                }

                // Российские городские номера (формат: +7 1234 56-78-90)
                cleanNumber.matches("^(8)[1-8]\\d{9}$".toRegex()) && isCity -> {
                    val digits = cleanNumber.takeLast(10)
                    "8(${digits.substring(0, 4)}) ${digits.substring(4, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}"
                }

                // Международные номера (общий формат)
                cleanNumber.matches("^\\+[1-9]\\d{6,14}$".toRegex()) -> {
                    // Формат: +XXX (XX) XXX-XX-XX или подобный
                    val countryCode = cleanNumber.takeWhile { it.isDigit() || it == '+' }
                    val rest = cleanNumber.substring(countryCode.length)

                    when (countryCode.length) {
                        2 -> when { // +1 (USA/Canada)
                            rest.length >= 10 -> "+$countryCode (${rest.substring(0, 3)}) ${rest.substring(3, 6)}-${rest.substring(6, 10)}"
                            else -> phone
                        }
                        3 -> when { // +44 (UK)
                            rest.length >= 9 -> "+$countryCode (${rest.substring(0, 2)}) ${rest.substring(2, 5)} ${rest.substring(5, 9)}"
                            else -> phone
                        }
                        else -> phone
                    }
                }

                // Короткие номера (сервисные и т.д.)
                cleanNumber.length <= 6 -> cleanNumber

                // Все остальные случаи (включая неправильные номера)
                else -> phone
            }
        } catch (e: Exception) {
            phone
        }
    }
}