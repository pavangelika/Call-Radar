package com.example.callradar

import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

    private var itemDetailTextView: TextView? = null
    private var aboutNumbersTextView: TextView? = null
    private var toolbarLayout: CollapsingToolbarLayout? = null

    private var _binding: FragmentItemDetailBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

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

        // Инициализация данных CallLogHelper
        CallLogHelper.initializeItems(requireContext())


        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                val itemId = it.getString(ARG_ITEM_ID)
                Log.d("bundleLog", "ARG_ITEM_ID: $itemId") // Логируем значение ARG_ITEM_ID
                item = CallLogHelper.ITEM_MAP[itemId]
            } else {
                Log.d("bundleLog", "ARG_ITEM_ID not found in arguments")
            }
        }
        Log.d("bundleLog", "ITEM_MAP keys: ${CallLogHelper.ITEM_MAP.keys}")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root

        toolbarLayout = binding.toolbarLayout
        itemDetailTextView = binding.itemDetail
//        aboutNumbersTextView = binding.aboutNumbers

        updateContent()
        rootView.setOnDragListener(dragListener)

        return rootView
    }


    private fun updateContent() {
        // Update the toolbar title with the phone number
        binding.toolbarLayout?.title = when {
            item?.contactName == "Неизвестный" -> "${item?.number}"
            else -> "${item?.contactName}"
        }

        if (toolbarLayout?.title == item?.contactName) {
            // Функция для поиска и отображения изображения контакта (необходимо реализовать)
//            toolbarLayout  = showContactImage(item?.contactName)
            aboutNumbersTextView?.text = item?.number ?: "Номер отсутствует"
        } else {
            // Действие по умолчанию
//            aboutNumbersTextView?.text = "здесь будет инфа о неизвестном номере"
        }

//        item = CallLogHelper.ITEM_MAP.put("${item?.number}", item)")


        item?.let { item ->
            Log.d("bundleLog", "callLog $item")

            // Формируем заголовок таблицы
            val header = "Журнал вызовов\n" +
                    "-------------------------------\n"

            // Формируем строки для каждого объекта `Detail`
            val detailsText = item.details.joinToString("\n") { detail ->
                " ${CallLogHelper.getCallTypeDescription(detail.type)} | " +
                        "${detail.date} | ${detail.time} | ${detail.duration} сек"
            }

            // Устанавливаем текст в TextView
            itemDetailTextView?.text = header + detailsText
        }

        // Show the placeholder content as text in a TextView.
//        item?.let {
//            itemDetailTextView.text = it.details.toString()
//        }

//

    }

    companion object {
        /**
         * The fragment argument representing the call log ID that this fragment represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
