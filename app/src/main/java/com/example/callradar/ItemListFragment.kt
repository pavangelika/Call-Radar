package com.example.callradar

import DatabaseHelper
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.callradar.databinding.FragmentItemListBinding
import com.example.callradar.databinding.ItemListContentBinding
import com.example.callradar.calls.CallLogHelper
import com.example.callradar.calls.CallLogHelper.getCallTypeIcon
import com.example.callradar.calls.GroupedCallLog
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
    private lateinit var helper: DatabaseHelper
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
        helper = DatabaseHelper(requireContext())

        // Настраиваем RecyclerView
        val recyclerView: RecyclerView = binding.itemList
        setupRecyclerView(recyclerView, itemDetailFragmentContainer)

        // Запуск мониторинга звонков
        CallLogHelper.startCallLogMonitoring(requireContext()) { updatedLogs ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val groupedLogs = withContext(Dispatchers.IO) {
                        CallLogHelper.groupCallLogs(updatedLogs)
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
        val callLogs = CallLogHelper.fetchCallLogs(requireContext()) // Получаем список звонков
        val groupedLogs = CallLogHelper.groupCallLogs(callLogs)  // Группировка звонков

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
        private var callLogs: List<GroupedCallLog>, // Список данных о звонках
        private val itemDetailFragmentContainer: View?, // Контейнер для деталей элементов
        private val helper: DatabaseHelper
    ) : RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        // Метод для обновления данных
        fun updateData(newData: List<GroupedCallLog>) {
            callLogs = newData
            notifyDataSetChanged() // Уведомляем адаптер о том, что данные обновились
        }

        // Создание нового ViewHolder для адаптера
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        // Привязка данных к элементам View
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = callLogs[position]
//            Log.d("bundleLog", "callog $item")
            val search = helper.searchPhone(item.number)
            Log.d("numbersLog", "${item.number}")

//            holder.typeView.text = item.type.toString() // Тип звонка

            // Устанавливаем иконку звонка
            holder.typeIcon.setImageDrawable(getCallTypeIcon(context, item.type))

            val isMissed = item.type == CallLog.Calls.MISSED_TYPE

            // Установка цветов
            if (isMissed) {
                // Красный цвет для пропущенных звонков
                val redColor = ContextCompat.getColor(context, R.color.missed_call_primary)
                holder.typeIcon.imageTintList = ColorStateList.valueOf(redColor)
                holder.contactView.setTextColor(redColor)
            } else {
                // Стандартные цвета для остальных звонков
                holder.typeIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.icon_default)
                )
                holder.contactView.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
            }



            holder.contactView.text = when {
                item.contactName == "Неизвестный" -> "${item.number} ${item.callCount}"
                else -> "${item.contactName} ${item.callCount}"
            }
            holder.placeView.text = when {
                item.contactName == "Неизвестный" -> search
                else -> item.accountApp
            }
            holder.dateView.text = item.date

            with(holder.itemView) {
                tag = item // Устанавливаем данные звонка в tag для передачи
                setOnClickListener { itemView ->
                    val item = itemView.tag as GroupedCallLog
                    Log.d("callLog", "callog $item")

                    val bundle = Bundle()
                    // Передаем ID элемента
                    bundle.putString(
                        ItemDetailFragment.ARG_ITEM_ID,
                        item.number
                    )

                    Log.d("bundleLog", "bundle $bundle")
                    if (item.number == ItemDetailFragment.ARG_ITEM_ID) {
                        Log.d("bundleLog", "item for ${item.number} $item")
                    }


                    if (itemDetailFragmentContainer != null) {
                        itemDetailFragmentContainer.findNavController()
                            .navigate(R.id.fragment_item_detail, bundle)
                    } else {
                        itemView.findNavController().navigate(R.id.show_item_detail, bundle)

                    }
                }

            }
        }

        // Возвращаем количество элементов в списке
        override fun getItemCount() = callLogs.size

        inner class ViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
//            val typeView: TextView = binding.callTypeIcon
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