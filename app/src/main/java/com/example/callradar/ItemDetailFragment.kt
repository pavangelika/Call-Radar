package com.example.callradar

import FormatPhoneNumber
import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.callradar.datamodels.CallDetail
import com.example.callradar.callog.CallLogDataHelper
import com.example.callradar.datamodels.ContactInfo
import com.example.callradar.datamodels.PhoneNumberInfo
import com.example.callradar.databinding.FragmentItemDetailBinding
import com.example.callradar.utils.GetRegionFromNumber

class ItemDetailFragment : Fragment() {
    private var item: CallDetail? = null
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper: GetRegionFromNumber
    private lateinit var favoriteIcon: ImageView
    private var isFavorite: Boolean = false

    private val dragListener = View.OnDragListener { _, event ->
        if (event.action == DragEvent.ACTION_DROP) {
            val clipDataItem: ClipData.Item = event.clipData.getItemAt(0)
            val dragData = clipDataItem.text
            item = CallLogDataHelper.ITEM_MAP[dragData.toString()]
            updateContent()
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper = GetRegionFromNumber(requireContext())
        CallLogDataHelper.initializeItems(requireContext())

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                val itemId = it.getString(ARG_ITEM_ID)
                val allNumbers = it.getStringArrayList(ARG_ALL_NUMBERS) ?: listOf(itemId)
                val detailsByNumber = allNumbers.mapNotNull { number ->
                    CallLogDataHelper.ITEM_MAP[number]?.details
                }.flatten()

                item = if (detailsByNumber.isNotEmpty()) {
                    CallDetail(
                        number = itemId ?: "",
                        contactName = CallLogDataHelper.ITEM_MAP[itemId]?.contactName
                            ?: "Неизвестный",
                        allPhoneNumbers = allNumbers,
                        details = detailsByNumber
                    )
                } else null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContent()

        binding.detailToolbar?.let { toolbar ->
            favoriteIcon = toolbar.findViewById(R.id.favorite_icon)

            item?.let { callDetail ->
                isFavorite = CallLogDataHelper.isContactStarred(requireContext(), callDetail.number)
                updateFavoriteIcon()

                favoriteIcon.setOnClickListener {
                    if (!CallLogDataHelper.hasContactsPermissions(requireContext())) {
                        requestContactsPermissions()
                        return@setOnClickListener
                    }
                    item?.number?.let { phoneNumber ->
                        toggleFavoriteStatus(phoneNumber)
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        favoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        favoriteIcon.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (isFavorite) R.color.missed_call_primary else R.color.text_primary
            )
        )
    }

    private fun updateContent() {
        item?.let { callDetail ->
            val contactInfo = if (callDetail.contactName != "Неизвестный") {
                // Для известных контактов получаем полную информацию
                val contactID =
                    CallLogDataHelper.findContactIdByName(requireContext(), callDetail.contactName)
                CallLogDataHelper.readFullContactInfo(requireContext(), contactID!!)
            } else {
                // Для неизвестных создаем минимальную ContactInfo
                ContactInfo.empty("").copy(
                    displayName = "Неизвестный",
                    phoneNumbers = listOf(
                        PhoneNumberInfo(
                            number = normalizePhoneNumber(callDetail.number),
                            type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                    )
                )
            }

            updateContactInfo(contactInfo)
            updateCallLogs(callDetail)
        } ?: run {
            Log.e("ItemDetailFragment", "Call detail item is null")
            binding.callLogsContainer?.removeAllViews()
            binding.toolbarLayout?.title = ""
        }
    }

    private fun updateContactInfo(contactInfo: ContactInfo) {
        try {
            Log.d("ContactInfo", "Данные контакта: " +
                    "Имя='${contactInfo.displayName}', " +
                    "Имя из журнала='${item?.contactName}', " +
                    "Номера в контакте=${contactInfo.phoneNumbers?.size ?: 0}, " +
                    "Номера в журнале=${item?.allPhoneNumbers?.size ?: 0}")

            if (item?.contactName != "Неизвестный") {
                Log.d("ContactInfo", "ID: ${contactInfo.id}")
                Log.d("ContactInfo", "displayName: ${contactInfo.displayName}")
                Log.d("ContactInfo", "firstName: ${contactInfo.firstName}")
                Log.d("ContactInfo", "lastName: ${contactInfo.lastName}")
                Log.d("ContactInfo", "middleName: ${contactInfo.middleName}")
                Log.d("ContactInfo", "nickname: ${contactInfo.nickname}")
                Log.d("ContactInfo", "organization: ${contactInfo.organization}")
                Log.d("ContactInfo", "notes: ${contactInfo.notes}")
                Log.d("ContactInfo", "birthday: ${contactInfo.birthday}")
                Log.d("ContactInfo", "ringtoneUri: ${contactInfo.ringtoneUri}")
                Log.d("ContactInfo", "photoUri: ${contactInfo.photoUri}")
                Log.d(
                    "ContactInfo",
                    "groups: ${contactInfo.groups.size} groups: ${contactInfo.groups}"
                )
                Log.d(
                    "ContactInfo",
                    "phone numbers: ${contactInfo.phoneNumbers.size} phone numbers: ${contactInfo.phoneNumbers}\""
                )
                Log.d(
                    "ContactInfo",
                    "socialNetworks: ${contactInfo.socialNetworks.size} socialNetworks: ${contactInfo.socialNetworks}"
                )
                Log.d(
                    "ContactInfo",
                    "emails: ${contactInfo.emails.size} emails: ${contactInfo.emails}\""
                )
                Log.d(
                    "ContactInfo",
                    "addresses: ${contactInfo.addresses.size} emails: ${contactInfo.addresses}\""
                )
                Log.d("ContactInfo", "starred: ${contactInfo.starred}\"")
            }

// Проверка на доступность Fragment и binding
            if (!isAdded || context == null || _binding == null) return

            val searchResult = helper.searchPhone(item!!.number)
            val isCityCall = searchResult.startsWith("г.") &&
                    (!searchResult.contains("обл.") && !searchResult.contains("АО") && !searchResult.contains(
                        "округ"
                    ))

            // 1. Получаем номера в порядке приоритета

// Модифицируйте получение номеров:
            val numbersToShow = when {
                !contactInfo.phoneNumbers.isNullOrEmpty() -> {
                    contactInfo.phoneNumbers.mapNotNull {
                        normalizePhoneNumber(it.number) // Нормализуем номера из контакта
                    }
                }

                !item?.allPhoneNumbers.isNullOrEmpty() -> {
                    item!!.allPhoneNumbers.map {
                        normalizePhoneNumber(it) // Нормализуем номера из журнала
                    }
                }

                item?.number != null -> {
                    listOf(normalizePhoneNumber(item!!.number)) // Нормализуем основной номер
                }

                else -> {
                    Log.e("ContactInfo", "Не найдено ни одного номера")
                    return
                }
            }.distinct()

            Log.d(
                "ContactInfo",
                "Номера для отображения (${numbersToShow.size}): ${numbersToShow.joinToString()}"
            )

// 2. Определяем основной номер для отображения
            val mainNumber = when {
                !contactInfo.phoneNumbers.isNullOrEmpty() -> contactInfo.phoneNumbers.first().number
                !item?.allPhoneNumbers.isNullOrEmpty() -> item!!.allPhoneNumbers.first()
                item?.number != null -> item!!.number
                else -> null
            }
            Log.d("ContactInfo", "MainNumber - ${mainNumber.toString()}")
// 3. Определяем отображаемое имя в заголовке
            val displayName = when {
                item?.contactName == "Неизвестный" -> FormatPhoneNumber.formatPhoneNumber(
                    mainNumber.toString(),
                    isCityCall
                )

                else -> contactInfo.displayName
            }

            binding.toolbarLayout?.title = displayName
            Log.d("ContactInfo", displayName)

// 4. Отображаем блок с номерами телефонов
            binding.callLogsContainer?.apply {
                findViewWithTag<View?>("contact_info_tag")?.let { removeView(it) }

                val infoContainer = layoutInflater.inflate(
                    R.layout.detail_contact_info_block, this, false
                ).apply { tag = "contact_info_tag" }

                val numbersContainer =
                    infoContainer.findViewById<LinearLayout>(R.id.numbers_container)
                        ?: return@apply

                numbersContainer.removeAllViews()

                numbersToShow.forEachIndexed { index, number ->
                    Log.d("ContactInfo", "numbersToShow ${number}")
                    val searchPlace = helper.searchPhone(item!!.number)
                    try {

                        val numberItem = layoutInflater.inflate(
                            R.layout.detail_phone_number, numbersContainer, false
                        ).apply {
                            findViewById<TextView>(R.id.phone_number)?.text =
                                FormatPhoneNumber.formatPhoneNumber(number, isCityCall)

                            findViewById<TextView>(R.id.call_source)?.text = when {
                                helper.searchPhone(number).startsWith("г.") -> "(городской)"
                                else -> "(мобильный)"
                            }

                            findViewById<TextView>(R.id.place)?.text = searchPlace


                            findViewById<ImageButton>(R.id.call_button)?.setOnClickListener {
                                makeCall(number)
                            }

                            findViewById<ImageButton>(R.id.sms_button)?.setOnClickListener {
                                sendSms(number)
                            }
                        }
                        numbersContainer.addView(numberItem)
                    } catch (e: Exception) {
                        Log.e("ContactInfo", "Ошибка создания элемента для $number", e)

                    }
                }

                if (numbersContainer.childCount > 0) {
                    addView(infoContainer)  // unknown numbers
                } else {
                    val searchPlace = helper.searchPhone(item!!.number)
                    Log.w("ContactInfo", "Не удалось найти номера")
                    // Fallback вариант
                    val fallbackNumber = numbersToShow.firstOrNull() ?: return@apply
                    val fallbackItem = layoutInflater.inflate(
                        R.layout.detail_phone_number, numbersContainer, false
                    ).apply {
                        findViewById<TextView>(R.id.phone_number)?.text =
                            FormatPhoneNumber.formatPhoneNumber(fallbackNumber, isCityCall)
                        findViewById<TextView>(R.id.call_source)?.text = "(основной)"
//                        findViewById<TextView>(R.id.call_source)?.text = when {
//                            searchPlace.startsWith("г.") -> "(городской)"
//                            else -> "(мобильный)"
//                        }
                        findViewById<TextView>(R.id.place)?.text = searchPlace
                    }
                    numbersContainer.addView(fallbackItem)
                    addView(infoContainer)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactInfo", "Ошибка при обновлении контакта", e)
            Toast.makeText(context, "Ошибка загрузки контакта", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCallLogs(callDetail: CallDetail) {
        val searchResult = helper.searchPhone(callDetail.number)
        val isCityCall = searchResult.startsWith("г.") &&
                (!searchResult.contains("обл.") && !searchResult.contains("АО") && !searchResult.contains(
                    "округ"
                ))

        binding.callLogsContainer?.findViewWithTag<View>("call_logs_tag")?.let {
            binding.callLogsContainer?.removeView(it)
        }

        val logsContainer = LinearLayout(context).apply {
            tag = "call_logs_tag"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val journalHeader = TextView(context).apply {
            text = "Журнал звонков"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(16, 16.dpToPx(), 8, 16.dpToPx())
        }
        logsContainer.addView(journalHeader)

        val numbersToShow = if (callDetail.contactName != "Неизвестный") {
            callDetail.allPhoneNumbers.distinct()
        } else {
            listOf(callDetail.number)
        }

        numbersToShow.forEach { number ->
            val headerView = layoutInflater.inflate(
                R.layout.detail_calllog_header,
                logsContainer,
                false
            ).apply {
                findViewById<TextView>(R.id.phone_number).text =
                    FormatPhoneNumber.formatPhoneNumber(number, isCityCall)

                val headerContainer = findViewById<LinearLayout>(R.id.header_container)
                val expandIcon = findViewById<ImageView>(R.id.expand_icon)
                val detailsContainer = findViewById<LinearLayout>(R.id.call_details_container)

                var isExpanded = true

                findViewById<TextView>(R.id.phone_number).setOnLongClickListener {
                    showDeleteCallLogDialog(number)
                    true
                }

                headerContainer.setOnClickListener {
                    isExpanded = !isExpanded
                    if (isExpanded) {
                        detailsContainer.visibility = View.VISIBLE
                        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                        detailsContainer.startAnimation(animation)
                        expandIcon.animate()
                            .rotation(0f)
                            .setDuration(300)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    } else {
                        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                        animation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationRepeat(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                detailsContainer.visibility = View.GONE
                            }
                        })
                        detailsContainer.startAnimation(animation)
                        expandIcon.animate()
                            .rotation(180f)
                            .setDuration(300)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                }

                callDetail.details
                    .filter { it.number == number }
                    .sortedByDescending { it.date }
                    .forEach { detail ->
                        val callItemView = layoutInflater.inflate(
                            R.layout.detail_callog,
                            detailsContainer,
                            false
                        ).apply {
                            findViewById<ImageView>(R.id.call_type_icon).setImageDrawable(
                                CallLogDataHelper.getCallTypeIcon(context, detail.type)
                            )

                            val durationText = when {
                                detail.duration >= 3600 -> "${detail.duration / 3600} ч ${(detail.duration % 3600) / 60} мин"
                                detail.duration >= 60 -> "${detail.duration / 60} мин"
                                else -> "${detail.duration} сек"
                            }

                            findViewById<TextView>(R.id.call_date).text = detail.dateString
                            findViewById<TextView>(R.id.call_time).text = detail.timeString
                            findViewById<TextView>(R.id.call_duration).text = durationText
                        }
                        detailsContainer.addView(callItemView)
                    }
            }
            logsContainer.addView(headerView)
        }

        binding.callLogsContainer?.addView(logsContainer)
    }

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun makeCall(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION
            )
        }
    }

    private fun sendSms(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть приложение сообщений", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestContactsPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS
            ),
            REQUEST_CONTACTS_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_WRITE_CALL_LOG_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    item?.let { callDetail ->
                        callDetail.allPhoneNumbers.firstOrNull()?.let { number ->
                            deleteCallLogForNumber(number)
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Для удаления звонков необходимо разрешение",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            REQUEST_CONTACTS_PERMISSION -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    item?.let { callDetail ->
                        CallLogDataHelper.setContactStarred(
                            requireContext(),
                            callDetail.number,
                            isFavorite
                        )
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Разрешения не предоставлены, изменения не сохранены в контактах",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDeleteCallLogDialog(number: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить журнал звонков")
            .setMessage(
                "Вы уверены, что хотите удалить все звонки для номера ${
                    FormatPhoneNumber.formatPhoneNumber(
                        number,
                        false
                    )
                }?"
            )
            .setPositiveButton("Удалить") { _, _ ->
                deleteCallLogForNumber(number)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleFavoriteStatus(phoneNumber: String) {
        isFavorite = !isFavorite
        updateFavoriteIcon()

        val success = CallLogDataHelper.setContactStarred(
            requireContext(),
            phoneNumber,
            isFavorite
        )

        if (!success) {
            Toast.makeText(
                context,
                "Не удалось обновить контакт",
                Toast.LENGTH_SHORT
            ).show()
            isFavorite = !isFavorite
            updateFavoriteIcon()
        } else {
            favoriteIcon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
                .withEndAction {
                    favoriteIcon.animate().scaleX(1f).scaleY(1f).setDuration(200)
                        .start()
                }.start()
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace("[^0-9+]".toRegex(), "")
            .replace("^8".toRegex(), "8") // Российские номера
            .replace("^7".toRegex(), "+7") // Уже международный
    }

    private fun deleteCallLogForNumber(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_CALL_LOG),
                REQUEST_WRITE_CALL_LOG_PERMISSION
            )
            return
        }

        val deletedRows = CallLogDataHelper.deleteCallLogForNumber(requireContext(), number)
        if (deletedRows > 0) {
            CallLogDataHelper.initializeItems(requireContext())
            val updatedItem = CallLogDataHelper.ITEM_MAP[number]
                ?: CallLogDataHelper.ITEM_MAP.values.firstOrNull {
                    it.allPhoneNumbers.contains(
                        number
                    )
                }
            item = updatedItem
            updateContent()
            Toast.makeText(context, "Журнал звонков очищен", Toast.LENGTH_SHORT).show()

            // как перейти обратно в itemlistfragment

        } else {
            Toast.makeText(context, "Не удалось удалить записи", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val ARG_ALL_NUMBERS = "all_numbers"
        private const val REQUEST_CALL_PHONE_PERMISSION = 101
        const val REQUEST_CONTACTS_PERMISSION = 102
        const val REQUEST_WRITE_CALL_LOG_PERMISSION = 103
    }
}