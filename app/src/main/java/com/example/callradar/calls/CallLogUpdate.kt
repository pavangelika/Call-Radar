import android.database.ContentObserver
import android.os.Handler
import android.content.Context
import android.util.Log
import com.example.callradar.calls.CallLogHelper

class CallLogUpdate(
    private val context: Context,
    handler: Handler,
    private val onCallLogChanged: () -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d("CallLogUpdate", "Журнал звонков изменился")

        // Получаем обновленные звонки
        val newCalls = CallLogHelper.fetchCallLogs(context)
        Log.d("CallLogUpdate", "Новые звонки: $newCalls")

        // Вызовем callback, если требуется
        onCallLogChanged()

    }
}
