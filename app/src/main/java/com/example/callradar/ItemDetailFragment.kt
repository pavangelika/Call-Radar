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
import android.widget.ImageView
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
    private var _binding: FragmentItemDetailBinding? = null
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
        item?.let { callDetail ->
            // Устанавливаем заголовок
            binding.toolbarLayout?.title = when {
                callDetail.contactName != "Неизвестный" -> callDetail.contactName
                else -> callDetail.number
            }

            // Устанавливаем информацию о номере/контакте
            binding.contactInfo?.text = when {
                callDetail.contactName != "Неизвестный" -> callDetail.contactName
                else -> "Неизвестный номер"
            }

            binding.numberInfo?.text = callDetail.number

            // Очищаем контейнер перед добавлением новых элементов
            binding.callLogsContainer?.removeAllViews()

            // Добавляем записи о звонках
            callDetail.details.forEach { detail ->
                val callItemView = layoutInflater.inflate(
                    R.layout.item_call_log_detail,
                    binding.callLogsContainer,
                    false
                )

                val icon = callItemView.findViewById<ImageView>(R.id.call_type_icon)
                icon.setImageDrawable(CallLogHelper.getCallTypeIcon(requireContext(), detail.type))

                callItemView.findViewById<TextView>(R.id.call_date).text = detail.date
                callItemView.findViewById<TextView>(R.id.call_time).text = detail.time
                callItemView.findViewById<TextView>(R.id.call_duration).text =
                    detail.duration.secondsToTimeString()  // Теперь работает с Long

                binding.callLogsContainer?.addView(callItemView)
            }


        }

    }

    fun Long.secondsToTimeString(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        val seconds = this % 60

        return when {
            hours > 0 -> "$hours час $minutes мин $seconds сек"
            minutes > 0 -> "$minutes мин $seconds сек"
            else -> "$seconds сек"
        }
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
