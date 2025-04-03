package com.example.callradar

import android.Manifest
import android.app.AlertDialog
import com.example.callradar.utils.GetRegionFromNumber
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import androidx.fragment.app.Fragment
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
import com.example.callradar.callog.CallDetail
import com.example.callradar.databinding.FragmentItemDetailBinding
import com.example.callradar.callog.CallLogDataHelper

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {
    private var item: CallDetail? = null
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper: GetRegionFromNumber
    private lateinit var favoriteIcon: ImageView
    private var isFavorite: Boolean = false // Добавлено объявление переменной

    private val dragListener = View.OnDragListener { v, event ->
        if (event.action == DragEvent.ACTION_DROP) {
            val clipDataItem: ClipData.Item = event.clipData.getItemAt(0)
            val dragData = clipDataItem.text
            item = CallLogDataHelper.ITEM_MAP[dragData]
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
                        contactName = CallLogDataHelper.ITEM_MAP[itemId]?.contactName ?: "Неизвестный",
                        allPhoneNumbers = allNumbers,
                        details = detailsByNumber
                    )
                } else null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateContent()

        // Безопасный доступ к Toolbar через binding
        binding.detailToolbar?.let { toolbar ->
            favoriteIcon = toolbar.findViewById(R.id.favorite_icon)

            item?.let { callDetail ->
                // Проверяем статус избранного в системной адресной книге
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


    private fun isContactFavorite(phoneNumber: String): Boolean {
        val prefs = requireContext().getSharedPreferences("favorites", Context.MODE_PRIVATE)
        return prefs.getBoolean(phoneNumber, false)
    }

    private fun updateContent() {
        item?.let { callDetail ->
            // Обновляем контактную информацию (все номера)
            updateContactInfo(callDetail)

            // Обновляем журнал звонков (только с существующими записями)
            updateCallLogs(callDetail.copy(
                details = callDetail.details.filter { detail ->
                    CallLogDataHelper.ITEM_MAP[detail.number]?.details?.any {
                        it.date == detail.date && it.type == detail.type
                    } == true
                }
            ))
        } ?: run {
            Log.e("ItemDetailFragment", "Call detail item is null")
            binding.callLogsContainer?.removeAllViews()
            binding.toolbarLayout?.title = ""
        }
    }

    private fun updateContactInfo(callDetail: CallDetail) {
        // Определяем тип основного номера (для форматирования)
        val mainNumberSearchResult = helper.searchPhone(callDetail.number)
        val isMainNumberCityCall = mainNumberSearchResult.startsWith("г.") &&
                (!mainNumberSearchResult.contains("обл.") &&
                        !mainNumberSearchResult.contains("АО") &&
                        !mainNumberSearchResult.contains("округ"))

        // Обновляем заголовок
        binding.toolbarLayout?.title = if (callDetail.contactName != "Неизвестный") {
            callDetail.contactName
        } else {
            FormatPhoneNumber.formatPhoneNumber(callDetail.number, isMainNumberCityCall)
        }

        // Удаляем предыдущую контактную информацию
        binding.callLogsContainer?.findViewWithTag<View>("contact_info_tag")?.let {
            binding.callLogsContainer?.removeView(it)
        }

        // Создаем контейнер для контактной информации
        val infoContainer = layoutInflater.inflate(
            R.layout.detail_contact_info_block,
            binding.callLogsContainer,
            false
        ).apply {
            tag = "contact_info_tag"
        }

        val numbersContainer = infoContainer.findViewById<LinearLayout>(R.id.numbers_container)
        val showAllButton = infoContainer.findViewById<TextView>(R.id.show_all_numbers_button)
        numbersContainer.removeAllViews()
        // Показываем кнопку, если номеров больше 3
        if (callDetail.allPhoneNumbers.size > 3) {
            showAllButton.visibility = View.VISIBLE
            showAllButton.setOnClickListener {
                // Реализация разворачивания/сворачивания
            }
        }

        // Отображаем все номера контакта с правильным форматированием для каждого
        callDetail.allPhoneNumbers.distinct().forEach { number ->
            val numberSearchResult = helper.searchPhone(number)
            val isCurrentNumberCityCall = numberSearchResult.startsWith("г.") &&
                    !numberSearchResult.contains("обл.") &&
                    !numberSearchResult.contains("АО") &&
                    !numberSearchResult.contains("округ")

            val callType = when {
                isCurrentNumberCityCall -> "(городской)"
                numberSearchResult.contains("обл.") ||
                        numberSearchResult.contains("АО") ||
                        numberSearchResult.contains("округ") -> "(мобильный)"
                else -> ""
            }

            val numberItem = layoutInflater.inflate(R.layout.detail_phone_number, numbersContainer, false).apply {
                // Форматируем каждый номер согласно его типу (городской/мобильный)
                val phoneNumberView = findViewById<TextView>(R.id.phone_number)
                phoneNumberView.text = FormatPhoneNumber.formatPhoneNumber(number, isCurrentNumberCityCall)

                findViewById<TextView>(R.id.call_source).text = callType
                findViewById<TextView>(R.id.place).text = numberSearchResult

                // Получаем ссылки на кнопки (предполагая, что они есть в layout)
                val callButton = findViewById<ImageButton>(R.id.call_button)
                val smsButton = findViewById<ImageButton>(R.id.sms_button)

                // Устанавливаем обработчики кликов
                callButton.setOnClickListener {
                    makeCall(number)
                }

                smsButton.setOnClickListener {
                    sendSms(number)
                }

                // Подсвечиваем основной номер (если их несколько)
                if (number == callDetail.number && callDetail.allPhoneNumbers.size > 1) {
                    phoneNumberView.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                    findViewById<TextView>(R.id.call_source).setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary)
                    )
                }
            }
            numbersContainer.addView(numberItem)
        }
        binding.callLogsContainer?.addView(infoContainer)
    }

    private fun updateCallLogs(callDetail: CallDetail) {
        val searchResult = helper.searchPhone(callDetail.number)
        val isCityCall = searchResult.startsWith("г.") &&
                (!searchResult.contains("обл.") && !searchResult.contains("АО") && !searchResult.contains("округ"))

        // Удаляем предыдущий журнал звонков, если он есть
        binding.callLogsContainer?.findViewWithTag<View>("call_logs_tag")?.let {
            binding.callLogsContainer?.removeView(it)
        }

        // Создаем контейнер для журнала звонков
        val logsContainer = LinearLayout(context).apply {
            tag = "call_logs_tag"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Добавляем заголовок
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
                R.layout.item_call_log_header,
                logsContainer,
                false
            ).apply {
                findViewById<TextView>(R.id.phone_number).text =
                    FormatPhoneNumber.formatPhoneNumber(number, isCityCall)

                val headerContainer = findViewById<LinearLayout>(R.id.header_container)
                val expandIcon = findViewById<ImageView>(R.id.expand_icon)
                val detailsContainer = findViewById<LinearLayout>(R.id.call_details_container)

                var isExpanded = true

                // Добавляем обработчик долгого нажатия на номер
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

                // Отображаем только существующие записи звонков
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
                                CallLogDataHelper.getCallTypeIcon(context, detail.type))

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

        // Добавляем журнал звонков после контактной информации
        binding.callLogsContainer?.addView(logsContainer)
    }


    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun makeCall(number: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } else {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE_PERMISSION)
        }
    }

    private fun sendSms(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть приложение сообщений", Toast.LENGTH_SHORT).show()
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
            else -> {
                // Обработка других permission request, если нужно
            }
        }
    }

    private fun showDeleteCallLogDialog(number: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить журнал звонков")
            .setMessage("Вы уверены, что хотите удалить все звонки для номера ${FormatPhoneNumber.formatPhoneNumber(number, false)}?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteCallLogForNumber(number)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleFavoriteStatus(phoneNumber: String) {
        // Инвертируем текущее состояние
        isFavorite = !isFavorite

        // Обновляем иконку
        updateFavoriteIcon()

        // Пытаемся сохранить в системных контактах
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
            // Откатываем изменения, если не удалось сохранить
            isFavorite = !isFavorite
            updateFavoriteIcon()
        } else {
            // Анимация при успешном сохранении
            favoriteIcon.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
                .withEndAction {
                    favoriteIcon.animate().scaleX(1f).scaleY(1f).setDuration(200)
                        .start()
                }.start()
        }
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
            // Полностью перезагружаем данные
            CallLogDataHelper.initializeItems(requireContext())

            // Обновляем текущий элемент
            val updatedItem = CallLogDataHelper.ITEM_MAP[number] ?: CallLogDataHelper.ITEM_MAP.values.firstOrNull { it.allPhoneNumbers.contains(number) }
            item = updatedItem

            // Полностью обновляем контент
            updateContent()

            Toast.makeText(context, "Журнал звонков очищен", Toast.LENGTH_SHORT).show()
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


