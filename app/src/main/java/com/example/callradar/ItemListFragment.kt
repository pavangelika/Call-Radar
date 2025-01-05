package com.example.callradar

import DatabaseHelper
import android.content.Context
//import com.example.callradar.placeholder.DatabaseHelper
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.callradar.databinding.FragmentItemListBinding
import com.example.callradar.databinding.ItemListContentBinding
import com.example.callradar.placeholder.CallLogHelper


/**
 * A Fragment representing a list of Pings. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of items, which when touched,
 * lead to a {@link ItemDetailFragment} representing
 * item details. On larger screens, the Navigation controller presents the list of items and
 * item details side-by-side using two vertical panes.
 */

// Класс фрагмента, отображающего список элементов
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


    // Ленивая инициализация binding, доступна только между `onCreateView` и `onDestroyView`
    private val binding get() = _binding!!
    private lateinit var helper: DatabaseHelper

    // Метод для создания представления фрагмента
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("StartLog", "Fragment onCreateView")
        // Инфлейтинг макета фрагмента через ViewBinding
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root   // Инфлейт XML разметки для фрагмента

    }

    // Метод для инициализации представления фрагмента
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        // Проверяем, есть ли контейнер для деталей элементов (для больших экранов)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        // Добавление обработчика для глобальных событий клавиатуры
        ViewCompat.addOnUnhandledKeyEventListener(view, unhandledKeyEventListenerCompat)

        // Инициализация базы данных
        helper = DatabaseHelper(requireContext())
        val db = helper.readableDatabase

        // Настраиваем RecyclerView
        val recyclerView: RecyclerView = binding.itemList
        setupRecyclerView(recyclerView, itemDetailFragmentContainer) 
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        itemDetailFragmentContainer: View?
    ) {
        val callLogs = CallLogHelper.fetchCallLogs(requireContext()) // Получаем список звонков
        val newcallLogs = CallLogHelper.groupCallLogs(callLogs) // Список звонков с группировкой по типу, номеру телефона (количеству звонков), дате
        helper.copyDatabase()
//        val search = helper.searchPhone("+79962240543")
//        Log.d("searchPhone", "searchPhone $search")
        
        
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(requireContext(),
            newcallLogs, itemDetailFragmentContainer, helper)

    }


    class SimpleItemRecyclerViewAdapter(
        private val context: Context,
        private val callLogs: List<CallLogHelper.GroupedCallLog>, // Список данных о звонках
        private val itemDetailFragmentContainer: View?, // Контейнер для деталей элементов
        private val helper: DatabaseHelper
    ) : RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        // Создание нового ViewHolder для адаптера
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding =
                ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        // Привязка данных к ViewHolder
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = callLogs[position]
//            Log.d("bundleLog", "callog $item")
            val search = helper.searchPhone(item.number)
            Log.d("numbersLog", "${item.number}")

            holder.idView.text = item.type.toString() // Тип звонка
            holder.contentView.text =  when {
                item.contactName == "Неизвестный" -> "${item.number} ${item.callCount} $search"
                else -> "${item.contactName} ${item.callCount}"}
            holder.dateView.text = item.date




            with(holder.itemView) {
                tag = item // Устанавливаем данные звонка в tag для передачи
                setOnClickListener { itemView ->
                    val item = itemView.tag as CallLogHelper.GroupedCallLog
                    Log.d("callLog", "callog $item")

                    val bundle = Bundle()
                    // Передаем ID элемента
                    bundle.putString(
                        ItemDetailFragment.ARG_ITEM_ID,
                        item.number
                    )

                    Log.d("bundleLog", "bundle $bundle")
                    if (item.number == ItemDetailFragment.ARG_ITEM_ID){
                        Log.d("bundleLog", "item for ${item.number} $item")}


                    if (itemDetailFragmentContainer != null) {
                        itemDetailFragmentContainer.findNavController()
                            .navigate(R.id.fragment_item_detail, bundle)
                    } else {
                        itemView.findNavController().navigate(R.id.show_item_detail, bundle)

                    }
                }


//                // Обработчик для правого клика (на мыши/тачпаде)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    /**
//                     * Context click listener to handle Right click events
//                     * from mice and trackpad input to provide a more native
//                     * experience on larger screen devices
//                     */
//                    setOnContextClickListener { v ->
//                        val item = v.tag as PlaceholderContent.PlaceholderItem
//                        Toast.makeText(
//                            v.context,
//                            "Context click of item " + item.id,
//                            Toast.LENGTH_LONG
//                        ).show()
//                        true
//                    }
//                }

                // Обработчик долгого нажатия для начала перетаскивания элемента
//                setOnLongClickListener { v ->
//                    // Setting the item id as the clip data so that the drop target is able to
//                    // identify the id of the content
//                    val clipItem = ClipData.Item(item.id)
//                    val dragData = ClipData(
//                        v.tag as? CharSequence,
//                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
//                        clipItem
//                    )
//
//                    if (Build.VERSION.SDK_INT >= 24) {
//                        v.startDragAndDrop(
//                            dragData,
//                            View.DragShadowBuilder(v),
//                            null,
//                            0
//                        )
//                    } else {
//                        v.startDrag(
//                            dragData,
//                            View.DragShadowBuilder(v),
//                            null,
//                            0
//                        )
//                    }
//                }
//
            }
        }

        // Возвращаем количество элементов в списке
        override fun getItemCount() = callLogs.size

        inner class ViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val idView: TextView = binding.idText
            val contentView: TextView = binding.content
            val dateView: TextView = binding.date // Текстовое поле для даты
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очистка ссылок на View
        helper.close()
    }




}