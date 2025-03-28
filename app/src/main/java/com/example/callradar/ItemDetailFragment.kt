package com.example.callradar

import android.Manifest
import DatabaseHelper
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.DragEvent
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.callradar.calls.CallDetail
import com.example.callradar.databinding.FragmentItemDetailBinding
import com.example.callradar.calls.CallLogHelper

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
    private lateinit var helper: DatabaseHelper

    private val dragListener = View.OnDragListener { v, event ->
        if (event.action == DragEvent.ACTION_DROP) {
            val clipDataItem: ClipData.Item = event.clipData.getItemAt(0)
            val dragData = clipDataItem.text
            item = CallLogHelper.ITEM_MAP[dragData]
            updateContent()
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper = DatabaseHelper(requireContext()) // Инициализация здесь
        CallLogHelper.initializeItems(requireContext())

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                val itemId = it.getString(ARG_ITEM_ID)
                item = CallLogHelper.ITEM_MAP[itemId]
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
//        item?.let { callDetail ->
//            val info = helper.searchPhone(callDetail.number)
//            val callType = if (info.contains("г.") && !info.contains("область", ignoreCase = true)) {
//                "Городской номер"
//            } else {
//                "Мобильный номер"
//            }
        item?.let { callDetail ->
            binding.toolbarLayout?.title = when {
                callDetail.contactName != "Неизвестный" -> callDetail.contactName
                else -> callDetail.number
            }

            binding.callLogsContainer?.removeAllViews()

            val infoContainer = layoutInflater.inflate(
                R.layout.item_contact_info_block,
                binding.callLogsContainer,
                false
            )

            infoContainer.findViewById<TextView>(R.id.contact_name).text =
                if (callDetail.contactName != "Неизвестный") callDetail.contactName else "Неизвестный номер"

            val numbersContainer = infoContainer.findViewById<LinearLayout>(R.id.numbers_container)

            val numbersToShow = if (callDetail.contactName != "Неизвестный") {
                callDetail.allPhoneNumbers.distinct()
            } else {
                listOf(callDetail.number)
            }

            numbersToShow.forEach { number ->
                val numberItem = layoutInflater.inflate(
                    R.layout.item_phone_number_simple,
                    numbersContainer,
                    false
                ).apply {
                    findViewById<TextView>(R.id.phone_number).text = formatPhoneNumber(number)

                    findViewById<ImageButton>(R.id.call_button).setOnClickListener {
                        makeCall(number)
                    }

                    findViewById<ImageButton>(R.id.sms_button).setOnClickListener {
                        item?.let { sendSms(it.number) }
                    }
                }
                numbersContainer.addView(numberItem)
            }

            // Добавляем блок информации перед журналом звонков
            binding.callLogsContainer?.addView(infoContainer)

            // Добавляем заголовок журнала вызовов
//            val header = TextView(context).apply {
//                text = "Журнал вызовов"
//                textSize = 18f
//                setTypeface(typeface, Typeface.BOLD)
//                setPadding(0, 16.dpToPx(), 0, 8.dpToPx())
//            }
//            binding.callLogsContainer?.addView(header)



            // Добавляем записи о звонках
            callDetail.details.sortedByDescending { it.date }.forEach { detail ->
                val callItemView = layoutInflater.inflate(
                    R.layout.item_call_log_detail,
                    binding.callLogsContainer,
                    false
                )

                // Устанавливаем иконку типа звонка
                callItemView.findViewById<ImageView>(R.id.call_type_icon).apply {
                    setImageDrawable(CallLogHelper.getCallTypeIcon(context, detail.type))
                }

                // Форматируем длительность
                val durationText = when {
                    detail.duration >= 3600 -> {
                        val hours = detail.duration / 3600
                        val minutes = (detail.duration % 3600) / 60
                        val seconds = detail.duration % 60
                        "$hours ч $minutes мин $seconds сек"
                    }
                    detail.duration >= 60 -> {
                        val minutes = detail.duration / 60
                        val seconds = detail.duration % 60
                        "$minutes мин $seconds сек"
                    }
                    else -> "${detail.duration} сек"
                }

                // Заполняем данные звонка
                callItemView.findViewById<TextView>(R.id.call_date).text = detail.date
                callItemView.findViewById<TextView>(R.id.call_time).text = detail.time
                callItemView.findViewById<TextView>(R.id.call_duration).text = durationText

                binding.callLogsContainer?.addView(callItemView)
            }
        } ?: run {
            Log.e("ItemDetailFragment", "Call detail item is null")
//            binding.contactInfo?.text = "Ошибка загрузки данных"
//            binding.numberInfo?.text = ""
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        return try {
            // Упрощенное форматирование номера
            when {
                phone.length == 11 && phone.startsWith("8") ->
                    "+7 (${phone.substring(1, 4)}) ${phone.substring(4, 7)}-${phone.substring(7, 9)}-${phone.substring(9)}"
                phone.length == 11 && phone.startsWith("7") ->
                    "+7 (${phone.substring(1, 4)}) ${phone.substring(4, 7)}-${phone.substring(7, 9)}-${phone.substring(9)}"
                phone.length == 10 ->
                    "+7 (${phone.substring(0, 3)}) ${phone.substring(3, 6)}-${phone.substring(6, 8)}-${phone.substring(8)}"
                else -> phone
            }
        } catch (e: Exception) {
            phone
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun makeCall(number: String) {
        // Проверяем разрешение
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Создаем intent для прямого вызова
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            // Запрашиваем разрешение, если его нет
            requestPermissions(
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE_PERMISSION
            )
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CALL_PHONE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Повторяем вызов, если разрешение получено
                    item?.let { makeCall(it.number) }
                } else {
                    Toast.makeText(context, "Необходимо разрешение для совершения звонков", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun sendSms(number: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")  // Обратите внимание на "smsto:" вместо "sms:"
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
        private const val REQUEST_CALL_PHONE_PERMISSION = 101
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
