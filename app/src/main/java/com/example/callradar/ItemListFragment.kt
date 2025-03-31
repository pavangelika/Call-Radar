package com.example.callradar

import com.example.callradar.utils.GetRegionFromNumber
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.callradar.databinding.FragmentItemListBinding
import com.example.callradar.databinding.ItemListContentBinding
import com.example.callradar.callog.CallLogDataHelper
import com.example.callradar.callog.CallLogDataHelper.getCallTypeIcon
import com.example.callradar.callog.GroupedCallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ItemListFragment : Fragment() {

    /**
     * Method to intercept global key events in the
     * item list fragment to trigger keyboard shortcuts
     * Currently provides a toast when Ctrl + Z and Ctrl + F
     * are triggered. Слушатель для глобальных клавиш, например Ctrl+Z или Ctrl+F
     */
    private val unhandledKeyEventListenerCompat =
        ViewCompat.OnUnhandledKeyEventListenerCompat { v, event ->
            if (event.keyCode == KeyEvent.KEYCODE_Z && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Undo (Ctrl + Z) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else if (event.keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Find (Ctrl + F) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            false
        }

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper: GetRegionFromNumber
    private lateinit var callLogsAdapter: SimpleItemRecyclerViewAdapter

    // Метод для создания представления фрагмента
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StartLog", "Fragment onCreateView")
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root

    }

    // Метод для инициализации представления фрагмента
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("StartLog", "Fragment onViewCreated")

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        // Проверяем, есть ли контейнер для деталей элементов (для больших экранов)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        // Добавление обработчика для глобальных событий клавиатуры
        ViewCompat.addOnUnhandledKeyEventListener(view, unhandledKeyEventListenerCompat)

//        // Инициализация базы данных
        helper = GetRegionFromNumber(requireContext())

        // Настраиваем RecyclerView
        val recyclerView: RecyclerView = binding.itemList
        setupRecyclerView(recyclerView, itemDetailFragmentContainer)

        // Запуск мониторинга звонков
        CallLogDataHelper.startCallLogMonitoring(requireContext()) { updatedLogs ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val groupedLogs = withContext(Dispatchers.IO) {
                        CallLogDataHelper.groupCallLogs(updatedLogs)
                    }

                    callLogsAdapter.updateData(groupedLogs)
                } catch (e: Exception) {
                    Log.e("CallLogMonitoring", "Ошибка обработки обновленных данных", e)
                }
            }
        }


    }

    // Метод для настройки RecyclerView
    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        itemDetailFragmentContainer: View?
    ) {
        val callLogs = CallLogDataHelper.fetchCallLogs(requireContext()) // Получаем список звонков
        val groupedLogs = CallLogDataHelper.groupCallLogs(callLogs)  // Группировка звонков

        // Настройка адаптера с начальными данными
        callLogsAdapter = SimpleItemRecyclerViewAdapter(
            requireContext(),
            groupedLogs, itemDetailFragmentContainer, helper
        )

        recyclerView.adapter = callLogsAdapter

    }

    // Адаптер для отображения звонков
    class SimpleItemRecyclerViewAdapter(
        private val context: Context,
        private var callLogs: List<GroupedCallLog>,
        private val itemDetailFragmentContainer: View?,
        private val helper: GetRegionFromNumber
    ) : RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        fun updateData(newData: List<GroupedCallLog>) {
            callLogs = newData
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }


        // Привязка данных к элементам View
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = callLogs[position]
            val searchResult = helper.searchPhone(item.number)
            val isCityCall = searchResult.startsWith("г.") &&
                    (!searchResult.contains("обл.") && !searchResult.contains("АО") && !searchResult.contains("округ"))
            val isMissed = item.type == CallLog.Calls.MISSED_TYPE

            // Установка иконки и цвета
            holder.typeIcon.setImageDrawable(getCallTypeIcon(context, item.type))
            val iconColor = if (isMissed) {
                ContextCompat.getColor(context, R.color.missed_call_primary)
            } else {
                ContextCompat.getColor(context, R.color.icon_default)
            }
            holder.typeIcon.imageTintList = ColorStateList.valueOf(iconColor)

// Установка текста контакта/номера (счетчик только для пропущенных)
            holder.contactView.text = when {
                item.contactName != "Неизвестный" -> {
                    if (isMissed) "${item.contactName} ${item.callCount}" else item.contactName
                }
                else -> {
                    if (isMissed) "${FormatPhoneNumber.formatPhoneNumber(item.number, isCityCall)} ${item.callCount}"
                    else FormatPhoneNumber.formatPhoneNumber(item.number, isCityCall)
                }
            }

            holder.contactView.setTextColor(
                ContextCompat.getColor(context, if (isMissed) R.color.missed_call_primary else R.color.text_primary)
            )

            // Установка места звонка
//            holder.placeView.text = when {
//                item.contactName != "Неизвестный" -> item.accountApp ?: ""
//                else -> searchResult ?: item.accountApp ?: ""
//            }

            holder.placeView.text =  searchResult ?: item.accountApp ?: ""

            // Установка даты
            holder.dateView.text = item.date

            // Обработка клика
            holder.itemView.tag = item
            holder.itemView.setOnClickListener { view ->
                val bundle = Bundle().apply {
                    putString(ItemDetailFragment.ARG_ITEM_ID, item.number)
                    // Передаем все номера контакта
                    putStringArrayList(ItemDetailFragment.ARG_ALL_NUMBERS, ArrayList(item.allNumbers))
                }

                if (itemDetailFragmentContainer != null) {
                    itemDetailFragmentContainer.findNavController()
                        .navigate(R.id.fragment_item_detail, bundle)
                } else {
                    view.findNavController().navigate(R.id.show_item_detail, bundle)
                }
            }
        }

        override fun getItemCount() = callLogs.size

        inner class ViewHolder(binding: ItemListContentBinding) : RecyclerView.ViewHolder(binding.root) {
            val typeIcon: ImageView = binding.callTypeIcon
            val contactView: TextView = binding.contactNameOrNumber
            val placeView: TextView = binding.callSource
            val dateView: TextView = binding.callDate
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("StartLog", "Fragment onDestroyView")
        _binding = null // Очистка ссылок на View
        helper.close()
    }


}