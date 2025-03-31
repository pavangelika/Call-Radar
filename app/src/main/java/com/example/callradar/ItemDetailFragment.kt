package com.example.callradar

import android.Manifest
import com.example.callradar.utils.GetRegionFromNumber
import android.content.ClipData
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

    /**
     * The call log entry this fragment is presenting.
     */
    private var item: CallDetail? = null
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper: GetRegionFromNumber

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

                // Получаем детали для каждого номера отдельно
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
                } else {
                    null
                }
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
    }

    private fun updateContent() {
        item?.let { callDetail ->
            val searchResult = helper.searchPhone(callDetail.number)
            val isCityCall = searchResult.startsWith("г.") &&
                    (!searchResult.contains("обл.") && !searchResult.contains("АО") && !searchResult.contains("округ"))
            if (callDetail.contactName != "Неизвестный") {
                binding.toolbarLayout?.title = callDetail.contactName
            } else {
                binding.toolbarLayout?.title = FormatPhoneNumber.formatPhoneNumber(callDetail.number, isCityCall)
            }

            binding.callLogsContainer?.removeAllViews()

            // Добавляем информацию о контакте
            val infoContainer = layoutInflater.inflate(
                R.layout.item_contact_info_block,
                binding.callLogsContainer,
                false
            )

            val numbersContainer = infoContainer.findViewById<LinearLayout>(R.id.numbers_container)

            // Добавляем каждый номер с кнопками
            callDetail.allPhoneNumbers.distinct().forEach { number ->
                val searchResult = helper.searchPhone(number)
                val callType = when {
                    searchResult.startsWith("г.") &&
                            !searchResult.contains("обл.") &&
                            !searchResult.contains("АО") &&
                            !searchResult.contains("округ") -> "(городской)"
                    searchResult.contains("обл.") ||
                            searchResult.contains("АО") ||
                            searchResult.contains("округ") -> "(мобильный)"
                    else -> ""
                }

                val numberItem = layoutInflater.inflate(
                    R.layout.item_phone_number_simple,
                    numbersContainer,
                    false
                ).apply {
                    findViewById<TextView>(R.id.phone_number).text = FormatPhoneNumber.formatPhoneNumber(number, isCityCall)
                    findViewById<TextView>(R.id.call_source).text = "$callType"
                    findViewById<TextView>(R.id.place).text = "$searchResult"

                    findViewById<ImageButton>(R.id.call_button).setOnClickListener {
                        makeCall(number)
                    }
                    findViewById<ImageButton>(R.id.sms_button).setOnClickListener {
                        sendSms(number)
                    }
                }
                numbersContainer.addView(numberItem)
            }
            binding.callLogsContainer?.addView(infoContainer)

            // Добавляем журнал звонков
            val journalHeader = TextView(context).apply {
                text = "Журнал звонков"
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
            }
            binding.callLogsContainer?.addView(journalHeader)

            // Получаем все уникальные номера для контакта
            val numbersToShow = if (callDetail.contactName != "Неизвестный") {
                callDetail.allPhoneNumbers.distinct()
            } else {
                listOf(callDetail.number)
            }

            // Для каждого номера контакта создаем отдельную секцию
            numbersToShow.forEach { number ->
                // Заголовок с номером телефона
                val numberHeader = TextView(context).apply {
                    text = FormatPhoneNumber.formatPhoneNumber(number, isCityCall)
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, 16.dpToPx(), 0, 8.dpToPx())
                }
                binding.callLogsContainer?.addView(numberHeader)

                // Фильтруем звонки по текущему номеру
                val callsForNumber = callDetail.details.filter { it.number == number }

                // Добавляем все звонки для этого номера
                callsForNumber.sortedByDescending { it.date }.forEach { detail ->
                    val callItemView = layoutInflater.inflate(
                        R.layout.item_call_log_detail,
                        binding.callLogsContainer,
                        false
                    )

                    // Заполняем данные звонка
                    callItemView.findViewById<ImageView>(R.id.call_type_icon).apply {
                        setImageDrawable(CallLogDataHelper.getCallTypeIcon(context, detail.type))
                    }

                    val durationText = when {
                        detail.duration >= 3600 -> "${detail.duration / 3600} ч ${(detail.duration % 3600) / 60} мин"
                        detail.duration >= 60 -> "${detail.duration / 60} мин"
                        else -> "${detail.duration} сек"
                    }

                    callItemView.findViewById<TextView>(R.id.call_date).text = detail.date
                    callItemView.findViewById<TextView>(R.id.call_time).text = detail.time
                    callItemView.findViewById<TextView>(R.id.call_duration).text = durationText

                    binding.callLogsContainer?.addView(callItemView)
                }
            }
        } ?: run {
            Log.e("ItemDetailFragment", "Call detail item is null")
        }
    }

    // Эти методы должны быть на уровне класса, а не внутри updateContent()

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun makeCall(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION
            )
        }
    }

    private fun sendSms(number: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть приложение сообщений", Toast.LENGTH_SHORT).show()
        }
    }
    companion object {
        /**
         * The fragment argument representing the call log ID that this fragment represents.
         */
        const val ARG_ITEM_ID = "item_id"
        const val ARG_ALL_NUMBERS = "all_numbers"
        private const val REQUEST_CALL_PHONE_PERMISSION = 101
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
