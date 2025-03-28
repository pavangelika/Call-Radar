import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.sql.SQLException
import java.io.FileOutputStream
import java.io.IOException


class GetRegionFromNumber(private val appContext: Context) :
    SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "sqlitenumbers.db" // имя файла в assets
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("StartLog", "Database created")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.d("StartLog", "Database upgraded")
    }

    fun copyDatabase() {
        val dbPath = appContext.getDatabasePath(DATABASE_NAME)

        if (!dbPath.exists()) {
            // Создаем папку, если её нет
            dbPath.parentFile?.mkdirs()
        }
        try {
            // Открываем файл в assets
            val inputStream = appContext.assets.open(DATABASE_NAME)

            // Создаем файл базы данных
            val outputStream = FileOutputStream(dbPath)

            // Копируем данные
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            // Закрываем потоки
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d("DatabaseHelper", "Database copied successfully to ${dbPath.absolutePath}")
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Error copying database: ${e.message}")
        }
    }


    val countryCodes = mapOf(
        "Австралия" to "+61",
        "Австрия" to "+43",
        "Азербайджан" to "+994",
        "Албания" to "+355",
        "Алжир" to "+213",
        "Ангола" to "+244",
        "Андорра" to "+376",
        "Антигуа и Барбуда" to "+1268",
        "Аргентина" to "+54",
        "Армения" to "+374",
        "Афганистан" to "+93",
        "Багамы" to "+1242",
        "Бангладеш" to "+880",
        "Барбадос" to "+1246",
        "Бахрейн" to "+973",
        "Беларусь" to "+375",
        "Белиз" to "+501",
        "Бельгия" to "+32",
        "Бенин" to "+229",
        "Болгария" to "+359",
        "Боливия" to "+591",
        "Босния и Герцеговина" to "+387",
        "Ботсвана" to "+267",
        "Бразилия" to "+55",
        "Бруней" to "+673",
        "Буркина Фасо" to "+226",
        "Бурунди" to "+257",
        "Бутан" to "+975",
        "Вануату" to "+678",
        "Ватикан" to "+39",
        "Великобритания" to "+44",
        "Венгрия" to "+36",
        "Венесуэла" to "+58",
        "Восточный Тимор" to "+670",
        "Вьетнам" to "+84",
        "Габон" to "+241",
        "Гаити" to "+509",
        "Гайана" to "+592",
        "Гамбия" to "+220",
        "Гана" to "+233",
        "Гватемала" to "+502",
        "Гвинея" to "+224",
        "Гвинея-Бисау" to "+245",
        "Германия" to "+49",
        "Гондурас" to "+504",
        "Гренада" to "+1473",
        "Греция" to "+30",
        "Грузия" to "+995",
        "Дания" to "+45",
        "Джибути" to "+253",
        "Доминика" to "+1767",
        "Доминиканская Республика" to "+1809",
        "Египет" to "+20",
        "Замбия" to "+260",
        "Зимбабве" to "+263",
        "Израиль" to "+972",
        "Индия" to "+91",
        "Индонезия" to "+62",
        "Иордания" to "+962",
        "Ирак" to "+964",
        "Иран" to "+98",
        "Ирландия" to "+353",
        "Исландия" to "+354",
        "Испания" to "+34",
        "Италия" to "+39",
        "Йемен" to "+967",
        "Кабо-Верде" to "+238",
        "Казахстан" to "+77",
        "Камбоджа" to "+855",
        "Камерун" to "+237",
        "Канада" to "+1",
        "Катар" to "+974",
        "Кения" to "+254",
        "Кипр" to "+357",
        "Киргизия" to "+996",
        "Кирибати" to "+686",
        "Китай" to "+86",
        "Колумбия" to "+57",
        "Коморы" to "+269",
        "Конго, демократическая республика" to "+243",
        "Конго, республика" to "+242",
        "Коста-Рика" to "+506",
        "Кот-д’Ивуар" to "+225",
        "Куба" to "+53",
        "Кувейт" to "+965",
        "Лаос" to "+856",
        "Латвия" to "+371",
        "Лесото" to "+266",
        "Либерия" to "+231",
        "Ливан" to "+961",
        "Ливия" to "+218",
        "Литва" to "+370",
        "Лихтенштейн" to "+423",
        "Люксембург" to "+352",
        "Маврикий" to "+230",
        "Мавритания" to "+222",
        "Мадагаскар" to "+261",
        "Македония" to "+389",
        "Малави" to "+265",
        "Малайзия" to "+60",
        "Мали" to "+223",
        "Мальдивы" to "+960",
        "Мальта" to "+356",
        "Марокко" to "+212",
        "Маршалловы Острова" to "+692",
        "Мексика" to "+52",
        "Мозамбик" to "+259",
        "Молдавия" to "+373",
        "Монако" to "+377",
        "Монголия" to "+976",
        "Мьянма" to "+95",
        "Намибия" to "+264",
        "Науру" to "+674",
        "Непал" to "+977",
        "Нигер" to "+227",
        "Нигерия" to "+234",
        "Нидерланды" to "+31",
        "Никарагуа" to "+505",
        "Новая Зеландия" to "+64",
        "Норвегия" to "+47",
        "Объединенные Арабские Эмираты" to "+971",
        "Оман" to "+968",
        "Пакистан" to "+92",
        "Палау" to "+680",
        "Панама" to "+507",
        "Папуа — Новая Гвинея" to "+675",
        "Парагвай" to "+595",
        "Перу" to "+51",
        "Польша" to "+48",
        "Португалия" to "+351",
        "Россия" to "+7",
        "Руанда" to "+250",
        "Румыния" to "+40",
        "Сальвадор" to "+503",
        "Самоа" to "+685",
        "Сан-Марино" to "+378",
        "Сан-Томе и Принсипи" to "+239",
        "Саудовская Аравия" to "+966",
        "Свазиленд" to "+268",
        "Северная Корея" to "+850",
        "Сейшелы" to "+248",
        "Сенегал" to "+221",
        "Сент-Винсент и Гренадины" to "+1784",
        "Сент-Китс и Невис" to "+1869",
        "Сент-Люсия" to "+1758",
        "Сербия" to "+381",
        "Сингапур" to "+65",
        "Сирия" to "+963",
        "Словакия" to "+421",
        "Словения" to "+986",
        "Соединенные Штаты Америки" to "+1",
        "Соломоновы Острова" to "+677",
        "Сомали" to "+252",
        "Судан" to "+249",
        "Суринам" to "+597",
        "Сьерра-Леоне" to "+232",
        "Таджикистан" to "+992",
        "Таиланд" to "+66",
        "Танзания" to "+255",
        "Того" to "+228",
        "Тонга" to "+676",
        "Тринидад и Тобаго" to "+1868",
        "Тувалу" to "+688",
        "Тунис" to "+216",
        "Туркмения" to "+993",
        "Турция" to "+90",
        "Уганда" to "+256",
        "Узбекистан" to "+998",
        "Украина" to "+380",
        "Уругвай" to "+598",
        "Федеративные штаты Микронезии" to "+691",
        "Фиджи" to "+679",
        "Филиппины" to "+63",
        "Финляндия" to "+358",
        "Франция" to "+33",
        "Хорватия" to "+385",
        "Центрально-Африканская Республика" to "+236",
        "Чад" to "+235",
        "Черногория" to "+381",
        "Чехия" to "+420",
        "Чили" to "+56",
        "Швейцария" to "+41",
        "Швеция" to "+46",
        "Шри-Ланка" to "+94",
        "Эквадор" to "+593",
        "Экваториальная Гвинея" to "+240",
        "Эритрея" to "+291",
        "Эстония" to "+372",
        "Эфиопия" to "+251",
        "Южная Корея" to "+82",
        "Южно-Африканская Республика" to "+27",
        "Ямайка" to "+1876",
        "Япония" to "+81"
    )

    fun formatPhoneNumber(phoneNumber: String): Pair<String, String> {
        var formattedNumber = phoneNumber.trim()
        var country = "Неизвестная страна"

        if (formattedNumber.length == 10 && formattedNumber.startsWith("9")) {
            country = "Россия"
        } else if (formattedNumber.startsWith("8") || formattedNumber.startsWith("79")) {
            country = "Россия"
            formattedNumber = formattedNumber.removePrefix("8")
        } else if (formattedNumber.startsWith("+")) {
            for ((countryName, code) in countryCodes) {
                if (formattedNumber.startsWith(code)) {
                    country = countryName
                    formattedNumber = formattedNumber.removePrefix(code)
                    break
                }
            }
        }
        return Pair(formattedNumber, country)
    }

    fun searchPhone(phone: String): String {
        val (formattedPhoneNumber, country) = formatPhoneNumber(phone)
        var result :  String = ""

        if (formattedPhoneNumber.length <= 3) {
           Log.d ("searchPhone","Неверный номер телефона")
        }

        if (country == "Россия") {
            val kod = formattedPhoneNumber.take(3)
            val number = try {
                formattedPhoneNumber.drop(3).toInt()
            } catch (e: NumberFormatException) {
                Log.d ("searchPhone","Номер телефона должен быть числом")
            }
            try {
                val cursor = readableDatabase.rawQuery("SELECT * FROM phones WHERE Код = $kod AND $number >= От AND $number <= До", null)
                while (cursor.moveToNext()) {
                    var region = cursor.getString(cursor.getColumnIndexOrThrow("Регион"))
                    var formattedRegion = region

                    if (formattedRegion.contains("|")) {
                        formattedRegion = formattedRegion.split("|")[0] // Оставляем только часть до символа "|"
                    }
                    if (formattedRegion.contains("*")) {
                        formattedRegion = formattedRegion.split("*")[0] // Оставляем только часть до символа "*"
                    }
                    if (formattedRegion.contains("область" )) {
                        formattedRegion = formattedRegion.replace("область", "обл.")
                    }

                    val resultMap = mapOf(
                        "Телефон" to phone,
                        "Код" to kod,
                        "Номер" to number,
                        "Регион" to formattedRegion
                    )
                    result = formattedRegion
                    Log.d("DatabaseLog", "Result: $resultMap")
                    return result

                }
                cursor.close()
            } catch (e: SQLException) {
                Log.e("DatabaseHelper", "Error querying phone number: ${e.message}", e)
            }

        } else {
            val resultMap = mapOf(
                "Телефон" to phone,
                "Страна" to country
            )
            result = country
            Log.d("StartLog", "Result: $resultMap")
            return result
        }
        return result

    }

}

